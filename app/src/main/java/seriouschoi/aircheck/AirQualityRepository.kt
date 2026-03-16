package seriouschoi.aircheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

// ── 에어코리아 API 응답 모델 ──────────────────────────────────────────────────

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
    val khaiValue: String = "-",     // 통합대기환경지수
    val khaiGrade: String = "0"      // 1=좋음 2=보통 3=나쁨 4=매우나쁨
)

// TM 기준좌표 조회 응답
@Serializable
data class TmCoordResponse(val response: AirKoreaBody? = null)

@Serializable
data class TmCoordItem(
    val tmX: String = "",
    val tmY: String = "",
    val umdName: String = "",
    val sggName: String = "",
    val sidoName: String = ""
)

// 근접측정소 응답
@Serializable
data class NearbyStationResponse(val response: AirKoreaBody? = null)

@Serializable
data class NearbyStationItem(
    val stationName: String = "",
    val addr: String = "",
    val tm: String = ""      // 거리(km)
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
    val pm25Level: AqiLevel,   // WHO 기준 PM2.5 등급
    val pm10Level: AqiLevel,   // WHO 기준 PM10 등급
    val stationName: String,
    val updatedAt: String,
    val level: AqiLevel        // 에어코리아 통합대기환경지수
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
        /** 에어코리아 khaiGrade (1~4) 기반 */
        fun fromKhaiGrade(grade: String): AqiLevel = when (grade) {
            "1" -> GOOD
            "2" -> MODERATE
            "3" -> UNHEALTHY
            "4" -> VERY_UNHEALTHY
            else -> UNKNOWN
        }

        /** PM2.5 WHO 기준 (μg/m³) */
        fun fromPm25(v: Double): AqiLevel = when {
            v <= 15.0  -> GOOD
            v <= 25.0  -> MODERATE
            v <= 37.5  -> SENSITIVE
            v <= 75.0  -> UNHEALTHY
            v <= 150.0 -> VERY_UNHEALTHY
            else       -> HAZARDOUS
        }

        /** PM10 WHO 기준 (μg/m³) */
        fun fromPm10(v: Double): AqiLevel = when {
            v <= 45.0  -> GOOD
            v <= 90.0  -> MODERATE
            v <= 180.0 -> SENSITIVE
            v <= 270.0 -> UNHEALTHY
            v <= 400.0 -> VERY_UNHEALTHY
            else       -> HAZARDOUS
        }
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class AirQualityRepository {

    private val apiKey = "34dddd00aa791a78203988d8151ead27a5b0c8c64fc5c0dea94c4b64160b2df5"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("User-Agent", "AirCheckApp/1.0")
                    .build()
            )
        }
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Nominatim 역지오코딩 → 읍면동명
            val (umdName, fallbackSido) = reverseGeocode(lat, lng)
                ?: return@withContext Result.failure(Exception("위치 정보를 확인할 수 없어요"))

            // Step 2: 읍면동명 → TM 좌표
            val (tmX, tmY) = getTmCoords(umdName)
                ?: run {
                    // TM 좌표 조회 실패 시 시/도 전체에서 첫 번째 측정소로 폴백
                    return@withContext fetchBySido(fallbackSido)
                }

            // Step 3: TM 좌표 → 가장 가까운 측정소명
            val stationName = getNearestStation(tmX, tmY)
                ?: return@withContext fetchBySido(fallbackSido)

            // Step 4: 측정소별 실시간 데이터 조회
            fetchByStation(stationName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Nominatim 역지오코딩 → (읍면동명, 시도명) */
    private fun reverseGeocode(lat: Double, lng: Double): Pair<String, String>? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$lat&lon=$lng&format=json&accept-language=ko&addressdetails=1"
        val body = get(url)
        val response = json.decodeFromString<NominatimResponse>(body)
        val addr = response.address ?: return null

        // 읍면동 추출: suburb > quarter > city_district > neighbourhood > village > town > city
        val umd = listOfNotNull(
            addr.suburb, addr.quarter, addr.city_district,
            addr.neighbourhood, addr.village, addr.town, addr.city
        ).firstOrNull { it.isNotBlank() }?.replace(Regex("[시군구]$"), "") ?: return null

        val sido = koreanSidoName(addr.state)
        Pair(umd, sido)
    } catch (e: Exception) {
        null
    }

    /** 읍면동명 → TM 좌표 (에어코리아 getTMStdrCrdnt) */
    private fun getTmCoords(umdName: String): Pair<Double, Double>? = try {
        val url = "http://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getTMStdrCrdnt" +
                "?umdName=${java.net.URLEncoder.encode(umdName, "UTF-8")}" +
                "&pageNo=1&numOfRows=5&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}"
        val body = get(url)
        // 응답이 items 배열이지만 AirKoreaBody 재사용 불가 → raw parse
        val jsonObj = json.parseToJsonElement(body)
        val items = jsonObj
            .jsonObject["response"]?.jsonObject
            ?.get("body")?.jsonObject
            ?.get("items")?.jsonArray
        val first = items?.firstOrNull()?.jsonObject ?: return null
        val x = first["tmX"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        val y = first["tmY"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        Pair(x, y)
    } catch (e: Exception) {
        null
    }

    /** TM 좌표 → 가장 가까운 측정소명 (에어코리아 getNearbyMsrstnList) */
    private fun getNearestStation(tmX: Double, tmY: Double): String? = try {
        val url = "http://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList" +
                "?tmX=$tmX&tmY=$tmY" +
                "&pageNo=1&numOfRows=3&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}"
        val body = get(url)
        val jsonObj = json.parseToJsonElement(body)
        val items = jsonObj
            .jsonObject["response"]?.jsonObject
            ?.get("body")?.jsonObject
            ?.get("items")?.jsonArray
        items?.firstOrNull()?.jsonObject?.get("stationName")?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }

    /** 측정소명으로 실시간 데이터 조회 */
    private fun fetchByStation(stationName: String): Result<AirQuality> = try {
        val url = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?stationName=${java.net.URLEncoder.encode(stationName, "UTF-8")}" +
                "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        val body = get(url)
        val response = json.decodeFromString<AirKoreaResponse>(body)
        val item = response.response?.body?.items?.firstOrNull()
            ?: return Result.failure(Exception("$stationName 측정소 데이터가 없어요"))
        Result.success(toAirQuality(item))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** 시/도 전체로 폴백 */
    private fun fetchBySido(sidoName: String): Result<AirQuality> = try {
        val url = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty" +
                "?sidoName=${java.net.URLEncoder.encode(sidoName, "UTF-8")}" +
                "&pageNo=1&numOfRows=10&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        val body = get(url)
        val response = json.decodeFromString<AirKoreaResponse>(body)
        val item = response.response?.body?.items
            ?.firstOrNull { it.pm25Value != "-" && it.pm25Value.isNotEmpty() }
            ?: response.response?.body?.items?.firstOrNull()
            ?: return Result.failure(Exception("$sidoName 측정 데이터를 찾을 수 없어요"))
        Result.success(toAirQuality(item))
    } catch (e: Exception) {
        Result.failure(e)
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
        state.contains("서울") -> "서울"
        state.contains("부산") -> "부산"
        state.contains("대구") -> "대구"
        state.contains("인천") -> "인천"
        state.contains("광주") -> "광주"
        state.contains("대전") -> "대전"
        state.contains("울산") -> "울산"
        state.contains("세종") -> "세종"
        state.contains("경기") -> "경기"
        state.contains("강원") -> "강원"
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
