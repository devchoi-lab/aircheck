package seriouschoi.aircheck

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import seriouschoi.aircheck.feature.weather.navigation.WeatherRoute
import seriouschoi.aircheck.feature.weather.navigation.weatherScreen
import seriouschoi.aircheck.ui.theme.AirCheckTheme
import seriouschoi.aircheck.feature.widget.AircheckWidgetProvider
import seriouschoi.aircheck.core.resource.R as CoreR

/** 권한 승인 콜백을 전달하기 위한 CompositionLocal */
val LocalPermissionGranted = staticCompositionLocalOf<() -> Unit> { {} }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirCheckTheme {
                val navController = rememberNavController()

                // 백그라운드 위치 권한 다이얼로그
                var showBgLocationDialog by remember {
                    mutableStateOf(
                        intent?.getBooleanExtra(AircheckWidgetProvider.EXTRA_REQUEST_BG_LOCATION, false) == true
                    )
                }

                if (showBgLocationDialog) {
                    AlertDialog(
                        onDismissRequest = { showBgLocationDialog = false },
                        title = { Text(stringResource(CoreR.string.bg_location_rationale_title)) },
                        text = { Text(stringResource(CoreR.string.bg_location_rationale_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showBgLocationDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", packageName, null)
                                    }
                                    startActivity(settingsIntent)
                                }
                            }) {
                                Text(stringResource(CoreR.string.bg_location_go_settings))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBgLocationDialog = false }) {
                                Text(stringResource(CoreR.string.button_cancel))
                            }
                        }
                    )
                }

                // 권한 승인 시 호출될 콜백
                var onPermissionGranted by remember { mutableStateOf<(() -> Unit)?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // 위치 권한 중 하나라도 승인되면 콜백 호출
                    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                  permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        onPermissionGranted?.invoke()
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                CompositionLocalProvider(
                    LocalPermissionGranted provides { onPermissionGranted?.invoke() }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = WeatherRoute.Main
                    ) {
                        weatherScreen(
                            navController = navController,
                            onRegisterPermissionCallback = { callback ->
                                onPermissionGranted = callback
                            }
                        )
                    }
                }
            }
        }
    }
}
