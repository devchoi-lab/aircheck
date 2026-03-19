package seriouschoi.aircheck

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import seriouschoi.aircheck.feature.weather.navigation.WeatherRoute
import seriouschoi.aircheck.feature.weather.navigation.weatherScreen
import seriouschoi.aircheck.ui.theme.AirCheckTheme

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
