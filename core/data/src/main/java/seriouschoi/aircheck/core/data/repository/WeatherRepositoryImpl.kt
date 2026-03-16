package seriouschoi.aircheck.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.core.network.api.OpenMeteoApiClient
import seriouschoi.aircheck.core.network.api.OpenMeteoHourly
import seriouschoi.aircheck.core.network.api.OpenMeteoResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val apiClient: OpenMeteoApiClient
) : WeatherRepository {

    override suspend fun fetch(lat: Double, lng: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val body = apiClient.getWeather(lat, lng)
            val response = apiClient.json.decodeFromString<OpenMeteoResponse>(body)
            val current = response.current
                ?: return@withContext Result.failure(Exception("날씨 데이터 없음"))

            val isDay = current.isDay == 1
            val hourlyForecast = parseHourlyForecast(response.hourly, isDay)
            
            val next6Hours = hourlyForecast.take(6)
            val precipProb = next6Hours.maxOfOrNull { it.precipitationProbability } ?: 0
            val snowProb = if (next6Hours.any { it.snowfall > 0 }) {
                next6Hours.filter { it.snowfall > 0 }.maxOfOrNull { it.precipitationProbability } ?: 0
            } else 0
            
            val weatherData = WeatherData(
                temperature = current.temperature,
                feelsLike = current.feelsLike,
                precipitation = current.precipitation,
                precipitationProbability = precipProb,
                snowfallProbability = snowProb,
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
                    hour in 6..18
                )
            )
        }
    }
}
