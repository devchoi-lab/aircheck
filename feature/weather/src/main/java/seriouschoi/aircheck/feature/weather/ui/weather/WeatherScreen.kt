@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package seriouschoi.aircheck.feature.weather.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import seriouschoi.aircheck.core.domain.model.LocationItem
import seriouschoi.aircheck.core.ui.ads.AdUnitIds
import seriouschoi.aircheck.core.ui.ads.BannerAd
import seriouschoi.aircheck.feature.weather.ui.weather.components.AirQualityInfoDialog
import seriouschoi.aircheck.feature.weather.ui.weather.pager.LocationPager
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun WeatherScreen(
    state: UiWeatherScreenState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onAddLocation: () -> Unit = {},
    onManageLocations: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
) {
    val locations = state.locations
    val pagerState = rememberPagerState(pageCount = { locations.size.coerceAtLeast(1) })
    val currentLocation = locations.getOrNull(pagerState.currentPage)
    
    // 설정에서 권한 활성화 후 복귀 시 감지
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    onIntent(WeatherContract.Intent.OnPermissionGranted)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentLocationLabel = stringResource(CoreR.string.label_current_location_name)
    val titleText = currentLocation?.let { loc ->
        if (loc.isCurrentLocation) {
            loc.name.takeIf {
                it.isNotEmpty() &&
                        it != LocationItem.CURRENT_LOCATION_PLACEHOLDER
            } ?: currentLocationLabel
        } else {
            loc.name
        }
    } ?: currentLocationLabel

    // 새로고침 시각 포맷
    val lastRefreshedText = currentLocation?.let { loc ->
        state.weathers[loc.bjdCode]?.lastRefreshedAt
    }?.let { instant ->
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        stringResource(CoreR.string.last_updated_at, localTime.format(formatter))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, fontWeight = FontWeight.Bold)
                        Text(
                            text = lastRefreshedText ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "캘린더 날씨"
                        )
                    }
                    IconButton(onClick = onAddLocation) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(CoreR.string.content_description_manage_locations)
                        )
                    }
                    IconButton(onClick = { onIntent(WeatherContract.Intent.ShowInfoDialog) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(CoreR.string.content_description_info)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        // 기준 안내 다이얼로그
        if (state.uiControl.showInfoDialog) {
            AirQualityInfoDialog(onDismiss = { onIntent(WeatherContract.Intent.DismissInfoDialog) })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 메인 콘텐츠
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(CoreR.string.loading_weather_data),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    locations.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(CoreR.string.empty_location_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        // 새 지역 추가시, 가장 끝 페이지로 자동 이동.
                        var previousLocationCount by rememberSaveable { mutableIntStateOf(locations.size) }
                        LaunchedEffect(locations.size) {
                            if (locations.size > previousLocationCount) {
                                pagerState.animateScrollToPage(locations.size - 1)
                            }
                            previousLocationCount = locations.size
                        }

                        // 페이지 변경 시 또는 앱 복귀 시 해당 위치 날씨 로드
                        LaunchedEffect(pagerState.currentPage) {
                            onIntent(WeatherContract.Intent.OnPageVisible(pagerState.currentPage))
                        }
                        LifecycleResumeEffect(Unit) {
                            onIntent(WeatherContract.Intent.OnPageVisible(pagerState.currentPage))
                            onPauseOrDispose { }
                        }

                        LocationPager(
                            pagerState = pagerState,
                            state = state,
                            onIntent = onIntent,
                            onManageLocations = onManageLocations,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 하단 배너 광고
            BannerAd(
                adUnitId = AdUnitIds.banner,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

