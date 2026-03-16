package seriouschoi.aircheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// ── OpenAQ v3 API 응답 모델 ───────────────────────────────────────────────────

@Serializable
data class OpenAqLocationsResponse(
    val results: List<OpenAqLocation> = emptyList()
)

@Serializable
data class OpenAqLocation(
    val id: Int = 0,
    val name: String = "",
    val locality: String? = null,
    val country: OpenAqCountry? = null,
    val sensors: List<OpenAqSensor> = emptyList()
)

@Serializable
data class OpenAqCountry(val name: String = "")

@Serializable
data class OpenAqSensor(
    val id: Int = 0,
    val parameter: OpenAqParameter? = null
)

@Serializable
data class OpenAqParameter(
    val id: Int = 0,
    val name: String = "",       // "pm25", "pm10"
    val units: String = ""
)

@Serializable
data class OpenAqMeasurementsResponse(
    val results: List<OpenAqMeasurement> = emptyList()
)

@Serializable
data class OpenAqMeasurement(
    val value: Double = 0.0,
    @SerialName("period") val period: OpenAqPeriod? = null
)

@Serializable
data class OpenAqPeriod(
    @SerialName("datetimeFrom") val datetimeFrom: OpenAqDatetime? = null
)

@Serializable
data class OpenAqDatetime(val local: String = "")

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
        /** PM2.5 µg/m³ 기준으로 AQI 레벨 계산 (WHO 기준 참고) */
        fun fromPm25(pm25: Double): AqiLevel = when {
            pm25 < 0 -> UNKNOWN
            pm25 <= 15 -> GOOD
            pm25 <= 35 -> MODERATE
            pm25 <= 75 -> SENSITIVE
            pm25 <= 150 -> UNHEALTHY
            pm25 <= 250 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }

        fun fromAqi(aqi: Int): AqiLevel = when {
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

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // OpenAQ는 User-Agent 필요
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
            // Step 1: 50km 반경 내 PM2.5 측정소 검색 (OpenAQ v3)
            val locUrl = "https://api.openaq.org/v3/locations" +
                    "?coordinates=$lat,$lng" +
                    "&radius=50000" +
                    "&parameters_id=2" +   // PM2.5
                    "&limit=5" +
                    "&sort=distance&order=asc"

            val locBody = get(locUrl)
            val locResponse = json.decodeFromString<OpenAqLocationsResponse>(locBody)
            val location = locResponse.results.firstOrNull()
                ?: return@withContext Result.failure(Exception("근처 측정소를 찾을 수 없어요"))

            // PM2.5 센서 ID 추출
            val pm25Sensor = location.sensors.firstOrNull { it.parameter?.name == "pm25" }
            val pm10Sensor = location.sensors.firstOrNull { it.parameter?.name == "pm10" }

            // Step 2: 최신 PM2.5 측정값
            val pm25Value = pm25Sensor?.let { fetchLatestMeasurement(it.id) }
            val pm10Value = pm10Sensor?.let { fetchLatestMeasurement(it.id) }

            val stationName = listOfNotNull(location.locality, location.country?.name)
                .joinToString(", ")
                .ifBlank { location.name }

            val level = pm25Value?.let { AqiLevel.fromPm25(it) } ?: AqiLevel.UNKNOWN
            // PM2.5 → AQI 근사 변환
            val aqi = pm25Value?.let { pm25ToAqi(it) } ?: -1

            Result.success(
                AirQuality(
                    aqi = aqi,
                    pm25 = pm25Value,
                    pm10 = pm10Value,
                    stationName = stationName,
                    updatedAt = "",
                    level = level
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchLatestMeasurement(sensorId: Int): Double? = try {
        val url = "https://api.openaq.org/v3/sensors/$sensorId/measurements" +
                "?limit=1&sort=desc&order=desc"
        val body = get(url)
        val response = json.decodeFromString<OpenAqMeasurementsResponse>(body)
        response.results.firstOrNull()?.value
    } catch (e: Exception) {
        null
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }

    /** PM2.5 µg/m³ → AQI 변환 (EPA 공식) */
    private fun pm25ToAqi(pm25: Double): Int {
        data class Range(val cLow: Double, val cHigh: Double, val iLow: Int, val iHigh: Int)
        val r = when {
            pm25 <= 12.0 -> Range(0.0, 12.0, 0, 50)
            pm25 <= 35.4 -> Range(12.1, 35.4, 51, 100)
            pm25 <= 55.4 -> Range(35.5, 55.4, 101, 150)
            pm25 <= 150.4 -> Range(55.5, 150.4, 151, 200)
            pm25 <= 250.4 -> Range(150.5, 250.4, 201, 300)
            else -> Range(250.5, 500.4, 301, 500)
        }
        return ((r.iHigh - r.iLow) / (r.cHigh - r.cLow) * (pm25 - r.cLow) + r.iLow).toInt()
    }
}
