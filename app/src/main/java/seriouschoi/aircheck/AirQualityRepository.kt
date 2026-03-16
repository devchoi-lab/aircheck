package seriouschoi.aircheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// ── 에어코리아 응답 모델 ──────────────────────────────────────────────────────

@Serializable
data class AirKoreaResponse(val response: AirKoreaBody? = null)

@Serializable
data class AirKoreaBody(val body: AirKoreaItems? = null)

@Serializable
data class AirKoreaItems(val items: List<AirKoreaItem> = emptyList())

@Serializable
data class AirKoreaItem(
    val stationName: String = "",
    val sidoName: String = "",
    val dataTime: String = "",
    val pm10Value: String = "-",
    val pm25Value: String = "-",
    val khaiValue: String = "-",
    val khaiGrade: String = "0"
)

// ── Nominatim 역지오코딩 모델 ─────────────────────────────────────────────────

@Serializable
data class NominatimResponse(val address: NominatimAddress? = null)

@Serializable
data class NominatimAddress(
    val state: String = "",
    val city: String? = null,
    val city_district: String? = null,
    val suburb: String? = null,
    val quarter: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val neighbourhood: String? = null
)

// ── 도메인 모델 ───────────────────────────────────────────────────────────────

data class AirQuality(
    val aqi: Int,
    val pm25: Double?,
    val pm10: Double?,
    val pm25Level: AqiLevel,
    val pm10Level: AqiLevel,
    val stationName: String,
    val updatedAt: String,
    val level: AqiLevel
)

enum class AqiLevel(val label: String, val emoji: String, val colorHex: Long) {
    GOOD("좋음", "😊", 0xFF00C8A0),
    MODERATE("보통", "😐", 0xFFFFD700),
    SENSITIVE("민감군 주의", "😷", 0xFFFF8C00),
    UNHEALTHY("나쁨", "😮", 0xFFFF4500),
    VERY_UNHEALTHY("매우 나쁨", "🤢", 0xFF9400D3),
    HAZARDOUS("위험", "☠️", 0xFF7E0023),
    UNKNOWN("알 수 없음", "❓", 0xFF9E9E9E);

    companion object {
        fun fromKhaiGrade(grade: String): AqiLevel = when (grade) {
            "1" -> GOOD; "2" -> MODERATE; "3" -> UNHEALTHY; "4" -> VERY_UNHEALTHY
            else -> UNKNOWN
        }
        fun fromPm25(v: Double): AqiLevel = when {
            v <= 15.0 -> GOOD; v <= 25.0 -> MODERATE; v <= 37.5 -> SENSITIVE
            v <= 75.0 -> UNHEALTHY; v <= 150.0 -> VERY_UNHEALTHY; else -> HAZARDOUS
        }
        fun fromPm10(v: Double): AqiLevel = when {
            v <= 45.0 -> GOOD; v <= 90.0 -> MODERATE; v <= 180.0 -> SENSITIVE
            v <= 270.0 -> UNHEALTHY; v <= 400.0 -> VERY_UNHEALTHY; else -> HAZARDOUS
        }
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class AirQualityRepository {

    private val apiKey = "34dddd00aa791a78203988d8151ead27a5b0c8c64fc5c0dea94c4b64160b2df5"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("User-Agent", "AirCheckApp/1.0").build())
        }
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Nominatim → 시도명 + 지역 키워드 목록
            val (sidoName, keywords) = reverseGeocode(lat, lng)
                ?: return@withContext Result.failure(Exception("위치 정보를 확인할 수 없어요"))

            // Step 2: 해당 시/도 전체 측정소 조회
            val allItems = fetchSidoItems(sidoName)

            // Step 3: 지역 키워드로 가장 가까운 측정소 선택
            val matched = findBestMatch(allItems, keywords)
                ?: allItems.firstOrNull { it.pm25Value != "-" && it.pm25Value.isNotEmpty() }
                ?: allItems.firstOrNull()
                ?: return@withContext Result.failure(Exception("$sidoName 측정 데이터가 없어요"))

            Result.success(toAirQuality(matched))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Nominatim 역지오코딩 → (시도명, 지역 키워드 목록)
     * 키워드: 세부 행정구역(동/구) 이름들. 측정소 이름 매칭에 사용.
     * ex) 부천시 중동 → sidoName="경기", keywords=["중동","원미","부천"]
     */
    private fun reverseGeocode(lat: Double, lng: Double): Pair<String, List<String>>? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$lat&lon=$lng&format=json&accept-language=ko&addressdetails=1"
        val body = get(url)
        val response = json.decodeFromString<NominatimResponse>(body)
        val addr = response.address ?: return null

        // 시/도 추론: state → city → county 순으로 시도
        val sido = inferSidoName(addr)

        // 키워드: 세부→광역 순으로 모든 행정구역명 수집 (suffix 제거 포함)
        val rawNames = listOfNotNull(
            addr.quarter, addr.suburb, addr.neighbourhood,
            addr.city_district, addr.village, addr.town, addr.city, addr.county
        ).filter { it.isNotBlank() }

        val keywords = rawNames.flatMap { name ->
            listOf(
                name,                                               // 원본: 중동, 원미구
                name.replace(Regex("[시군구동읍면리]$"), "").trim()  // suffix 제거: 원미구→원미
            )
        }.filter { it.length >= 2 }.distinct()

        Pair(sido, keywords)
    } catch (e: Exception) {
        null
    }

    /** 시/도명 추론: state가 없으면 city/county로 추론 */
    private fun inferSidoName(addr: NominatimAddress): String {
        // 1. state가 있으면 직접 매핑
        if (addr.state.isNotBlank()) {
            return koreanSidoName(addr.state)
        }

        // 2. city가 광역시/특별시면 직접 사용
        val city = addr.city ?: ""
        val directCities = mapOf(
            "서울" to "서울", "부산" to "부산", "대구" to "대구", "인천" to "인천",
            "광주" to "광주", "대전" to "대전", "울산" to "울산", "세종" to "세종"
        )
        for ((key, value) in directCities) {
            if (city.contains(key)) return value
        }

        // 3. 경기도 주요 도시 매핑 (Nominatim이 state 안 주는 경우 대비)
        val gyeonggiCities = listOf(
            "수원", "성남", "안양", "안산", "부천", "광명", "평택", "과천", "오산", "시흥",
            "군포", "의왕", "하남", "이천", "용인", "파주", "안성", "김포", "화성", "광주",
            "양주", "포천", "고양", "의정부", "동두천", "구리", "남양주", "여주", "양평",
            "가평", "연천"
        )
        for (gc in gyeonggiCities) {
            if (city.contains(gc)) return "경기"
        }

        // 4. county 확인
        val county = addr.county ?: ""
        for (gc in gyeonggiCities) {
            if (county.contains(gc)) return "경기"
        }

        // 5. 기본값 - 경기 (한국 인구 대부분)
        return "경기"
    }

    /** 시/도 전체 측정소 실시간 데이터 (최대 100개) */
    private fun fetchSidoItems(sidoName: String): List<AirKoreaItem> {
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty" +
                "?sidoName=${java.net.URLEncoder.encode(sidoName, "UTF-8")}" +
                "&pageNo=1&numOfRows=100&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        val body = get(url)
        
        // API 에러 체크
        if (body.startsWith("Unauthorized") || body.startsWith("Forbidden")) {
            throw Exception("API 인증 오류: $body")
        }
        
        return json.decodeFromString<AirKoreaResponse>(body).response?.body?.items
            ?: throw Exception("$sidoName 응답 파싱 실패")
    }

    /**
     * 측정소 목록에서 지역 키워드로 가장 잘 맞는 측정소 선택
     * 매칭: stationName에 keyword가 포함되거나 keyword에 stationName이 포함
     * ex) keywords=["중동","원미","부천"] + stationName="송내대로(중동)" → "중동" 포함 → 매칭
     */
    private fun findBestMatch(items: List<AirKoreaItem>, keywords: List<String>): AirKoreaItem? {
        // 키워드 앞쪽(세부)이 더 정확 → 순서대로 탐색
        for (keyword in keywords) {
            val match = items.firstOrNull { item ->
                item.stationName.contains(keyword) || keyword.contains(item.stationName)
            }
            if (match != null) return match
        }
        return null
    }

    private fun toAirQuality(item: AirKoreaItem): AirQuality {
        val pm25 = item.pm25Value.toDoubleOrNull()
        val pm10 = item.pm10Value.toDoubleOrNull()
        return AirQuality(
            aqi = item.khaiValue.toIntOrNull() ?: -1,
            pm25 = pm25,
            pm10 = pm10,
            pm25Level = pm25?.let { AqiLevel.fromPm25(it) } ?: AqiLevel.UNKNOWN,
            pm10Level = pm10?.let { AqiLevel.fromPm10(it) } ?: AqiLevel.UNKNOWN,
            stationName = "${item.stationName} (${item.sidoName})",
            updatedAt = item.dataTime,
            level = AqiLevel.fromKhaiGrade(item.khaiGrade)
        )
    }

    private fun koreanSidoName(state: String): String = when {
        state.contains("서울") -> "서울"; state.contains("부산") -> "부산"
        state.contains("대구") -> "대구"; state.contains("인천") -> "인천"
        state.contains("광주") -> "광주"; state.contains("대전") -> "대전"
        state.contains("울산") -> "울산"; state.contains("세종") -> "세종"
        state.contains("경기") -> "경기"; state.contains("강원") -> "강원"
        state.contains("충청북") || state.contains("충북") -> "충북"
        state.contains("충청남") || state.contains("충남") -> "충남"
        state.contains("전라북") || state.contains("전북") -> "전북"
        state.contains("전라남") || state.contains("전남") -> "전남"
        state.contains("경상북") || state.contains("경북") -> "경북"
        state.contains("경상남") || state.contains("경남") -> "경남"
        state.contains("제주") -> "제주"
        else -> "서울"
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
