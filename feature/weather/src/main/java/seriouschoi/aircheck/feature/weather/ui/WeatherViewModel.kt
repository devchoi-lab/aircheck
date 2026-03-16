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
import javax.inject.Inject

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val airQualityRepository: AirQualityRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            
            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.value = WeatherUiState.Error("위치를 확인할 수 없어요.\n위치 권한을 확인해주세요.")
                return@launch
            }
            
            val lat = location.latitude
            val lng = location.longitude
            
            val airResult = airQualityRepository.fetch(lat, lng)
            val weatherResult = weatherRepository.fetch(lat, lng)
            
            _uiState.value = WeatherUiState.Success(
                airQuality = WeatherDataResult(
                    data = airResult.getOrNull(),
                    error = airResult.exceptionOrNull()?.message
                ),
                weather = WeatherDataResult(
                    data = weatherResult.getOrNull(),
                    error = weatherResult.exceptionOrNull()?.message
                )
            )
        }
    }
}
