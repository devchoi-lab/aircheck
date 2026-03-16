@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package seriouschoi.aircheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AirCheckScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("에어체크", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "기준 안내")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        }
    ) { innerPadding ->
    
        // 기준 안내 다이얼로그
        if (showInfoDialog) {
            AirQualityInfoDialog(onDismiss = { showInfoDialog = false })
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is MainUiState.Idle -> IdleContent(onRefresh)
                is MainUiState.Loading -> LoadingContent()
                is MainUiState.Success -> SuccessContent(uiState.airQuality, uiState.weather, onRefresh)
                is MainUiState.Error -> ErrorContent(uiState.message, onRefresh)
            }
        }
    }
}

// ── Idle ──────────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🌬️", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("현재 위치의 날씨와 공기질을 확인해보세요", fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart) { Text("확인하기") }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("날씨 및 공기질 가져오는 중...")
    }
}

// ── Success ───────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(
    air: DataResult<AirQuality>,
    weather: DataResult<WeatherData>,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        // ── 날씨 카드 ────────────────────────────────────────
        when {
            weather.data != null -> WeatherCard(weather.data)
            weather.error != null -> ErrorCard(title = "날씨", message = weather.error, onRetry = onRefresh)
        }

        // ── 공기질 카드 ──────────────────────────────────────
        when {
            air.data != null -> AirQualityCard(air.data)
            air.error != null -> ErrorCard(title = "공기질", message = air.error, onRetry = onRefresh)
        }

        // 업데이트 시간
        air.data?.updatedAt?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = "공기질 업데이트: $it",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ── 날씨 카드 ─────────────────────────────────────────────────────────────────

@Composable
private fun WeatherCard(weather: WeatherData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (weather.precipitation > 0) {
                        val precipLabel = when (weather.condition) {
                            WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWER -> "적설 ${weather.precipitation}mm"
                            else -> "강수량 ${weather.precipitation}mm"
                        }
                        Text(
                            text = precipLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            // 기온 / 체감온도 / 햇빛
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat(
                    emoji = "🌡️",
                    label = "기온",
                    value = "%.1f°C".format(weather.temperature)
                )
                WeatherStat(
                    emoji = "🤔",
                    label = "체감온도",
                    value = "%.1f°C".format(weather.feelsLike)
                )
                WeatherStat(
                    emoji = if (weather.isDay) "☀️" else "🌙",
                    label = if (weather.isDay) "햇빛" else "달빛",
                    value = when {
                        weather.cloudCover <= 20 -> "강함"
                        weather.cloudCover <= 50 -> "보통"
                        weather.cloudCover <= 80 -> "약함"
                        else -> "없음"
                    }
                )
            }
            
            // 48시간 예보
            if (weather.hourlyForecast.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "48시간 예보",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(weather.hourlyForecast) { forecast ->
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
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 시간
        Text(
            text = forecast.hour,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(4.dp))
        
        // 날씨 이모지
        Text(forecast.condition.emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        
        // 기온
        Text(
            text = "%.0f°".format(forecast.temperature),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        // 체감온도
        Text(
            text = "체감 %.0f°".format(forecast.feelsLike),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        
        // 강수확률 (0%가 아닐 때만)
        if (forecast.precipitationProbability > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "💧${forecast.precipitationProbability}%",
                fontSize = 10.sp,
                color = Color(0xFF2196F3)
            )
        }
        
        // 눈 (0cm가 아닐 때만)
        if (forecast.snowfall > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "❄️%.1fcm".format(forecast.snowfall),
                fontSize = 10.sp,
                color = Color(0xFF64B5F6)
            )
        }
    }
}

@Composable
private fun WeatherStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

// ── 공기질 카드 ───────────────────────────────────────────────────────────────

@Composable
private fun AirQualityCard(air: AirQuality) {
    // 통합 등급: AQI/PM2.5/PM10 중 최악
    val worstColor = Color(air.worstLevel.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 통합 공기질 등급 (최악 기준)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(worstColor.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = air.worstLevel.emoji,
                        fontSize = 32.sp
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
                        text = "통합 공기질 (AQI/초미세먼지/미세먼지 중 최악)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // 초미세먼지 (PM2.5)
            PollutantRow(
                label = "초미세먼지",
                pmData = air.pm25,
                mainStationName = air.mainStationName
            )
            Spacer(Modifier.height(8.dp))
            // 미세먼지 (PM10)
            PollutantRow(
                label = "미세먼지",
                pmData = air.pm10,
                mainStationName = air.mainStationName
            )
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
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

// ── 부분 오류 카드 (날씨 또는 공기질 하나만 실패) ─────────────────────────────

@Composable
private fun ErrorCard(title: String, message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
            TextButton(onClick = onRetry) { Text("재시도") }
        }
    }
}

// ── 전체 오류 (위치 실패 등) ──────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("😵", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}

// ── 공기질 기준 안내 다이얼로그 ────────────────────────────────────────────────

@Composable
private fun AirQualityInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "공기질 기준 안내",
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
                    text = "AQI(통합대기환경지수), 초미세먼지, 미세먼지 중 가장 나쁜 등급을 기준으로 표시합니다.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // 초미세먼지 (PM2.5)
                Text(
                    text = "초미세먼지 (PM2.5)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = listOf(
                        Triple("🌟 최고", "0 ~ 5", Color(0xFF00E5FF)),
                        Triple("😊 좋음", "6 ~ 9", Color(0xFF00C8A0)),
                        Triple("🙂 양호", "10 ~ 12", Color(0xFF8BC34A)),
                        Triple("😐 보통", "13 ~ 15", Color(0xFFFFD700)),
                        Triple("😮 나쁨", "16 ~ 37", Color(0xFFFF9800)),
                        Triple("😷 상당히 나쁨", "38 ~ 50", Color(0xFFFF5722)),
                        Triple("🤢 매우 나쁨", "51 ~ 75", Color(0xFF9400D3)),
                        Triple("☠️ 최악", "76+", Color(0xFF7E0023))
                    )
                )
                
                HorizontalDivider()
                
                // 미세먼지 (PM10)
                Text(
                    text = "미세먼지 (PM10)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = listOf(
                        Triple("🌟 최고", "0 ~ 10", Color(0xFF00E5FF)),
                        Triple("😊 좋음", "11 ~ 22", Color(0xFF00C8A0)),
                        Triple("🙂 양호", "23 ~ 35", Color(0xFF8BC34A)),
                        Triple("😐 보통", "36 ~ 45", Color(0xFFFFD700)),
                        Triple("😮 나쁨", "46 ~ 75", Color(0xFFFF9800)),
                        Triple("😷 상당히 나쁨", "76 ~ 100", Color(0xFFFF5722)),
                        Triple("🤢 매우 나쁨", "101 ~ 150", Color(0xFF9400D3)),
                        Triple("☠️ 최악", "151+", Color(0xFF7E0023))
                    )
                )
                
                HorizontalDivider()
                
                // AQI 기준 (에어코리아)
                Text(
                    text = "AQI (통합대기환경지수) - 에어코리아",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = listOf(
                        Triple("😊 좋음", "0 ~ 50", Color(0xFF00C8A0)),
                        Triple("😐 보통", "51 ~ 100", Color(0xFFFFD700)),
                        Triple("😮 나쁨", "101 ~ 250", Color(0xFFFF9800)),
                        Triple("🤢 매우 나쁨", "251+", Color(0xFF9400D3))
                    )
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
