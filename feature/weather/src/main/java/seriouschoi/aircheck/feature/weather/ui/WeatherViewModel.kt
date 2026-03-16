package seriouschoi.aircheck.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.core.resource.R
import seriouschoi.aircheck.core.resource.UiText
import javax.inject.Inject

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val airQualityRepository: AirQualityRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            
            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.value = WeatherUiState.Error(
                    UiText.StringResource(R.string.error_location_unavailable)
                )
                return@launch
            }
            
            val lat = location.latitude
            val lng = location.longitude
            
            val airResult = airQualityRepository.fetch(lat, lng)
            val weatherResult = weatherRepository.fetch(lat, lng)
            
            _uiState.value = WeatherUiState.Success(
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
}
