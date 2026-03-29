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
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import seriouschoi.aircheck.feature.weather.navigation.WeatherRoute
import seriouschoi.aircheck.feature.weather.navigation.weatherScreen
import seriouschoi.aircheck.core.ui.theme.AirCheckTheme
import seriouschoi.aircheck.core.resource.R as CoreR

/** 권한 승인 콜백을 전달하기 위한 CompositionLocal */
val LocalPermissionGranted = staticCompositionLocalOf<() -> Unit> { {} }

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
