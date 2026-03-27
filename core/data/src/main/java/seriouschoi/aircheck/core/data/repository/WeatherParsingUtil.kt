@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.DataError
import seriouschoi.aircheck.core.domain.model.DayLabel
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.network.api.AirQualityResponse
import seriouschoi.aircheck.core.network.api.HourlyForecastResponse
import seriouschoi.aircheck.core.network.api.WeatherResponse
import java.time.LocalDate
import java.time.ZoneId

/**
 * API 응답 → 도메인 모델 변환 유틸리티
 *
 * WeatherRepositoryImpl, AirQualityRepositoryImpl, CombinedWeatherRepositoryImpl에서
 * 공통으로 사용하는 파싱 로직을 한 곳에 모아 중복을 제거한다.
 */
internal object WeatherParsingUtil {

    fun parseWeatherData(weather: WeatherResponse?): Result<WeatherData> {
        weather ?: return Result.failure(DataError.WeatherUnavailable)
        val current = weather.current
            ?: return Result.failure(DataError.WeatherUnavailable)

        val hourlyForecast = parseHourlyForecast(weather.hourlyForecast, current.isDay)

        val next6Hours = hourlyForecast.take(6)
        val precipProb = next6Hours.maxOfOrNull { it.precipitationProbability } ?: 0
        val snowProb = if (next6Hours.any { it.snowfall > 0 }) {
            next6Hours.filter { it.snowfall > 0 }.maxOfOrNull { it.precipitationProbability } ?: 0
        } else 0

        return Result.success(
            WeatherData(
                temperature = current.temperature,
                feelsLike = current.feelsLike,
                precipitation = current.precipitation,
                precipitationProbability = precipProb,
                snowfallProbability = snowProb,
                condition = WeatherCondition.fromString(current.weatherCondition, current.isDay),
                cloudCover = current.cloudCover,
                isDay = current.isDay,
                hourlyForecast = hourlyForecast
            )
        )
    }

    fun parseAirQualityData(air: AirQualityResponse?): Result<AirQuality> {
        air ?: return Result.failure(DataError.AirQualityUnavailable)

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

    fun parseHourlyForecast(
        hourlyList: List<HourlyForecastResponse>,
        isDay: Boolean
    ): List<HourlyForecast> {
        val localZone = ZoneId.systemDefault()
        val today = LocalDate.now(localZone)

        return hourlyList.map { hourly ->
            // UTC Instant → 로컬 시간으로 변환
            val localDateTime = hourly.time.atZone(localZone).toLocalDateTime()
            val forecastDate = localDateTime.toLocalDate()
            val hour = localDateTime.hour

            val dayLabel = when (forecastDate) {
                today -> DayLabel.Today
                today.plusDays(1) -> DayLabel.Tomorrow
                today.plusDays(2) -> DayLabel.DayAfterTomorrow
                else -> DayLabel.Date(forecastDate.monthValue, forecastDate.dayOfMonth)
            }

            HourlyForecast(
                hour = "${hour}시",
                dayLabel = dayLabel,
                temperature = hourly.temperature,
                feelsLike = hourly.feelsLike,
                precipitationProbability = hourly.precipitationProbability,
                snowfall = hourly.snowfall,
                condition = WeatherCondition.fromString(hourly.weatherCondition, hour in 6..18)
            )
        }
    }
}
