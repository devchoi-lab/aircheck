package seriouschoi.aircheck.feature.weather.ui.weather.components

import seriouschoi.aircheck.core.domain.model.DayLabel
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.RecommendedClothing

/**
 * 48시간 예보 요약 정보
 */
data class WeatherSummaryData(
    val minTemp: TempWithTime,
    val maxTemp: TempWithTime,
    val minFeelsLike: FeelsLikeWithTime,
    val maxFeelsLike: FeelsLikeWithTime,
    val precipitationPeriod: TimePeriod?,
    val snowfallPeriod: TimePeriod?,
)

data class TempWithTime(
    val temperature: Double,
    val dayLabel: DayLabel,
    val hour: String,
)

data class FeelsLikeWithTime(
    val temperature: Double,
    val dayLabel: DayLabel,
    val hour: String,
    val clothing: RecommendedClothing,
)

data class TimePeriod(
    val startDayLabel: DayLabel,
    val startHour: String,
    val endDayLabel: DayLabel,
    val endHour: String,
    val maxProbability: Int? = null,
)

/**
 * HourlyForecast 목록에서 요약 데이터 계산
 */
fun List<HourlyForecast>.toSummaryData(): WeatherSummaryData? {
    if (isEmpty()) return null

    val minTempForecast = minByOrNull { it.temperature } ?: return null
    val maxTempForecast = maxByOrNull { it.temperature } ?: return null
    val minFeelsLikeForecast = minByOrNull { it.feelsLike } ?: return null
    val maxFeelsLikeForecast = maxByOrNull { it.feelsLike } ?: return null

    // 비 상태인 연속 구간
    val precipitationPeriod = findFirstPeriodWithProbability(
        predicate = { it.condition.isRainy() },
        probability = { it.precipitationProbability }
    )

    // 눈 상태인 연속 구간
    val snowfallPeriod = findFirstPeriodWithProbability(
        predicate = { it.condition.isSnowy() },
        probability = { it.precipitationProbability }
    )

    return WeatherSummaryData(
        minTemp = TempWithTime(
            temperature = minTempForecast.temperature,
            dayLabel = minTempForecast.dayLabel,
            hour = minTempForecast.hour,
        ),
        maxTemp = TempWithTime(
            temperature = maxTempForecast.temperature,
            dayLabel = maxTempForecast.dayLabel,
            hour = maxTempForecast.hour,
        ),
        minFeelsLike = FeelsLikeWithTime(
            temperature = minFeelsLikeForecast.feelsLike,
            dayLabel = minFeelsLikeForecast.dayLabel,
            hour = minFeelsLikeForecast.hour,
            clothing = RecommendedClothing.fromFeelsLike(minFeelsLikeForecast.feelsLike),
        ),
        maxFeelsLike = FeelsLikeWithTime(
            temperature = maxFeelsLikeForecast.feelsLike,
            dayLabel = maxFeelsLikeForecast.dayLabel,
            hour = maxFeelsLikeForecast.hour,
            clothing = RecommendedClothing.fromFeelsLike(maxFeelsLikeForecast.feelsLike),
        ),
        precipitationPeriod = precipitationPeriod,
        snowfallPeriod = snowfallPeriod,
    )
}

/**
 * 조건을 만족하는 첫 번째 연속 구간 찾기
 */
private fun List<HourlyForecast>.findFirstPeriodWithProbability(
    predicate: (HourlyForecast) -> Boolean,
    probability: (HourlyForecast) -> Int,
): TimePeriod? {
    var startIndex: Int? = null
    var endIndex: Int? = null
    var maxProb = 0

    for ((index, forecast) in withIndex()) {
        if (predicate(forecast)) {
            if (startIndex == null) startIndex = index
            endIndex = index
            maxProb = maxOf(maxProb, probability(forecast))
        } else if (startIndex != null) {
            break
        }
    }

    if (startIndex == null || endIndex == null) return null

    val startForecast = this[startIndex]
    val endForecast = this[endIndex]

    return TimePeriod(
        startDayLabel = startForecast.dayLabel,
        startHour = startForecast.hour,
        endDayLabel = endForecast.dayLabel,
        endHour = endForecast.hour,
        maxProbability = maxProb,
    )
}
