package seriouschoi.aircheck.feature.weather.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import seriouschoi.aircheck.feature.weather.ui.WeatherScreen
import seriouschoi.aircheck.feature.weather.ui.WeatherViewModel

/** 날씨 피처 네비게이션 그래프 */
fun NavGraphBuilder.weatherScreen() {
    composable<WeatherRoute.Main> {
        val viewModel: WeatherViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        WeatherScreen(
            uiState = uiState,
            onRefresh = { viewModel.load() }
        )
    }
}
