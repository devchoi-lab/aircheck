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
data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("apparent_temperature") val feelsLike: Double = 0.0,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("weathercode") val weatherCode: Int = 0,
    @SerialName("cloudcover") val cloudCover: Int = 0,
    @SerialName("is_day") val isDay: Int = 1
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
    @SerialName("apparent_temperature") val feelsLike: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int> = emptyList(),
    val snowfall: List<Double> = emptyList(),
    @SerialName("weathercode") val weatherCode: List<Int> = emptyList()
)

// ── 도메인 모델 ───────────────────────────────────────────────────────────────

data class WeatherData(
    val temperature: Double,
    val feelsLike: Double,
    val precipitation: Double,
    val condition: WeatherCondition,
    val cloudCover: Int,
    val isDay: Boolean,
    val hourlyForecast: List<HourlyForecast> = emptyList()
)

/** 시간별 예보 */
data class HourlyForecast(
    val hour: String,              // "14시", "15시" 등
    val temperature: Double,
    val feelsLike: Double,
    val precipitationProbability: Int,  // 강수확률 (%)
    val snowfall: Double,          // 적설량 (cm)
    val condition: WeatherCondition
)

enum class WeatherCondition(val label: String, val emoji: String) {
    CLEAR_DAY("맑음", "☀️"),
    CLEAR_NIGHT("맑음 (야간)", "🌙"),
    PARTLY_CLOUDY("구름 조금", "⛅"),
    CLOUDY("흐림", "☁️"),
    FOG("안개", "🌫️"),
    DRIZZLE("이슬비", "🌦️"),
    RAIN("비", "🌧️"),
    HEAVY_RAIN("폭우", "⛈️"),
    SNOW("눈", "❄️"),
    SNOW_SHOWER("소나기눈", "🌨️"),
    THUNDERSTORM("뇌우", "⚡"),
    UNKNOWN("알 수 없음", "❓");

    companion object {
        fun from(code: Int, isDay: Boolean): WeatherCondition = when (code) {
            0 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            61, 63 -> RAIN
            65, 80, 81, 82 -> HEAVY_RAIN
            71, 73, 75 -> SNOW
            85, 86 -> SNOW_SHOWER
            95, 96, 99 -> THUNDERSTORM
            else -> UNKNOWN
        }
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class WeatherRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(lat: Double, lng: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lng" +
                    "&current=temperature_2m,apparent_temperature,precipitation,weathercode,cloudcover,is_day" +
                    "&hourly=temperature_2m,apparent_temperature,precipitation_probability,snowfall,weathercode" +
                    "&forecast_hours=12" +
                    "&timezone=auto"

            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
            val response = json.decodeFromString<OpenMeteoResponse>(body)
            val current = response.current
                ?: return@withContext Result.failure(Exception("날씨 데이터 없음"))

            val isDay = current.isDay == 1
            
            // 시간별 예보 파싱
            val hourlyForecast = parseHourlyForecast(response.hourly, isDay)
            
            val weatherData = WeatherData(
                temperature = current.temperature,
                feelsLike = current.feelsLike,
                precipitation = current.precipitation,
                condition = WeatherCondition.from(current.weatherCode, isDay),
                cloudCover = current.cloudCover,
                isDay = isDay,
                hourlyForecast = hourlyForecast
            )
            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseHourlyForecast(hourly: OpenMeteoHourly?, isDay: Boolean): List<HourlyForecast> {
        if (hourly == null) return emptyList()
        
        return hourly.time.mapIndexedNotNull { index, timeStr ->
            // ISO 8601 형식에서 시간 추출: "2026-03-16T14:00" → "14시"
            val hour = try {
                timeStr.substringAfter("T").substringBefore(":").toInt()
            } catch (e: Exception) {
                return@mapIndexedNotNull null
            }
            
            HourlyForecast(
                hour = "${hour}시",
                temperature = hourly.temperature.getOrNull(index) ?: return@mapIndexedNotNull null,
                feelsLike = hourly.feelsLike.getOrNull(index) ?: 0.0,
                precipitationProbability = hourly.precipitationProbability.getOrNull(index) ?: 0,
                snowfall = hourly.snowfall.getOrNull(index) ?: 0.0,
                condition = WeatherCondition.from(
                    hourly.weatherCode.getOrNull(index) ?: 0,
                    hour in 6..18  // 6시~18시를 낮으로 간주
                )
            )
        }
    }
}
