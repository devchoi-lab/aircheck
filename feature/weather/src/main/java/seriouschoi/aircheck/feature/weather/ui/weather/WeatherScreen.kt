@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.ui.mapper.toColor
import seriouschoi.aircheck.core.ui.mapper.toDetailedLabelRes
import seriouschoi.aircheck.core.ui.mapper.toEmoji
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.ui.theme.WeatherColors
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun WeatherScreen(
    uiState: WeatherContract.UiState,
    multiState: MultiLocationState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onAddLocation: () -> Unit = {}
) {
    val isRefreshing = uiState.isRefreshing || multiState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onAddLocation) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(CoreR.string.content_description_add_location))
                    }
                    IconButton(onClick = { onIntent(WeatherContract.Intent.ShowInfoDialog) }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(CoreR.string.content_description_info))
                    }
                    IconButton(onClick = { onIntent(WeatherContract.Intent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(CoreR.string.content_description_refresh))
                    }
                }
            )
        }
    ) { innerPadding ->

        // 기준 안내 다이얼로그
        if (uiState.showInfoDialog) {
            AirQualityInfoDialog(onDismiss = { onIntent(WeatherContract.Intent.DismissInfoDialog) })
        }

        // 삭제 확인 다이얼로그
        uiState.deleteConfirm?.let { confirm ->
            AlertDialog(
                onDismissRequest = { onIntent(WeatherContract.Intent.DismissDeleteDialog) },
                title = { Text(stringResource(CoreR.string.delete_location_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(CoreR.string.delete_location_message, confirm.locationName)) },
                confirmButton = {
                    TextButton(onClick = { onIntent(WeatherContract.Intent.ConfirmDeleteLocation) }) {
                        Text(stringResource(CoreR.string.button_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(WeatherContract.Intent.DismissDeleteDialog) }) {
                        Text(stringResource(CoreR.string.button_cancel))
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (multiState.locations.isEmpty() && uiState.isLoading) {
                // 초기 로딩 중
                Box(modifier = Modifier.fillMaxSize())
            } else if (multiState.locations.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { multiState.locations.size })

                LocationPager(
                    pagerState = pagerState,
                    multiState = multiState,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}



// ── Success ───────────────────────────────────────────────────────────────────

@Composable
internal fun SuccessContent(
    air: WeatherContract.DataResult<AirQuality>,
    weather: WeatherContract.DataResult<WeatherData>,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 추천 아이템 (가장 위에) ─────────────────────────────
        if (weather.data != null || air.data != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                RecommendationSection(
                    weather = weather.data, 
                    airQuality = air.data,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // ── 날씨 섹션 ────────────────────────────────────────
        when {
            weather.data != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    WeatherSection(weather.data, modifier = Modifier.padding(16.dp))
                }
            }
            weather.error != null -> ErrorSection(
                title = stringResource(CoreR.string.weather_title), 
                message = weather.error.asString(), 
                onRetry = { onIntent(WeatherContract.Intent.Refresh) }
            )
        }

        // ── 공기질 섹션 ──────────────────────────────────────
        when {
            air.data != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    AirQualitySection(air.data, modifier = Modifier.padding(16.dp))
                }
            }
            air.error != null -> ErrorSection(
                title = stringResource(CoreR.string.air_quality_title), 
                message = air.error.asString(), 
                onRetry = { onIntent(WeatherContract.Intent.Refresh) }
            )
        }

        // 업데이트 시간
        air.data?.updatedAt?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = stringResource(CoreR.string.air_quality_update, it),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ── 날씨 섹션 ─────────────────────────────────────────────────────────────────

@Composable
private fun WeatherSection(weather: WeatherData, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 현재 날씨 라벨
        Text(
            text = stringResource(CoreR.string.section_current_weather),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        
        // 날씨 상태 + 이모지
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStat(
                emoji = "🌡️",
                label = stringResource(CoreR.string.label_temperature),
                value = stringResource(CoreR.string.format_temperature, weather.temperature)
            )
            WeatherStat(
                emoji = "🤔",
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grouped = weather.hourlyForecast.groupBy { it.dayLabel }
                grouped.forEach { (dayLabel, forecasts) ->
                    // 날짜 헤더
                    item {
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
                                text = dayLabel,
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

@Composable
private fun HourlyForecastItem(forecast: HourlyForecast) {
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
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified
        )
        Text(
            text = stringResource(clothing.toLabelRes()),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 우산/눈 추천
        if (forecast.precipitationProbability >= 40) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_recommend_umbrella),
                contentDescription = stringResource(CoreR.string.item_umbrella),
                modifier = Modifier.size(16.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(CoreR.string.item_umbrella),
                fontSize = 9.sp,
                color = WeatherColors.Precipitation
            )
        } else if (forecast.snowfall > 0) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_recommend_snow),
                contentDescription = stringResource(CoreR.string.item_snow),
                modifier = Modifier.size(16.dp),
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
private fun WeatherStat(emoji: String, label: String, value: String) {
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

// ── 추천 아이템 섹션 ──────────────────────────────────────────────────────────

@Composable
private fun RecommendationSection(weather: WeatherData?, airQuality: AirQuality?, modifier: Modifier = Modifier) {
    val recommendations = mutableListOf<Pair<Int, String>>() // drawableRes to label
    
    // 1. 체감온도 기반 옷 추천
    weather?.feelsLike?.let { temp ->
        val clothing = RecommendedClothing.fromFeelsLike(temp)
        recommendations.add(clothing.toIconRes() to stringResource(clothing.toDetailedLabelRes()))
        if (clothing == RecommendedClothing.TANK_TOP) {
            recommendations.add(CoreR.drawable.ic_recommend_parasol to stringResource(CoreR.string.item_parasol))
        }
    }
    
    // 2. 강수확률 기반 우산 (6시간 내 최대값)
    weather?.hourlyForecast?.take(6)?.maxOfOrNull { it.precipitationProbability }?.let { maxPrecip ->
        when {
            maxPrecip >= 40 -> recommendations.add(CoreR.drawable.ic_recommend_umbrella to stringResource(CoreR.string.item_umbrella_with_prob, maxPrecip))
            maxPrecip >= 20 -> recommendations.add(CoreR.drawable.ic_recommend_umbrella_light to "${stringResource(CoreR.string.item_umbrella_take)} (${maxPrecip}%)")
        }
    }
    
    // 3. 적설량 기반 (시간별 예보 중 합계)
    weather?.hourlyForecast?.sumOf { it.snowfall }?.let { totalSnow ->
        if (totalSnow > 0) {
            recommendations.add(CoreR.drawable.ic_recommend_snow to stringResource(CoreR.string.item_snow_cm, totalSnow))
        }
    }
    
    // 4. 대기질 민감군 주의 이상이면 마스크
    airQuality?.worstLevel?.let { level ->
        if (level.ordinal >= AqiLevel.UNHEALTHY_SENSITIVE.ordinal && level != AqiLevel.UNKNOWN) {
            recommendations.add(CoreR.drawable.ic_recommend_mask to stringResource(CoreR.string.item_mask, stringResource(level.toLabelRes())))
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

// ── 공기질 섹션 ───────────────────────────────────────────────────────────────

@Composable
private fun AirQualitySection(air: AirQuality, modifier: Modifier = Modifier) {
    val worstColor = air.worstLevel.toColor()

    Column(modifier = modifier.fillMaxWidth()) {
        // 섹션 라벨
        Text(
            text = stringResource(CoreR.string.section_air_quality),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        
        // 통합 공기질 등급 (최악 기준)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(worstColor.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = air.worstLevel.toEmoji(),
                    fontSize = 32.sp
                )
            }
            Column {
                Text(
                    text = stringResource(air.worstLevel.toLabelRes()),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = worstColor
                )
                Text(
                    text = stringResource(CoreR.string.air_quality_integrated),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 초미세먼지 (PM2.5)
        PollutantRow(
            label = stringResource(CoreR.string.label_pm25),
            pmData = air.pm25,
            mainStationName = air.mainStationName
        )
        Spacer(Modifier.height(8.dp))
        // 미세먼지 (PM10)
        PollutantRow(
            label = stringResource(CoreR.string.label_pm10),
            pmData = air.pm10,
            mainStationName = air.mainStationName
        )
        
        Spacer(Modifier.height(8.dp))
        
        // 측정소 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(CoreR.string.label_station, air.mainStationName),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (air.updatedAt.isNotBlank()) {
                Text(
                    text = air.updatedAt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PollutantRow(label: String, pmData: PmData?, mainStationName: String) {
    val level = pmData?.level ?: AqiLevel.UNKNOWN
    val color = level.toColor()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 라벨 + 값
            Column {
                Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = pmData?.let { "%.1f μg/m³".format(it.value) } ?: "- μg/m³",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // WHO 등급 칩
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${level.toEmoji()} ${stringResource(level.toLabelRes())}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
        
        // 폴백 측정소에서 가져온 경우 출처 표시
        if (pmData != null && pmData.isFromFallback) {
            Text(
                text = stringResource(CoreR.string.air_quality_from_station, pmData.stationName),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

// ── 부분 오류 섹션 (날씨 또는 공기질 하나만 실패) ─────────────────────────────

@Composable
private fun ErrorSection(title: String, message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(CoreR.string.error_section_failed, title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        TextButton(onClick = onRetry) { Text(stringResource(CoreR.string.button_retry)) }
    }
}

// ── 전체 오류 (위치 실패 등) ──────────────────────────────────────────────────

// ── 권한 필요 화면 ───────────────────────────────────────────────────────

@Composable
internal fun PermissionRequiredContent(
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text("📍", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "위치 권한이 필요합니다",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "현재 위치의 날씨와 미세먼지 정보를\n확인하려면 위치 권한을 허용해주세요.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text("설정으로 이동")
        }
    }
}

// ── 전체 오류 (위치 실패 등) ──────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    error: WeatherContract.ErrorState,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("😵", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(error.message.asString(), textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onIntent(error.action) }) {
            Text(stringResource(CoreR.string.button_retry_again))
        }
    }
}

