@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherRepository
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
import seriouschoi.aircheck.core.network.api.AircheckApi
import seriouschoi.aircheck.core.network.api.AircheckWeatherResponse
import seriouschoi.aircheck.core.network.api.AirQualityResponse
import seriouschoi.aircheck.core.network.api.BatchWeatherItem
import seriouschoi.aircheck.core.network.api.BatchWeatherRequest
import seriouschoi.aircheck.core.network.api.HourlyForecastResponse
import seriouschoi.aircheck.core.network.api.LocationRequest
import seriouschoi.aircheck.core.network.api.WeatherResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedWeatherRepositoryImpl @Inject constructor(
    private val api: AircheckApi
) : CombinedWeatherRepository {

    override suspend fun fetch(lat: Double, lng: Double): CombinedWeatherResult {
        return try {
            val response = api.getWeather(lat, lng)

            CombinedWeatherResult(
                weather = parseWeather(response),
                airQuality = parseAirQuality(response)
            )
        } catch (e: Exception) {
            CombinedWeatherResult(
                weather = Result.failure(e),
                airQuality = Result.failure(e)
            )
        }
    }

    override suspend fun fetchBatch(locations: List<Pair<Double, Double>>): List<CombinedWeatherResult> {
        if (locations.isEmpty()) return emptyList()

        return try {
            val request = BatchWeatherRequest(
                locations = locations.map { (lat, lng) -> LocationRequest(lat, lng) }
            )
            val items = api.getBatchWeather(request)

            items.map { item ->
                CombinedWeatherResult(
                    weather = parseWeatherFromBatch(item.weather),
                    airQuality = parseAirQualityFromBatch(item.airQuality)
                )
            }
        } catch (e: Exception) {
            // 배치 실패 시 개별 호출로 fallback
            locations.map { (lat, lng) -> fetch(lat, lng) }
        }
    }

    private fun parseWeatherFromBatch(weather: WeatherResponse?): Result<WeatherData> {
        weather ?: return Result.failure(Exception("날씨 데이터 없음"))
        val current = weather.current
            ?: return Result.failure(Exception("현재 날씨 데이터 없음"))

        val hourlyForecast = parseHourlyForecast(weather.hourlyForecast, current.isDay)

        val next6Hours = hourlyForecast.take(6)
        val precipProb = next6Hours.maxOfOrNull { it.precipitationProbability } ?: 0
        val snowProb = if (next6Hours.any { it.snowfall > 0 }) {
            next6Hours.filter { it.snowfall > 0 }.maxOfOrNull { it.precipitationProbability } ?: 0
        } else 0

        return Result.success(WeatherData(
            temperature = current.temperature,
            feelsLike = current.feelsLike,
            precipitation = current.precipitation,
            precipitationProbability = precipProb,
            snowfallProbability = snowProb,
            condition = WeatherCondition.from(current.weatherCode, current.isDay),
            cloudCover = current.cloudCover,
            isDay = current.isDay,
            hourlyForecast = hourlyForecast
        ))
    }

    private fun parseAirQualityFromBatch(air: AirQualityResponse?): Result<AirQuality> {
        air ?: return Result.failure(Exception("대기질 데이터 없음"))

        val mainStation = air.stationName

        val pm10Data = air.pm10?.let { value ->
            PmData(
                value = value.toDouble(),
                level = AqiLevel.fromPm10(value.toDouble()),
                stationName = air.pm10Station,
                isFromFallback = air.pm10Station != mainStation
            )
        }

        val pm25Data = air.pm25?.let { value ->
            PmData(
                value = value.toDouble(),
                level = AqiLevel.fromPm25(value.toDouble()),
                stationName = air.pm25Station,
                isFromFallback = air.pm25Station != mainStation
            )
        }

        val levels = listOfNotNull(pm25Data?.level, pm10Data?.level)
        val worstLevel = levels.maxByOrNull { it.ordinal } ?: AqiLevel.UNKNOWN

        return Result.success(AirQuality(
            aqi = 0,
            pm25 = pm25Data,
            pm10 = pm10Data,
            mainStationName = "${air.stationName} (${air.sidoName})",
            updatedAt = air.dataTime,
            level = worstLevel,
            worstLevel = worstLevel
        ))
    }

    private fun parseWeather(response: AircheckWeatherResponse): Result<WeatherData> {
        val weather = response.weather
            ?: return Result.failure(Exception("날씨 데이터 없음"))
        val current = weather.current
            ?: return Result.failure(Exception("현재 날씨 데이터 없음"))

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
        return Result.success(weatherData)
    }

    private fun parseAirQuality(response: AircheckWeatherResponse): Result<AirQuality> {
        val air = response.airQuality
            ?: return Result.failure(Exception("대기질 데이터 없음"))

        val mainStation = air.stationName

        val pm10Data = air.pm10?.let { value ->
            val isFromFallback = air.pm10Station != mainStation
            PmData(
                value = value.toDouble(),
                level = AqiLevel.fromPm10(value.toDouble()),
                stationName = air.pm10Station,
                isFromFallback = isFromFallback
            )
        }

        val pm25Data = air.pm25?.let { value ->
            val isFromFallback = air.pm25Station != mainStation
            PmData(
                value = value.toDouble(),
                level = AqiLevel.fromPm25(value.toDouble()),
                stationName = air.pm25Station,
                isFromFallback = isFromFallback
            )
        }

        val levels = listOfNotNull(pm25Data?.level, pm10Data?.level)
        val worstLevel = levels.maxByOrNull { it.ordinal } ?: AqiLevel.UNKNOWN

        return Result.success(
            AirQuality(
                aqi = 0,
                pm25 = pm25Data,
                pm10 = pm10Data,
                mainStationName = "${air.stationName} (${air.sidoName})",
                updatedAt = air.dataTime,
                level = worstLevel,
                worstLevel = worstLevel
            )
        )
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
