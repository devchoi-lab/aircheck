@file:OptIn(ExperimentalMaterial3Api::class)

package seriouschoi.aircheck.feature.weather.ui.weather.pager

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.feature.weather.ui.weather.LocationWeatherData
import seriouschoi.aircheck.feature.weather.ui.weather.SuccessContent
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.feature.weather.ui.weather.components.PermissionRequiredContent
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun LocationPage(
    locationData: LocationWeatherData,
    pageIndex: Int,
    onIntent: (WeatherContract.Intent) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = locationData.isRefreshing,
        onRefresh = { onIntent(WeatherContract.Intent.RefreshLocation(pageIndex)) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LocationHeader(locationData = locationData, onIntent = onIntent)

            if (locationData.needsPermission) {
                val context = LocalContext.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionRequiredContent(
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
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
    onIntent: (WeatherContract.Intent) -> Unit,
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
        if (!locationData.isCurrentLocation && locationData.savedLocationId != null) {
            IconButton(
                onClick = {
                    onIntent(
                        WeatherContract.Intent.RequestDeleteLocation(
                            locationId = locationData.savedLocationId,
                            locationName = locationData.locationName
                        )
                    )
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
