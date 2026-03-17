package seriouschoi.aircheck

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import seriouschoi.aircheck.feature.weather.navigation.WeatherRoute
import seriouschoi.aircheck.feature.weather.navigation.weatherScreen
import seriouschoi.aircheck.ui.theme.AirCheckTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirCheckTheme {
                val navController = rememberNavController()
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* 권한 결과는 WeatherViewModel에서 위치 요청 시 처리 */ }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = WeatherRoute.Main
                ) {
                    weatherScreen(navController)
                }
            }
        }
    }
}
