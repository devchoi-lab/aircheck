package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.ui.mapper.toDetailedLabelRes
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun RecommendationSection(
    weather: WeatherData?,
    airQuality: AirQuality?,
    modifier: Modifier = Modifier
) {
    val recommendations = mutableListOf<Pair<Int, String>>() // drawableRes to label
    
    // 1. 체감온도 기반 옷 추천
    weather?.feelsLike?.let { temp ->
        val clothing = RecommendedClothing.fromFeelsLike(temp)
        recommendations.add(clothing.toIconRes() to stringResource(clothing.toDetailedLabelRes()))
    }
    
    // 2. 강수확률 기반 우산 (6시간 내 최대값)
    weather?.hourlyForecast?.take(6)?.maxOfOrNull { it.precipitationProbability }?.let { maxPrecip ->
        when {
            maxPrecip >= 20 -> recommendations.add(CoreR.drawable.ic_umbrella to "${stringResource(CoreR.string.item_umbrella_with_prob)} (${maxPrecip}%)")
        }
    }
    
    // 3. 적설량 기반 (시간별 예보 중 합계)
    weather?.hourlyForecast?.sumOf { it.snowfall }?.let { totalSnow ->
        if (totalSnow > 0) {
            recommendations.add(CoreR.drawable.ic_snow_man to stringResource(CoreR.string.item_snow_cm, totalSnow))
        }
    }
    
    // 4. 대기질 민감군 주의 이상이면 마스크
    airQuality?.worstLevel?.let { level ->
        if (level.ordinal >= AqiLevel.UNHEALTHY_SENSITIVE.ordinal && level != AqiLevel.UNKNOWN) {
            recommendations.add(CoreR.drawable.ic_mask to stringResource(CoreR.string.item_mask, stringResource(level.toLabelRes())))
        }
    }
    
    if (recommendations.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(CoreR.string.recommendation_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(CoreR.string.recommendation_description),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            recommendations.forEach { (iconRes, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
