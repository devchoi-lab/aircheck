package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.ui.mapper.toEmoji
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.ui.theme.WeatherColors
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun HourlyForecastItem(forecast: HourlyForecast) {
    val fontScale = LocalDensity.current.fontScale
    val clothingIconSize = (20 * fontScale).dp
    val smallIconSize = (16 * fontScale).dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // 시간
        Text(
            text = forecast.hour,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        
        // 날씨 이모지
        Text(forecast.condition.toEmoji(), fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        
        // 기온
        Text(
            text = "%.0f°".format(forecast.temperature),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 체감온도
        Text(
            text = stringResource(CoreR.string.format_feels_like, forecast.feelsLike),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(4.dp))
        
        // 강수/강설 확률
        if (forecast.snowfall > 0) {
            Text(
                text = "❄️${forecast.precipitationProbability}%",
                fontSize = 10.sp,
                color = WeatherColors.Snow
            )
        } else {
            Text(
                text = "🌧️${forecast.precipitationProbability}%",
                fontSize = 10.sp,
                color = if (forecast.precipitationProbability >= 40) 
                    WeatherColors.Precipitation 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.width(40.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(4.dp))
        
        // 추천 의상 아이콘 (체감온도 기반)
        val clothing = RecommendedClothing.fromFeelsLike(forecast.feelsLike)
        Icon(
            painter = painterResource(clothing.toIconRes()),
            contentDescription = stringResource(clothing.toLabelRes()),
            modifier = Modifier.size(clothingIconSize),
            tint = Color.Unspecified
        )
        Text(
            text = stringResource(clothing.toLabelRes()),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 우산/눈 추천 (날씨 상태 기반)
        if (forecast.condition.isRainy()) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_umbrella_blue),
                contentDescription = stringResource(CoreR.string.item_umbrella),
                modifier = Modifier.size(smallIconSize),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(CoreR.string.item_umbrella),
                fontSize = 9.sp,
                color = WeatherColors.Precipitation
            )
        } else if (forecast.condition.isSnowy()) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_snow_man),
                contentDescription = stringResource(CoreR.string.item_snow),
                modifier = Modifier.size(smallIconSize),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(CoreR.string.item_snow),
                fontSize = 9.sp,
                color = WeatherColors.Snow
            )
        }
    }
}

@Composable
internal fun WeatherStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun WeatherStatWithIcon(iconRes: Int, label: String, value: String) {
    val fontScale = LocalDensity.current.fontScale
    val iconSize = (28 * fontScale).dp
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(iconSize),
            tint = Color.Unspecified
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
