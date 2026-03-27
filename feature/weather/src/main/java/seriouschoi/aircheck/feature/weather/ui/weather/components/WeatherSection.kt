package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.ui.mapper.toDisplayString
import seriouschoi.aircheck.core.ui.mapper.toEmoji
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun WeatherSection(weather: WeatherData, modifier: Modifier = Modifier) {
    val horizontalPadding = 16.dp
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        // 현재 날씨 라벨
        Text(
            text = stringResource(CoreR.string.section_current_weather),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
        Spacer(Modifier.height(12.dp))

        // 날씨 상태 + 이모지
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        ) {
            Text(weather.condition.toEmoji(), fontSize = 48.sp)
            Column {
                Text(
                    text = stringResource(weather.condition.toLabelRes()),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (weather.precipitation > 0) {
                    val precipLabel = when (weather.condition) {
                        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWER -> stringResource(CoreR.string.format_snowfall, weather.precipitation.toString())
                        else -> stringResource(CoreR.string.format_precipitation, weather.precipitation.toString())
                    }
                    Text(
                        text = precipLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 기온 / 체감온도 / 강수확률 / 강설확률
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStat(
                emoji = "🌡️",
                label = stringResource(CoreR.string.label_temperature),
                value = stringResource(CoreR.string.format_temperature, weather.temperature)
            )
            WeatherStatWithIcon(
                iconRes = RecommendedClothing.fromFeelsLike(weather.feelsLike).toIconRes(),
                label = stringResource(CoreR.string.label_feels_like),
                value = stringResource(CoreR.string.format_temperature, weather.feelsLike)
            )
            WeatherStat(
                emoji = "🌧️",
                label = stringResource(CoreR.string.label_precipitation_prob),
                value = stringResource(CoreR.string.format_percent, weather.precipitationProbability)
            )
            WeatherStat(
                emoji = "❄️",
                label = stringResource(CoreR.string.label_snowfall_prob),
                value = stringResource(CoreR.string.format_percent, weather.snowfallProbability)
            )
        }
        
        // 48시간 예보
        if (weather.hourlyForecast.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = stringResource(CoreR.string.section_hourly_forecast),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )
            Spacer(Modifier.height(8.dp))
            
            // 48시간 요약 정보
            weather.hourlyForecast.toSummaryData()?.let { summary ->
                WeatherSummary(
                    summary = summary,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
                Spacer(Modifier.height(12.dp))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grouped = weather.hourlyForecast.groupBy { it.dayLabel }
                grouped.forEach { (dayLabel, forecasts) ->
                    // 날짜 헤더
                    item {
                        val displayLabel = dayLabel.toDisplayString()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = displayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // 해당 날짜의 시간별 예보
                    items(forecasts) { forecast ->
                        HourlyForecastItem(forecast)
                    }
                }
            }
        }
    }
}
