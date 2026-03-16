@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package seriouschoi.aircheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
                is MainUiState.Success -> SuccessContent(uiState.airQuality)
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
        Text("현재 위치의 공기질을 확인해보세요", fontSize = 16.sp, textAlign = TextAlign.Center)
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
        Text("위치 및 공기질 데이터 가져오는 중...")
    }
}

// ── Success ───────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(air: AirQuality) {
    val levelColor = Color(air.level.colorHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 위치
        Text(
            text = air.stationName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // AQI 메인 카드
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(air.level.emoji, fontSize = 48.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = air.aqi.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
                Text("AQI", fontSize = 12.sp, color = levelColor)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 레벨 배지
        Surface(
            color = levelColor,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = air.level.label,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        // 상세 수치 (PM2.5 / PM10)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PollutantCard(label = "PM2.5", value = air.pm25)
            PollutantCard(label = "PM10", value = air.pm10)
        }

        if (air.updatedAt.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "업데이트: ${air.updatedAt}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PollutantCard(label: String, value: Double?) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value?.let { "%.1f".format(it) } ?: "-",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text("μg/m³", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
