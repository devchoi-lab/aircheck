@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── API 응답 모델 ──────────────────────────────────────────────────────────

@Serializable
data class AircheckWeatherResponse(
    val weather: WeatherResponse? = null,
    val airQuality: AirQualityResponse? = null
)

@Serializable
data class WeatherResponse(
    val current: CurrentWeather? = null,
    val hourlyForecast: List<HourlyForecastResponse> = emptyList()
)

@Serializable
data class CurrentWeather(
    val temperature: Double = 0.0,
    val feelsLike: Double = 0.0,
    val precipitation: Double = 0.0,
    val weatherCode: Int = 0,
    val weatherCondition: String = "",
    val cloudCover: Int = 0,
    val isDay: Boolean = true
)

@Serializable
data class HourlyForecastResponse(
    val time: String = "",
    val hour: Int = 0,
    val temperature: Double = 0.0,
    val feelsLike: Double = 0.0,
    val precipitationProbability: Int = 0,
    val snowfall: Double = 0.0,
    val weatherCode: Int = 0,
    val weatherCondition: String = ""
)

@Serializable
data class AirQualityResponse(
    val stationName: String = "",
    val sidoName: String = "",
    val dataTime: String = "",
    val pm10: Int? = null,
    val pm10Station: String = "",
    val pm25: Int? = null,
    val pm25Station: String = ""
)

// ── API 클라이언트 ─────────────────────────────────────────────────────────

@Singleton
class AircheckApiClient @Inject constructor() {
    
    companion object {
        // 프로덕션 서버 URL
        private const val BASE_URL = "https://api.todaygonggi.com"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 날씨 + 대기질 통합 API
     */
    fun getWeather(lat: Double, lng: Double): String {
        val url = "$BASE_URL/api/v1/weather?lat=$lat&lng=$lng"
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API 호출 실패: ${response.code}")
            }
            response.body?.string() ?: ""
        }
    }
}
