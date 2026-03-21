package seriouschoi.aircheck.core.network.api

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import seriouschoi.aircheck.core.network.serializer.InstantSerializer
import java.time.Instant

// ── API 응답 모델 ──────────────────────────────────────────────────────────
@InternalSerializationApi
@Serializable
data class AircheckWeatherResponse(
    val weather: WeatherResponse? = null,
    val airQuality: AirQualityResponse? = null
)

@InternalSerializationApi
@Serializable
data class WeatherResponse(
    val current: CurrentWeather? = null,
    val hourlyForecast: List<HourlyForecastResponse> = emptyList()
)

@InternalSerializationApi
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

@InternalSerializationApi
@Serializable
data class HourlyForecastResponse(
    @Serializable(with = InstantSerializer::class)
    val time: Instant,
    val hour: Int = 0,  // 서버에서 보내는 KST 시간 (표시용)
    val temperature: Double = 0.0,
    val feelsLike: Double = 0.0,
    val precipitationProbability: Int = 0,
    val snowfall: Double = 0.0,
    val weatherCode: Int = 0,
    val weatherCondition: String = ""
)

@InternalSerializationApi
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

// ── 배치 API 모델 ──────────────────────────────────────────────────────────
@InternalSerializationApi
@Serializable
data class BatchWeatherRequest(
    val locations: List<LocationRequest>
)

@InternalSerializationApi
@Serializable
data class LocationRequest(
    val lat: Double,
    val lng: Double
)

@InternalSerializationApi
@Serializable
data class BatchWeatherItem(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val weather: WeatherResponse? = null,
    val airQuality: AirQualityResponse? = null
)

// ── Retrofit API 인터페이스 ─────────────────────────────────────────────────

interface AircheckApi {

    @OptIn(InternalSerializationApi::class)
    @GET("api/v1/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): AircheckWeatherResponse

    @OptIn(InternalSerializationApi::class)
    @POST("api/v1/weather/batch")
    suspend fun getBatchWeather(
        @Body request: BatchWeatherRequest
    ): List<BatchWeatherItem>
}
