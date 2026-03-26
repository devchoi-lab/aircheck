@file:OptIn(ExperimentalFoundationApi::class)

package seriouschoi.aircheck.feature.weather.ui.weather.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import seriouschoi.aircheck.core.ui.theme.AirCheckPreview
import seriouschoi.aircheck.core.ui.theme.AirCheckPreviewWrapper
import seriouschoi.aircheck.feature.weather.ui.weather.LocationInfo
import seriouschoi.aircheck.feature.weather.ui.weather.LocationWeatherData
import seriouschoi.aircheck.feature.weather.ui.weather.LocationsState
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherScreenState
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun LocationPager(
    pagerState: PagerState,
    state: WeatherScreenState,
    onIntent: (WeatherContract.Intent) -> Unit,
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
                currentLocation = currentLocation,
                onIntent = onIntent
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
            LocationWeatherData(
                locationInfo = LocationInfo(name = "서울", isCurrentLocation = true),
            )
        )
        val pagerState = rememberPagerState(pageCount = { locations.size })
        LocationPager(
            pagerState = pagerState,
            state = WeatherScreenState(
                locations = LocationsState(locations = locations, isLoading = false)
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
            LocationWeatherData(
                locationInfo = LocationInfo(name = "서울", isCurrentLocation = true),
            ),
            LocationWeatherData(
                locationInfo = LocationInfo(name = "부산", isCurrentLocation = false, savedLocationId = 1L),
            ),
            LocationWeatherData(
                locationInfo = LocationInfo(name = "제주", isCurrentLocation = false, savedLocationId = 2L),
            ),
        )
        val pagerState = rememberPagerState(pageCount = { locations.size })
        LocationPager(
            pagerState = pagerState,
            state = WeatherScreenState(
                locations = LocationsState(locations = locations, isLoading = false)
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
            LocationWeatherData(
                locationInfo = LocationInfo(name = "현재 위치", isCurrentLocation = true, needsPermission = true),
            )
        )
        val pagerState = rememberPagerState(pageCount = { locations.size })
        LocationPager(
            pagerState = pagerState,
            state = WeatherScreenState(
                locations = LocationsState(locations = locations, isLoading = false)
            ),
            onIntent = {}
        )
    }
}

@Composable
private fun LocationHeader(
    locations: List<LocationWeatherData>,
    currentPage: Int,
    currentLocation: LocationWeatherData,
    onIntent: (WeatherContract.Intent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 인디케이터 (왼쪽)
        if (locations.size > 1) {
            Row(
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

        // 타이틀 (중앙)
        Text(
            text = if (currentLocation.locationInfo.isCurrentLocation) {
                if (currentLocation.locationInfo.name.isNotEmpty() && currentLocation.locationInfo.name != "현재 위치") {
                    currentLocation.locationInfo.name
                } else {
                    stringResource(CoreR.string.label_current_location)
                }
            } else {
                currentLocation.locationInfo.name
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // 삭제 버튼 (오른쪽, 저장된 위치만)
        if (!currentLocation.locationInfo.isCurrentLocation && currentLocation.locationInfo.savedLocationId != null) {
            IconButton(
                onClick = {
                    onIntent(
                        WeatherContract.Intent.RequestDeleteLocation(
                            locationId = currentLocation.locationInfo.savedLocationId!!,
                            locationName = currentLocation.locationInfo.name
                        )
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(CoreR.string.content_description_delete_location),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (locations.size > 1) {
            // 균형을 위한 빈 공간
            Box(modifier = Modifier.size(32.dp))
        }
    }
}
