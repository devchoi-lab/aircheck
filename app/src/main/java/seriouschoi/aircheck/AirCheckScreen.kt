@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package seriouschoi.aircheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("에어체크", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is MainUiState.Idle -> IdleContent(onRefresh)
                is MainUiState.Loading -> LoadingContent()
                is MainUiState.Success -> SuccessContent(uiState.airQuality, uiState.weather)
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
private fun SuccessContent(air: AirQuality, weather: WeatherData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 위치
        Text(
            text = air.stationName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // ── 날씨 카드 ────────────────────────────────────────
        WeatherCard(weather)

        // ── 공기질 카드 ──────────────────────────────────────
        AirQualityCard(air)

        // 업데이트 시간
        if (air.updatedAt.isNotEmpty()) {
            Text(
                text = "공기질 업데이트: ${air.updatedAt}",
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
    val levelColor = Color(air.level.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // AQI 요약
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = air.aqi.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                        Text("AQI", fontSize = 10.sp, color = levelColor)
                    }
                }
                Column {
                    Text(
                        text = "${air.level.emoji} ${air.level.label}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "공기질",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // PM2.5
            PollutantRow(
                label = "PM2.5",
                value = air.pm25,
                level = air.pm25Level
            )
            Spacer(Modifier.height(8.dp))
            // PM10
            PollutantRow(
                label = "PM10",
                value = air.pm10,
                level = air.pm10Level
            )
        }
    }
}

@Composable
private fun PollutantRow(label: String, value: Double?, level: AqiLevel) {
    val color = Color(level.colorHex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 라벨
        Column {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value?.let { "%.1f μg/m³".format(it) } ?: "- μg/m³",
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
}

// ── Error ─────────────────────────────────────────────────────────────────────

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
