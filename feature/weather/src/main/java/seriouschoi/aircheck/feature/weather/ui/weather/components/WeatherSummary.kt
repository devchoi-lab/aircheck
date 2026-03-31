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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.ui.mapper.toDisplayString
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.resource.R as CoreR

/**
 * 48시간 예보 요약 UI
 */
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
        TemperatureSummaryRow(
            emoji = "🌡️",
            label = stringResource(CoreR.string.label_temperature),
            min = summary.minTemp,
            max = summary.maxTemp,
        )

        FeelsLikeSummaryRow(
            min = summary.minFeelsLike,
            max = summary.maxFeelsLike,
        )

        summary.precipitationPeriod?.let { period ->
            PeriodRow(
                emoji = "🌧️",
                label = stringResource(CoreR.string.label_rain),
                period = period,
            )
        }

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
    val minDayLabel = min.dayLabel.toDisplayString()
    val maxDayLabel = max.dayLabel.toDisplayString()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 헤더
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        // 최저/최고 온도
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(CoreR.string.label_min),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${formatTemp(min.temperature)} ($minDayLabel ${min.hour})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(CoreR.string.label_max),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${formatTemp(max.temperature)} ($maxDayLabel ${max.hour})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun FeelsLikeSummaryRow(
    min: FeelsLikeWithTime,
    max: FeelsLikeWithTime,
) {
    val minDayLabel = min.dayLabel.toDisplayString()
    val maxDayLabel = max.dayLabel.toDisplayString()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 헤더
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧥", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(CoreR.string.label_feels_like),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        // 최저 체감온도 + 옷차림
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(CoreR.string.label_min),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${formatTemp(min.temperature)} ($minDayLabel ${min.hour})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(min.clothing.toIconRes()),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = stringResource(min.clothing.toLabelRes()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(2.dp))
        // 최고 체감온도 + 옷차림
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(CoreR.string.label_max),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${formatTemp(max.temperature)} ($maxDayLabel ${max.hour})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(max.clothing.toIconRes()),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = stringResource(max.clothing.toLabelRes()),
                fontSize = 11.sp,
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
    val startLabel = period.startDayLabel.toDisplayString()
    val endLabel = period.endDayLabel.toDisplayString()
    
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
                append("$startLabel ${period.startHour} ~ $endLabel ${period.endHour}")
                period.maxProbability?.let { prob ->
                    append(" (${prob}%)")
                }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatTemp(temp: Double): String = "${temp.toInt()}°"
