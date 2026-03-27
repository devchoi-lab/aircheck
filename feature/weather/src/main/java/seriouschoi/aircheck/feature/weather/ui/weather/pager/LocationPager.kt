@file:OptIn(ExperimentalFoundationApi::class)

package seriouschoi.aircheck.feature.weather.ui.weather.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import seriouschoi.aircheck.core.ui.theme.AirCheckPreview
import seriouschoi.aircheck.core.ui.theme.AirCheckPreviewWrapper
import seriouschoi.aircheck.feature.weather.ui.weather.UiLocationInfo
import seriouschoi.aircheck.feature.weather.ui.weather.UiLocationWeatherData
import seriouschoi.aircheck.feature.weather.ui.weather.UiLocationsState
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.feature.weather.ui.weather.UiWeatherScreenState
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun LocationPager(
    pagerState: PagerState,
    state: UiWeatherScreenState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onManageLocations: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val locations = state.locations.locations
    val currentLocation = locations.getOrNull(pagerState.currentPage)

    Column(modifier = modifier) {
        // 상단 헤더 (인디케이터 + 타이틀)
        if (currentLocation != null) {
            LocationHeader(
                locations = locations,
                currentPage = pagerState.currentPage,
                onManageLocations = onManageLocations
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            LocationPage(
                locationData = locations[page],
                pageIndex = page,
                onIntent = onIntent
            )
        }
    }
}

@AirCheckPreview
@Composable
private fun LocationPagerPreviewSingle() {
    AirCheckPreviewWrapper {
        val locations = listOf(
            UiLocationWeatherData(
                locationInfo = UiLocationInfo(name = "서울", isCurrentLocation = true),
            )
        )
        val pagerState = rememberPagerState(pageCount = { locations.size })
        LocationPager(
            pagerState = pagerState,
            state = UiWeatherScreenState(
                locations = UiLocationsState(locations = locations, isLoading = false)
            ),
            onIntent = {}
        )
    }
}

@AirCheckPreview
@Composable
private fun LocationPagerPreviewMultiple() {
    AirCheckPreviewWrapper {
        val locations = listOf(
            UiLocationWeatherData(
                locationInfo = UiLocationInfo(name = "서울", isCurrentLocation = true),
            ),
            UiLocationWeatherData(
                locationInfo = UiLocationInfo(name = "부산", isCurrentLocation = false, savedLocationId = 1L),
            ),
            UiLocationWeatherData(
                locationInfo = UiLocationInfo(name = "제주", isCurrentLocation = false, savedLocationId = 2L),
            ),
        )
        val pagerState = rememberPagerState(pageCount = { locations.size })
        LocationPager(
            pagerState = pagerState,
            state = UiWeatherScreenState(
                locations = UiLocationsState(locations = locations, isLoading = false)
            ),
            onIntent = {}
        )
    }
}

@AirCheckPreview
@Composable
private fun LocationPagerPreviewPermission() {
    AirCheckPreviewWrapper {
        val locations = listOf(
            UiLocationWeatherData(
                locationInfo = UiLocationInfo(name = "현재 위치", isCurrentLocation = true, needsPermission = true),
            )
        )
        val pagerState = rememberPagerState(pageCount = { locations.size })
        LocationPager(
            pagerState = pagerState,
            state = UiWeatherScreenState(
                locations = UiLocationsState(locations = locations, isLoading = false)
            ),
            onIntent = {}
        )
    }
}

@Composable
private fun LocationHeader(
    locations: List<UiLocationWeatherData>,
    currentPage: Int,
    onManageLocations: () -> Unit = {},
) {
    if (locations.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onManageLocations)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            locations.forEachIndexed { index, locationData ->
                val isSelected = currentPage == index
                val color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                if (locationData.locationInfo.isCurrentLocation) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(CoreR.string.content_description_current_location),
                        tint = color,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (isSelected) 16.dp else 12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}
