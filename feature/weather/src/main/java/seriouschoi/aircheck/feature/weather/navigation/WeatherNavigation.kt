package seriouschoi.aircheck.feature.weather.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerScreen
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerViewModel
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherScreen
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherViewModel

/** 날씨 피처 네비게이션 그래프 */
fun NavGraphBuilder.weatherScreen(
    navController: NavController,
    onRegisterPermissionCallback: ((callback: () -> Unit) -> Unit)? = null
) {
    composable<WeatherRoute.Main> {
        val viewModel: WeatherViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val multiState by viewModel.multiState.collectAsStateWithLifecycle()
        
        // 권한 승인 시 자동 새로고침 콜백 등록
        LaunchedEffect(Unit) {
            onRegisterPermissionCallback?.invoke {
                viewModel.processIntent(seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract.Intent.Refresh)
            }
        }

        WeatherScreen(
            uiState = uiState,
            multiState = multiState,
            onIntent = viewModel::processIntent,
            onAddLocation = { navController.navigate(WeatherRoute.LocationPicker) }
        )
    }

    composable<WeatherRoute.LocationPicker> {
        val viewModel: LocationPickerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is LocationPickerContract.Effect.LocationSaved -> navController.popBackStack()
                }
            }
        }

        LocationPickerScreen(
            uiState = uiState,
            onIntent = viewModel::processIntent,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
