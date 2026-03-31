package seriouschoi.aircheck

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import seriouschoi.aircheck.feature.weather.navigation.WeatherRoute
import seriouschoi.aircheck.feature.weather.navigation.weatherScreen
import seriouschoi.aircheck.core.ui.theme.AirCheckTheme
import seriouschoi.aircheck.core.resource.R as CoreR

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AirCheckTheme {
                val navController = rememberNavController()
                val versionState by viewModel.versionState.collectAsState()
                
                // 강제 업데이트 다이얼로그
                if (versionState is VersionState.UpdateRequired) {
                    val updateUrl = (versionState as VersionState.UpdateRequired).updateUrl
                    AlertDialog(
                        onDismissRequest = { /* 닫기 불가 */ },
                        title = { Text(stringResource(CoreR.string.update_required_title)) },
                        text = { Text(stringResource(CoreR.string.update_required_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    updateUrl ?: "https://play.google.com/store/apps/details?id=$packageName"
                                ))
                                startActivity(intent)
                            }) {
                                Text(stringResource(CoreR.string.update_button))
                            }
                        }
                    )
                }

                // 앱 시작 시 권한 요청
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* WeatherScreen에서 onResume 시 권한 체크함 */ }

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
                    weatherScreen(navController = navController)
                }
            }
        }
    }
}
