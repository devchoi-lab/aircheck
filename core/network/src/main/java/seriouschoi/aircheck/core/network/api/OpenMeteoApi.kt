package seriouschoi.aircheck.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

// ── API 응답 모델 ──────────────────────────────────────────────────────────

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

// ── API 클라이언트 ─────────────────────────────────────────────────────────

@Singleton
class OpenMeteoApiClient @Inject constructor() {
    
    private val client = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }
    
    fun getWeather(lat: Double, lng: Double): String {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lng" +
                "&current=temperature_2m,apparent_temperature,precipitation,weathercode,cloudcover,is_day" +
                "&hourly=temperature_2m,apparent_temperature,precipitation_probability,snowfall,weathercode" +
                "&forecast_hours=48" +
                "&timezone=auto"
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
