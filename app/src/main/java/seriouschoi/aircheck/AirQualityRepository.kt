package seriouschoi.aircheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// ── 에어코리아 API 응답 모델 ──────────────────────────────────────────────────

@Serializable
data class AirKoreaResponse(
    val response: AirKoreaBody? = null
)

@Serializable
data class AirKoreaBody(
    val body: AirKoreaItems? = null
)

@Serializable
data class AirKoreaItems(
    val items: List<AirKoreaItem> = emptyList()
)

@Serializable
data class AirKoreaItem(
    val stationName: String = "",
    val sidoName: String = "",
    val dataTime: String = "",
    val pm10Value: String = "-",      // µg/m³ ("" 또는 "-" = 측정 불가
    val pm25Value: String = "-",
    val khaiValue: String = "-",      // 통합대기환경지수 (Korean AQI)
    @SerialName("khaiGrade") val khaiGrade: String = "0"  // 1=좋음 2=보통 3=나쁨 4=매우나쁨
)

// ── Nominatim 역지오코딩 모델 ─────────────────────────────────────────────────

@Serializable
data class NominatimResponse(
    val address: NominatimAddress? = null
)

@Serializable
data class NominatimAddress(
    val state: String = "",
    val city: String? = null,
    val county: String? = null
)

// ── 도메인 모델 ───────────────────────────────────────────────────────────────

data class AirQuality(
    val aqi: Int,
    val pm25: Double?,
    val pm10: Double?,
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
        /** 에어코리아 khaiGrade (1~4) → AqiLevel */
        fun fromKhaiGrade(grade: String): AqiLevel = when (grade) {
            "1" -> GOOD
            "2" -> MODERATE
            "3" -> UNHEALTHY
            "4" -> VERY_UNHEALTHY
            else -> UNKNOWN
        }
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class AirQualityRepository {

    // 에어코리아 API 키 (공공데이터포털 발급)
    // ⚠️ 실제 배포 시 BuildConfig 또는 secrets로 이동 필요
    private val apiKey = "6e50f6e991e12cdf01168422bc4bea2797b3c044765977f01c210aa4058fe02f"

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
            // Step 1: Nominatim 역지오코딩 → 시/도 이름
            val sidoName = reverseGeocodeToSido(lat, lng)
                ?: return@withContext Result.failure(Exception("위치 정보를 변환할 수 없어요"))

            // Step 2: 에어코리아 실시간 측정값 (해당 시/도 전체)
            val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty" +
                    "?sidoName=${java.net.URLEncoder.encode(sidoName, "UTF-8")}" +
                    "&pageNo=1&numOfRows=20" +
                    "&returnType=json" +
                    "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&ver=1.0"

            val body = get(url)
            val response = json.decodeFromString<AirKoreaResponse>(body)
            val items = response.response?.body?.items ?: emptyList()
            val item = items.firstOrNull { it.pm25Value != "-" && it.pm25Value.isNotEmpty() }
                ?: items.firstOrNull()
                ?: return@withContext Result.failure(Exception("$sidoName 지역 측정 데이터를 찾을 수 없어요"))

            val pm25 = item.pm25Value.toDoubleOrNull()
            val pm10 = item.pm10Value.toDoubleOrNull()
            val aqi = item.khaiValue.toIntOrNull() ?: -1
            val level = AqiLevel.fromKhaiGrade(item.khaiGrade)

            Result.success(
                AirQuality(
                    aqi = aqi,
                    pm25 = pm25,
                    pm10 = pm10,
                    stationName = "${item.stationName} (${item.sidoName})",
                    updatedAt = item.dataTime,
                    level = level
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Nominatim 역지오코딩으로 에어코리아 sidoName 반환 */
    private fun reverseGeocodeToSido(lat: Double, lng: Double): String? {
        return try {
            val url = "https://nominatim.openstreetmap.org/reverse" +
                    "?lat=$lat&lon=$lng&format=json&accept-language=ko"
            val body = get(url)
            val response = json.decodeFromString<NominatimResponse>(body)
            val state = response.address?.state ?: return null
            // 시/도 이름 → 에어코리아 sidoName 매핑
            koreanSidoName(state)
        } catch (e: Exception) {
            null
        }
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
        else -> "서울" // fallback
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
