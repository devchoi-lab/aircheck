package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.resource.R as CoreR

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
    val dayLabel: String,
    val hour: String,
)

data class FeelsLikeWithTime(
    val temperature: Double,
    val dayLabel: String,
    val hour: String,
    val clothing: RecommendedClothing,
)

data class TimePeriod(
    val startDayLabel: String,
    val startHour: String,
    val endDayLabel: String,
    val endHour: String,
    val maxProbability: Int? = null,  // 강수/강설 최대 확률
)

/**
 * HourlyForecast 목록에서 요약 데이터 계산
 */
fun List<HourlyForecast>.toSummaryData(): WeatherSummaryData? {
    if (isEmpty()) return null

    // 최저/최고 기온
    val minTempForecast = minByOrNull { it.temperature } ?: return null
    val maxTempForecast = maxByOrNull { it.temperature } ?: return null

    // 최저/최고 체감온도
    val minFeelsLikeForecast = minByOrNull { it.feelsLike } ?: return null
    val maxFeelsLikeForecast = maxByOrNull { it.feelsLike } ?: return null

    // 강수 구간 (첫 번째 구간만)
    val precipitationPeriod = findFirstPeriodWithProbability(
        predicate = { it.precipitationProbability >= 30 },
        probability = { it.precipitationProbability }
    )

    // 강설 구간 (첫 번째 구간만)
    val snowfallPeriod = findFirstPeriodWithProbability(
        predicate = { it.snowfall > 0 },
        probability = { it.precipitationProbability }  // 강설도 강수확률 사용
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
 * 조건을 만족하는 첫 번째 연속 구간을 찾고, 해당 구간의 최대 확률도 반환한다
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
            if (startIndex == null) {
                startIndex = index
            }
            endIndex = index
            maxProb = maxOf(maxProb, probability(forecast))
        } else if (startIndex != null) {
            // 구간이 끝남
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

@Composable
internal fun WeatherSummary(
    summary: WeatherSummaryData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 기온 요약
        TemperatureSummaryRow(
            emoji = "🌡️",
            label = stringResource(CoreR.string.label_temperature),
            min = summary.minTemp,
            max = summary.maxTemp,
        )

        // 체감온도 요약 (의상 포함)
        FeelsLikeSummaryRow(
            min = summary.minFeelsLike,
            max = summary.maxFeelsLike,
        )

        // 강수 정보
        summary.precipitationPeriod?.let { period ->
            PeriodRow(
                emoji = "🌧️",
                label = stringResource(CoreR.string.label_rain),
                period = period,
            )
        }

        // 강설 정보
        summary.snowfallPeriod?.let { period ->
            PeriodRow(
                emoji = "❄️",
                label = stringResource(CoreR.string.label_snow),
                period = period,
            )
        }
    }
}

@Composable
private fun TemperatureSummaryRow(
    emoji: String,
    label: String,
    min: TempWithTime,
    max: TempWithTime,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${formatTemp(min.temperature)} (${min.dayLabel} ${min.hour})",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = " ~ ",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${formatTemp(max.temperature)} (${max.dayLabel} ${max.hour})",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun FeelsLikeSummaryRow(
    min: FeelsLikeWithTime,
    max: FeelsLikeWithTime,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🧥", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(CoreR.string.label_feels_like),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${formatTemp(min.temperature)} (${min.dayLabel} ${min.hour})",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = " ~ ",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatTemp(max.temperature)} (${max.dayLabel} ${max.hour})",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(4.dp))
        // 의상 추천
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(min.clothing.toIconRes()),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(min.clothing.toLabelRes()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = " → ",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                painter = painterResource(max.clothing.toIconRes()),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(max.clothing.toLabelRes()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PeriodRow(
    emoji: String,
    label: String,
    period: TimePeriod,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = buildString {
                append("${period.startDayLabel} ${period.startHour} ~ ${period.endDayLabel} ${period.endHour}")
                period.maxProbability?.let { prob ->
                    append(" (${prob}%)")
                }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatTemp(temp: Double): String {
    return "${temp.toInt()}°"
}
