package seriouschoi.aircheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// ── API 응답 모델 ─────────────────────────────────────────────────────────────

@Serializable
data class AqiResponse(
    val status: String,
    val data: AqiData? = null
)

@Serializable
data class AqiData(
    val aqi: Int = -1,
    val city: AqiCity? = null,
    val iaqi: AqiIaqi? = null,
    val time: AqiTime? = null
)

@Serializable
data class AqiCity(val name: String = "")

@Serializable
data class AqiIaqi(
    val pm25: AqiValue? = null,
    val pm10: AqiValue? = null,
    val o3: AqiValue? = null,
    val no2: AqiValue? = null
)

@Serializable
data class AqiValue(val v: Double = 0.0)

@Serializable
data class AqiTime(
    @SerialName("s") val timestamp: String = ""
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
        fun from(aqi: Int) = when {
            aqi < 0 -> UNKNOWN
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class AirQualityRepository {

    // AQICN 데모 토큰 (테스트용). 실사용은 https://aqicn.org/data-platform/token 에서 무료 발급.
    private val token = "demo"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.waqi.info/feed/geo:$lat;$lng/?token=$token"
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
            val response = json.decodeFromString<AqiResponse>(body)

            if (response.status != "ok" || response.data == null) {
                return@withContext Result.failure(Exception("API 오류: ${response.status}"))
            }

            val data = response.data
            val aqi = data.aqi
            val airQuality = AirQuality(
                aqi = aqi,
                pm25 = data.iaqi?.pm25?.v,
                pm10 = data.iaqi?.pm10?.v,
                stationName = data.city?.name ?: "알 수 없는 위치",
                updatedAt = data.time?.timestamp ?: "",
                level = AqiLevel.from(aqi)
            )
            Result.success(airQuality)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
