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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import seriouschoi.aircheck.feature.weather.ui.weather.components.AirQualityInfoDialog
import seriouschoi.aircheck.feature.weather.ui.weather.components.AirQualitySection
import seriouschoi.aircheck.feature.weather.ui.weather.components.ErrorSection
import seriouschoi.aircheck.feature.weather.ui.weather.components.RecommendationSection
import seriouschoi.aircheck.feature.weather.ui.weather.components.WeatherSection
import seriouschoi.aircheck.feature.weather.ui.weather.pager.LocationPager
import seriouschoi.aircheck.core.domain.model.LocationItem
import seriouschoi.aircheck.core.ui.ads.AdUnitIds
import seriouschoi.aircheck.core.ui.ads.BannerAd
import seriouschoi.aircheck.core.resource.R as CoreR
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun WeatherScreen(
    state: UiWeatherScreenState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onAddLocation: () -> Unit = {},
    onManageLocations: () -> Unit = {},
) {
    val locations = state.locations.locations
    val pagerState = rememberPagerState(pageCount = { locations.size.coerceAtLeast(1) })
    val currentLocation = locations.getOrNull(pagerState.currentPage)

    val currentLocationLabel = stringResource(CoreR.string.label_current_location_name)
    val titleText = currentLocation?.let { loc ->
        if (loc.locationInfo.isCurrentLocation) {
            loc.locationInfo.name.takeIf { 
                it.isNotEmpty() && 
                it != LocationItem.CURRENT_LOCATION_PLACEHOLDER 
            } ?: currentLocationLabel
        } else {
            loc.locationInfo.name
        }
    } ?: currentLocationLabel

    // 새로고침 시각 포맷
    val lastRefreshedText = currentLocation?.lastRefreshedAt?.let { instant ->
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
                        lastRefreshedText?.let {
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 메인 콘텐츠
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(CoreR.string.empty_location_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        var previousLocationCount by rememberSaveable { mutableIntStateOf(locations.size) }

                        LaunchedEffect(locations.size) {
                            if (locations.size > previousLocationCount) {
                                pagerState.animateScrollToPage(locations.size - 1)
                            }
                            previousLocationCount = locations.size
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

