@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.feature.weather.ui.weather.components.ErrorContent
import seriouschoi.aircheck.feature.weather.ui.weather.components.PermissionRequiredContent
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun LocationPager(
    pagerState: PagerState,
    state: WeatherScreenState,
    onIntent: (WeatherContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val locations = state.locations.locations
    
    Column(modifier = modifier) {
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

        // 페이지 인디케이터
        if (locations.size > 1) {
            PageIndicator(
                locations = locations,
                currentPage = pagerState.currentPage
            )
        }
    }
}

@Composable
private fun LocationPage(
    locationData: LocationWeatherData,
    pageIndex: Int,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = locationData.isRefreshing,
        onRefresh = { onIntent(WeatherContract.Intent.RefreshLocation(pageIndex)) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 위치명 + 삭제 버튼
            LocationHeader(locationData = locationData, onIntent = onIntent)

            // 권한 필요 시 권한 요청 화면, 아니면 날씨 데이터
            if (locationData.needsPermission) {
                val context = LocalContext.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionRequiredContent(
                        onOpenSettings = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            } else {
                SuccessContent(locationData.airQuality, locationData.weather, onIntent)
            }
        }
    }
}

@Composable
private fun LocationHeader(
    locationData: LocationWeatherData,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (locationData.isCurrentLocation) stringResource(CoreR.string.label_current_location)
                   else stringResource(CoreR.string.label_saved_location, locationData.locationName),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        // 저장된 위치만 삭제 버튼 표시
        if (!locationData.isCurrentLocation && locationData.savedLocationId != null) {
            IconButton(
                onClick = {
                    onIntent(WeatherContract.Intent.RequestDeleteLocation(
                        locationId = locationData.savedLocationId,
                        locationName = locationData.locationName
                    ))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(CoreR.string.content_description_delete_location),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    locations: List<LocationWeatherData>,
    currentPage: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        locations.forEachIndexed { index, locationData ->
            val isSelected = currentPage == index
            val color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

            if (locationData.isCurrentLocation) {
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
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
