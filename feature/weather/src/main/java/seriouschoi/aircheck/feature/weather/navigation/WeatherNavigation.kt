package seriouschoi.aircheck.feature.weather.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import seriouschoi.aircheck.feature.weather.ui.LocationPickerScreen
import seriouschoi.aircheck.feature.weather.ui.LocationPickerViewModel
import seriouschoi.aircheck.feature.weather.ui.WeatherScreen
import seriouschoi.aircheck.feature.weather.ui.WeatherViewModel

/** 날씨 피처 네비게이션 그래프 */
fun NavGraphBuilder.weatherScreen(navController: NavController) {
    composable<WeatherRoute.Main> {
        val viewModel: WeatherViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        WeatherScreen(
            uiState = uiState,
            onRefresh = { viewModel.load() },
            onAddLocation = { navController.navigate(WeatherRoute.LocationPicker) }
        )
    }
    
    composable<WeatherRoute.LocationPicker> {
        val viewModel: LocationPickerViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        
        LaunchedEffect(state.locationSaved) {
            if (state.locationSaved) {
                navController.popBackStack()
            }
        }
        
        LocationPickerScreen(
            regions = state.regions,
            currentPath = state.currentPath,
            searchQuery = state.searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onRegionClick = viewModel::onRegionClick,
            onBackClick = viewModel::onBackClick,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
