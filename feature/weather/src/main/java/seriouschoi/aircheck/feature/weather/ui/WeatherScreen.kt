@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package seriouschoi.aircheck.feature.weather.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.domain.model.WeatherData

@Composable
internal fun WeatherScreen(
    state: WeatherContract.State,
    multiState: MultiLocationState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onAddLocation: () -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val isRefreshing = (state as? WeatherContract.State.Success)?.isRefreshing == true || multiState.isLoading
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onAddLocation) {
                        Icon(Icons.Default.Add, contentDescription = "위치 추가")
                    }
                    IconButton(onClick = { showInfoDialog = true }) {
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
        if (showInfoDialog) {
            AirQualityInfoDialog(onDismiss = { showInfoDialog = false })
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (multiState.locations.isEmpty()) {
                // 위치 없음 또는 로딩 중
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { onIntent(WeatherContract.Intent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            is WeatherContract.State.Loading -> { }
                            is WeatherContract.State.Success -> SuccessContent(state.airQuality, state.weather, onIntent)
                            is WeatherContract.State.Error -> ErrorContent(state.message.asString(), onIntent)
                        }
                    }
                }
            } else {
                // 다중 위치 페이저
                val pagerState = rememberPagerState(pageCount = { multiState.locations.size })
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val locationData = multiState.locations[page]
                    
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { onIntent(WeatherContract.Intent.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 위치명 표시
                            Text(
                                text = if (locationData.isCurrentLocation) "📍 현재 위치" else "📌 ${locationData.locationName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 8.dp)
                            )
                            
                            when (val locationState = locationData.state) {
                                is WeatherContract.State.Loading -> { }
                                is WeatherContract.State.Success -> SuccessContent(locationState.airQuality, locationState.weather, onIntent)
                                is WeatherContract.State.Error -> ErrorContent(locationState.message.asString(), onIntent)
                            }
                        }
                    }
                }
                
                // 페이지 인디케이터
                if (multiState.locations.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(multiState.locations.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}



// ── Success ───────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(
    air: WeatherContract.DataResult<AirQuality>,
    weather: WeatherContract.DataResult<WeatherData>,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 위치 (공기질 데이터 있을 때만)
        air.data?.mainStationName?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // ── 추천 아이템 (가장 위에) ─────────────────────────────
        if (weather.data != null || air.data != null) {
            RecommendationSection(weather = weather.data, airQuality = air.data)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // ── 날씨 섹션 ────────────────────────────────────────
        when {
            weather.data != null -> {
                WeatherSection(weather.data)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            weather.error != null -> ErrorSection(
                title = stringResource(CoreR.string.weather_title), 
                message = weather.error.asString(), 
                onRetry = { onIntent(WeatherContract.Intent.Refresh) }
            )
        }

        // ── 공기질 섹션 ──────────────────────────────────────
        when {
            air.data != null -> AirQualitySection(air.data)
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
private fun WeatherSection(weather: WeatherData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 현재 날씨 라벨
        Text(
            text = "🌤️ 현재 날씨",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        
        // 날씨 상태 + 이모지
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(weather.condition.emoji, fontSize = 36.sp)
            Column {
                Text(
                    text = weather.condition.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (weather.precipitation > 0) {
                    val precipLabel = when (weather.condition) {
                        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWER -> "적설 ${weather.precipitation}mm"
                        else -> "강수량 ${weather.precipitation}mm"
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
                text = "📅 48시간 예보",
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
        Text(forecast.condition.emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        
        // 기온
        Text(
            text = "%.0f°".format(forecast.temperature),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 체감온도
        Text(
            text = "체감 %.0f°".format(forecast.feelsLike),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(4.dp))
        
        // 강수/강설 확률
        if (forecast.snowfall > 0) {
            Text(
                text = "❄️${forecast.precipitationProbability}%",
                fontSize = 10.sp,
                color = Color(0xFF64B5F6)
            )
        } else {
            Text(
                text = "🌧️${forecast.precipitationProbability}%",
                fontSize = 10.sp,
                color = if (forecast.precipitationProbability >= 40) 
                    Color(0xFF2196F3) 
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
        
        // 추천 아이템 (체감온도 기반)
        val (outfitEmoji, outfitLabel) = getOutfitRecommendation(forecast.feelsLike)
        Text(outfitEmoji, fontSize = 14.sp)
        Text(
            text = outfitLabel,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 우산/눈 추천
        if (forecast.precipitationProbability >= 40) {
            Text("☂️", fontSize = 12.sp)
            Text(
                text = "우산",
                fontSize = 9.sp,
                color = Color(0xFF2196F3)
            )
        } else if (forecast.snowfall > 0) {
            Text("⛄", fontSize = 12.sp)
            Text(
                text = "눈",
                fontSize = 9.sp,
                color = Color(0xFF64B5F6)
            )
        }
    }
}

/** 체감온도 기반 옷 추천 (이모지, 라벨) */
private fun getOutfitRecommendation(feelsLike: Double): Pair<String, String> = when {
    feelsLike >= 28 -> "👕" to "민소매"
    feelsLike >= 23 -> "👕" to "반팔"
    feelsLike >= 17 -> "👔" to "긴팔"
    feelsLike >= 12 -> "🧥" to "자켓"
    feelsLike >= 6 -> "🧥" to "코트"
    feelsLike >= 0 -> "🧥" to "패딩"
    else -> "🧣" to "두꺼운패딩"
}

@Composable
private fun WeatherStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── 추천 아이템 섹션 ──────────────────────────────────────────────────────────

@Composable
private fun RecommendationSection(weather: WeatherData?, airQuality: AirQuality?) {
    val recommendations = mutableListOf<Pair<String, String>>() // emoji to label
    
    // 1. 체감온도 기반 옷 추천
    weather?.feelsLike?.let { temp ->
        when {
            temp >= 28 -> {
                recommendations.add("👕" to "민소매/반팔")
                recommendations.add("🏖️" to "양산")
            }
            temp >= 23 -> recommendations.add("👕" to "반팔")
            temp >= 17 -> recommendations.add("👔" to "얇은 긴팔")
            temp >= 12 -> recommendations.add("🧥" to "자켓/가디건")
            temp >= 6 -> recommendations.add("🧥" to "코트/니트")
            temp >= 0 -> recommendations.add("🧥" to "패딩/목도리")
            else -> recommendations.add("🧣" to "두꺼운 패딩")
        }
    }
    
    // 2. 강수확률 기반 우산 (6시간 내 최대값)
    weather?.hourlyForecast?.take(6)?.maxOfOrNull { it.precipitationProbability }?.let { maxPrecip ->
        when {
            maxPrecip >= 40 -> recommendations.add("☂️" to "우산 (${maxPrecip}%)")
            maxPrecip >= 20 -> recommendations.add("🌂" to "우산 챙기기 (${maxPrecip}%)")
        }
    }
    
    // 3. 적설량 기반 (시간별 예보 중 합계)
    weather?.hourlyForecast?.sumOf { it.snowfall }?.let { totalSnow ->
        if (totalSnow > 0) {
            recommendations.add("⛄" to "눈 %.1fcm".format(totalSnow))
        }
    }
    
    // 4. 대기질 민감군 주의 이상이면 마스크
    airQuality?.worstLevel?.let { level ->
        if (level.ordinal >= AqiLevel.UNHEALTHY_SENSITIVE.ordinal && level != AqiLevel.UNKNOWN) {
            recommendations.add("😷" to "마스크 (${level.label})")
        }
    }
    
    if (recommendations.isEmpty()) return
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "🎒 오늘의 추천",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "현재 날씨 + 6시간 내 예보 기준",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            recommendations.forEach { (emoji, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(emoji, fontSize = 28.sp)
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
private fun AirQualitySection(air: AirQuality) {
    val worstColor = Color(air.worstLevel.colorHex)

    Column(modifier = Modifier.fillMaxWidth()) {
        // 섹션 라벨
        Text(
            text = "💨 공기질",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        
        // 통합 공기질 등급 (최악 기준)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(worstColor.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = air.worstLevel.emoji,
                    fontSize = 28.sp
                )
            }
            Column {
                Text(
                    text = air.worstLevel.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = worstColor
                )
                Text(
                    text = stringResource(CoreR.string.air_quality_integrated),
                    fontSize = 11.sp,
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
                text = "📍 ${air.mainStationName}",
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
    val color = Color(level.colorHex)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 라벨 + 값
            Column {
                Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = pmData?.let { "%.1f μg/m³".format(it.value) } ?: "- μg/m³",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // WHO 등급 칩
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${level.emoji} ${level.label}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
        
        // 폴백 측정소에서 가져온 경우 출처 표시
        if (pmData != null && pmData.isFromFallback) {
            Text(
                text = "↳ ${pmData.stationName}에서 가져옴",
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
                text = "$title 정보를 불러오지 못했어요",
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

@Composable
private fun ErrorContent(message: String, onIntent: (WeatherContract.Intent) -> Unit) {
    ErrorContentInner(message = message, onRetry = { onIntent(WeatherContract.Intent.Refresh) })
}

@Composable
private fun ErrorContentInner(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("😵", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(CoreR.string.button_retry_again)) }
    }
}

// ── 공기질 기준 안내 다이얼로그 ────────────────────────────────────────────────

@Composable
private fun AirQualityInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(CoreR.string.dialog_air_quality_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 통합 등급 설명
                Text(
                    text = "통합 공기질 등급",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "초미세먼지(PM2.5), 미세먼지(PM10) 중 가장 나쁜 등급을 기준으로 표시합니다.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // 초미세먼지 (PM2.5) - WHO 2021
                Text(
                    text = "초미세먼지 (PM2.5)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = listOf(
                        Triple("😊 좋음", "0 ~ 15", Color(0xFF00C853)),
                        Triple("🙂 보통", "16 ~ 25", Color(0xFFFFD600)),
                        Triple("😐 민감군 주의", "26 ~ 37.5", Color(0xFFFF9100)),
                        Triple("😷 나쁨", "37.6 ~ 50", Color(0xFFFF5722)),
                        Triple("🤢 매우 나쁨", "51 ~ 75", Color(0xFF9C27B0)),
                        Triple("☠️ 위험", "76+", Color(0xFF7E0023))
                    )
                )
                
                HorizontalDivider()
                
                // 미세먼지 (PM10) - WHO 2021
                Text(
                    text = "미세먼지 (PM10)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = listOf(
                        Triple("😊 좋음", "0 ~ 45", Color(0xFF00C853)),
                        Triple("🙂 보통", "46 ~ 50", Color(0xFFFFD600)),
                        Triple("😐 민감군 주의", "51 ~ 75", Color(0xFFFF9100)),
                        Triple("😷 나쁨", "76 ~ 100", Color(0xFFFF5722)),
                        Triple("🤢 매우 나쁨", "101 ~ 150", Color(0xFF9C27B0)),
                        Triple("☠️ 위험", "151+", Color(0xFF7E0023))
                    )
                )
                
                HorizontalDivider()
                
                // 출처
                Text(
                    text = "📋 기준 출처",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "WHO Global Air Quality Guidelines 2021\nISBN: 978-92-4-003422-8",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

@Composable
private fun GradeTable(grades: List<Triple<String, String, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        grades.forEach { (label, range, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = color
                )
                Text(
                    text = "$range μg/m³",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
