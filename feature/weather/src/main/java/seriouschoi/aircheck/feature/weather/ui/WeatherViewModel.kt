package seriouschoi.aircheck.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.model.SavedLocation
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.core.resource.R
import seriouschoi.aircheck.core.resource.UiText
import javax.inject.Inject

/** 위치별 날씨 데이터 */
internal data class LocationWeatherData(
    val locationName: String,
    val isCurrentLocation: Boolean,
    val state: WeatherUiState
)

/** 전체 UI 상태 */
internal data class MultiLocationState(
    val locations: List<LocationWeatherData> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val airQualityRepository: AirQualityRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val savedLocationRepository: SavedLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    
    private val _multiState = MutableStateFlow(MultiLocationState())
    val multiState: StateFlow<MultiLocationState> = _multiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _multiState.value = MultiLocationState(isLoading = true)
            
            val locationDataList = mutableListOf<LocationWeatherData>()
            
            // 1. 현재 위치
            val currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation != null) {
                val state = fetchWeather(currentLocation.latitude, currentLocation.longitude)
                locationDataList.add(
                    LocationWeatherData(
                        locationName = "현재 위치",
                        isCurrentLocation = true,
                        state = state
                    )
                )
                // 첫 번째 위치는 기존 uiState에도 반영
                _uiState.value = state
            } else {
                _uiState.value = WeatherUiState.Error(
                    UiText.StringResource(R.string.error_location_unavailable)
                )
            }
            
            // 2. 저장된 위치들
            val savedLocations = savedLocationRepository.getAll().first()
            savedLocations.forEach { saved ->
                val state = fetchWeather(saved.lat, saved.lng)
                locationDataList.add(
                    LocationWeatherData(
                        locationName = saved.displayName,
                        isCurrentLocation = false,
                        state = state
                    )
                )
            }
            
            _multiState.value = MultiLocationState(
                locations = locationDataList,
                isLoading = false
            )
        }
    }
    
    private suspend fun fetchWeather(lat: Double, lng: Double): WeatherUiState {
        val airResult = airQualityRepository.fetch(lat, lng)
        val weatherResult = weatherRepository.fetch(lat, lng)
        
        return WeatherUiState.Success(
            airQuality = WeatherDataResult(
                data = airResult.getOrNull(),
                error = airResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            ),
            weather = WeatherDataResult(
                data = weatherResult.getOrNull(),
                error = weatherResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            )
        )
    }
}
