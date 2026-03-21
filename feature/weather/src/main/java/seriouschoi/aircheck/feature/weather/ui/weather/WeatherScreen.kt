@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.feature.weather.ui.weather.components.AirQualitySection
import seriouschoi.aircheck.feature.weather.ui.weather.components.ErrorSection
import seriouschoi.aircheck.feature.weather.ui.weather.components.RecommendationSection
import seriouschoi.aircheck.feature.weather.ui.weather.components.WeatherSection
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun WeatherScreen(
    state: WeatherScreenState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onAddLocation: () -> Unit = {}
) {
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
        if (state.uiControl.showInfoDialog) {
            AirQualityInfoDialog(onDismiss = { onIntent(WeatherContract.Intent.DismissInfoDialog) })
        }

        // 삭제 확인 다이얼로그
        state.uiControl.deleteConfirm?.let { confirm ->
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
            val locations = state.locations.locations
            
            if (locations.isEmpty() && state.isLoading) {
                // 초기 로딩 중
                Box(modifier = Modifier.fillMaxSize())
            } else if (locations.isNotEmpty()) {
                // 이전 위치 개수 추적 (새 위치 추가 감지용)
                var previousLocationCount by rememberSaveable { mutableIntStateOf(locations.size) }
                val pagerState = rememberPagerState(pageCount = { locations.size })
                
                // 새 위치 추가 시 해당 페이지로 스크롤
                LaunchedEffect(locations.size) {
                    if (locations.size > previousLocationCount) {
                        // 새 위치가 추가됨 → 마지막 페이지로 이동
                        pagerState.animateScrollToPage(locations.size - 1)
                    }
                    previousLocationCount = locations.size
                }

                LocationPager(
                    pagerState = pagerState,
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Success Content ───────────────────────────────────────────────────────────

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
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    WeatherSection(weather.data)
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
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
