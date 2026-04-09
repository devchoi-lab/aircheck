package seriouschoi.aircheck.core.domain.usecase

import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.CalendarEvent
import seriouschoi.aircheck.core.domain.model.CalendarEventWeatherInsight
import seriouschoi.aircheck.core.domain.model.CalendarEventWeatherSnapshot
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.model.WeatherData
import javax.inject.Inject
import kotlin.math.abs

/**
 * [CalendarEventWeatherSnapshot] 한 건을 받아 사용자에게 보여줄 인사이트
 * ([CalendarEventWeatherInsight]) 로 변환한다.
 *
 * 이전에는 이 계산 로직이 `CalendarViewModel.findForecastsForEvent` /
 * `getRecommendationsForEvent` private 함수에 들어 있었지만, 명백한 도메인
 * 로직이라 ViewModel 의 책임이 아니어서 도메인 use case 로 분리한다.
 *
 * 계산 항목:
 * 1. 일정 시간대에 해당하는 forecast 추출 — 정확히 맞는 게 없으면 가장 가까운 1개
 * 2. forecast 의 최저/최고 체감온도, 강수확률 최댓값과 그 시각
 * 3. 추천 아이템:
 *    - 의상: 최저/최고 체감온도 기반 (서로 다르면 둘 다)
 *    - 우산: 비 예보 + 강수확률 최댓값
 *    - 눈: 눈 예보의 강설량 합
 *    - 마스크: AqiLevel.UNHEALTHY_SENSITIVE 이상이면
 */
class ComputeCalendarEventWeatherInsightUseCase @Inject constructor() {

    operator fun invoke(snapshot: CalendarEventWeatherSnapshot): CalendarEventWeatherInsight {
        val forecasts = findForecastsForEvent(snapshot.event, snapshot.weatherData)
        val maxPrecip = forecasts.maxByOrNull { it.precipitationProbability }

        return CalendarEventWeatherInsight(
            event = snapshot.event,
            resolvedLocation = snapshot.resolvedLocation,
            minFeelsLike = forecasts.minOfOrNull { it.feelsLike.toInt() },
            maxFeelsLike = forecasts.maxOfOrNull { it.feelsLike.toInt() },
            hasPrecipitation = forecasts.any { it.condition.isRainy() || it.condition.isSnowy() },
            maxPrecipitationProbability = maxPrecip?.precipitationProbability,
            maxPrecipitationHour = maxPrecip?.hour,
            recommendations = buildRecommendations(forecasts, snapshot.airQuality?.worstLevel),
        )
    }

    private fun findForecastsForEvent(
        event: CalendarEvent,
        weatherData: WeatherData?,
    ): List<HourlyForecast> {
        val hourlyForecast = weatherData?.hourlyForecast ?: return emptyList()
        if (hourlyForecast.isEmpty()) return emptyList()

        val startHour = if (event.isAllDay) 0 else event.startTime.hour
        val endHour = if (event.isAllDay) 24 else event.endTime.hour.coerceAtLeast(startHour + 1)

        val forecasts = hourlyForecast.filter { forecast ->
            val forecastHour = forecast.hour.replace("시", "").toIntOrNull() ?: 0
            forecastHour in startHour until endHour
        }

        // 범위에 정확히 맞는 예보가 없으면 가장 가까운 1개
        if (forecasts.isEmpty()) {
            val targetHour = if (event.isAllDay) 12 else event.startTime.hour
            return listOfNotNull(
                hourlyForecast.minByOrNull { forecast ->
                    val forecastHour = forecast.hour.replace("시", "").toIntOrNull() ?: 0
                    abs(forecastHour - targetHour)
                },
            )
        }
        return forecasts
    }

    private fun buildRecommendations(
        forecasts: List<HourlyForecast>,
        worstAirLevel: AqiLevel?,
    ): List<Recommendation> {
        if (forecasts.isEmpty()) return emptyList()

        return buildList {
            // 1. 체감온도 기반 의상 (최저/최고 체감온도 각각)
            val minFeelsLike = forecasts.minOf { it.feelsLike }
            val maxFeelsLike = forecasts.maxOf { it.feelsLike }
            val minClothing = RecommendedClothing.fromFeelsLike(minFeelsLike)
            val maxClothing = RecommendedClothing.fromFeelsLike(maxFeelsLike)
            add(Recommendation.Clothing(minClothing))
            if (maxClothing != minClothing) {
                add(Recommendation.Clothing(maxClothing))
            }

            // 2. 우산 (일정 시간대 비 예보)
            forecasts
                .filter { it.condition.isRainy() }
                .maxOfOrNull { it.precipitationProbability }
                ?.let { add(Recommendation.Umbrella(it)) }

            // 3. 눈 (일정 시간대 눈 예보)
            forecasts
                .filter { it.condition.isSnowy() }
                .sumOf { it.snowfall }
                .takeIf { it > 0 }
                ?.let { add(Recommendation.Snow(it)) }

            // 4. 마스크 (공기질은 일정 위치의 worstLevel 기준)
            worstAirLevel
                ?.takeIf { it.ordinal >= AqiLevel.UNHEALTHY_SENSITIVE.ordinal && it != AqiLevel.UNKNOWN }
                ?.let { add(Recommendation.Mask(it)) }
        }
    }
}
