@file:OptIn(ExperimentalMaterial3Api::class)

package seriouschoi.aircheck.feature.weather.ui.weather.pager

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import seriouschoi.aircheck.feature.weather.ui.weather.UiLocationInfo
import seriouschoi.aircheck.feature.weather.ui.weather.UiWeatherResult
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.feature.weather.ui.weather.components.PermissionRequiredContent

@Composable
internal fun LocationPage(
    locationInfo: UiLocationInfo,
    weatherResult: UiWeatherResult,
    pageIndex: Int,
    onIntent: (WeatherContract.Intent) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = weatherResult.isRefreshing,
        onRefresh = { onIntent(WeatherContract.Intent.RefreshLocation(pageIndex)) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (locationInfo.needsPermission) {
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
                WeatherDetailPanel(
                    weatherResult.airQuality,
                    weatherResult.weather,
                    weatherResult.recommendations,
                    pageIndex,
                    onIntent
                )
            }
        }
    }
}