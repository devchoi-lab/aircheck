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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import seriouschoi.aircheck.core.ui.theme.AirCheckPreview
import seriouschoi.aircheck.core.ui.theme.AirCheckPreviewWrapper
import seriouschoi.aircheck.feature.weather.ui.calendar.CalendarPage
import seriouschoi.aircheck.feature.weather.ui.calendar.CalendarIntent
import seriouschoi.aircheck.feature.weather.ui.calendar.CalendarUiState
import seriouschoi.aircheck.feature.weather.ui.weather.UiLocationInfo
import seriouschoi.aircheck.feature.weather.ui.weather.UiWeatherResult
import seriouschoi.aircheck.feature.weather.ui.weather.UiWeatherScreenState
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun LocationPager(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    state: UiWeatherScreenState,
    calendarState: CalendarUiState,
    onIntent: (WeatherContract.Intent) -> Unit,
    onCalendarIntent: (CalendarIntent) -> Unit,
    onManageLocations: () -> Unit = {},
    includeCalendar: Boolean = true,
) {
    val locations = state.locations
    val pageOffset = if (includeCalendar) 1 else 0
    val currentLocationPage = if (includeCalendar) pagerState.currentPage - 1 else pagerState.currentPage
    val currentLocation = locations.getOrNull(currentLocationPage)
    val isOnCalendarPage = includeCalendar && pagerState.currentPage == 0

    Column(modifier = modifier) {
        // 상단 헤더 (인디케이터 + 타이틀)
        LocationHeader(
            locations = locations,
            currentPage = pagerState.currentPage,
            includeCalendar = includeCalendar,
            onManageLocations = onManageLocations
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (includeCalendar && page == 0) {
                // 캘린더 페이지
                CalendarPage(
                    state = calendarState,
                    onIntent = onCalendarIntent,
                )
            } else {
                // 위치 페이지
                val locationIndex = if (includeCalendar) page - 1 else page
                val locationInfo = locations.getOrNull(locationIndex)
                if (locationInfo != null) {
                    val weatherResult = state.weathers[locationInfo.bjdCode] ?: UiWeatherResult()
                    LocationPage(
                        locationInfo = locationInfo,
                        weatherResult = weatherResult,
                        pageIndex = locationIndex,
                        onIntent = onIntent
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationHeader(
    locations: List<UiLocationInfo>,
    currentPage: Int,
    includeCalendar: Boolean,
    onManageLocations: () -> Unit = {},
) {
    val totalIndicators = locations.size + (if (includeCalendar) 1 else 0)
    
    if (totalIndicators > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onManageLocations)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 캘린더 인디케이터
            if (includeCalendar) {
                val isSelected = currentPage == 0
                val color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "캘린더",
                    tint = color,
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (isSelected) 16.dp else 12.dp)
                )
            }
            
            // 위치 인디케이터
            locations.forEachIndexed { index, locationInfo ->
                val pageIndex = if (includeCalendar) index + 1 else index
                val isSelected = currentPage == pageIndex
                val color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                if (locationInfo.isCurrentLocation) {
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

@AirCheckPreview
@Composable
private fun LocationPagerPreviewSingle() {
    AirCheckPreviewWrapper {
        val locations = listOf(
            UiLocationInfo(name = "서울", isCurrentLocation = true),
        )
        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { locations.size + 1 }
        )
        LocationPager(
            pagerState = pagerState,
            state = UiWeatherScreenState(
                locations = locations, isLoading = false
            ),
            calendarState = CalendarUiState(),
            onIntent = {},
            onCalendarIntent = {},
            includeCalendar = true,
        )
    }
}

@AirCheckPreview
@Composable
private fun LocationPagerPreviewMultiple() {
    AirCheckPreviewWrapper {
        val locations = listOf(
            UiLocationInfo(name = "서울", isCurrentLocation = true),
            UiLocationInfo(name = "부산", isCurrentLocation = false),
            UiLocationInfo(name = "제주", isCurrentLocation = false),
        )
        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { locations.size + 1 }
        )
        LocationPager(
            pagerState = pagerState,
            state = UiWeatherScreenState(
                locations = locations, isLoading = false
            ),
            calendarState = CalendarUiState(),
            onIntent = {},
            onCalendarIntent = {},
            includeCalendar = true,
        )
    }
}

@AirCheckPreview
@Composable
private fun LocationPagerPreviewCalendarPage() {
    AirCheckPreviewWrapper {
        val locations = listOf(
            UiLocationInfo(name = "서울", isCurrentLocation = true),
        )
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { locations.size + 1 }
        )
        LocationPager(
            pagerState = pagerState,
            state = UiWeatherScreenState(
                locations = locations, isLoading = false
            ),
            calendarState = CalendarUiState(
                isLoading = false,
                hasCalendarPermission = false,
            ),
            onIntent = {},
            onCalendarIntent = {},
            includeCalendar = true,
        )
    }
}
