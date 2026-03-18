@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.core.network.api.AircheckApiClient
import seriouschoi.aircheck.core.network.api.AircheckWeatherResponse
import seriouschoi.aircheck.core.network.api.HourlyForecastResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val apiClient: AircheckApiClient
) : WeatherRepository {

    override suspend fun fetch(lat: Double, lng: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val body = apiClient.getWeather(lat, lng)
            val response = apiClient.json.decodeFromString<AircheckWeatherResponse>(body)
            val weather = response.weather
                ?: return@withContext Result.failure(Exception("날씨 데이터 없음"))
            val current = weather.current
                ?: return@withContext Result.failure(Exception("현재 날씨 데이터 없음"))

            val hourlyForecast = parseHourlyForecast(weather.hourlyForecast, current.isDay)
            
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
                condition = WeatherCondition.from(current.weatherCode, current.isDay),
                cloudCover = current.cloudCover,
                isDay = current.isDay,
                hourlyForecast = hourlyForecast
            )
            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseHourlyForecast(
        hourlyList: List<HourlyForecastResponse>,
        isDay: Boolean
    ): List<HourlyForecast> {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        return hourlyList.map { hourly ->
            val hour = hourly.hour
            
            val dateStr = hourly.time.substringBefore("T")
            val forecastDate = try {
                LocalDate.parse(dateStr, dateFormatter)
            } catch (e: Exception) {
                today
            }
            
            val dayLabel = when (forecastDate) {
                today -> "오늘"
                today.plusDays(1) -> "내일"
                today.plusDays(2) -> "모레"
                else -> "${forecastDate.monthValue}/${forecastDate.dayOfMonth}"
            }
            
            HourlyForecast(
                hour = "${hour}시",
                dayLabel = dayLabel,
                temperature = hourly.temperature,
                feelsLike = hourly.feelsLike,
                precipitationProbability = hourly.precipitationProbability,
                snowfall = hourly.snowfall,
                condition = WeatherCondition.from(hourly.weatherCode, hour in 6..18)
            )
        }
    }
}
