package seriouschoi.aircheck.feature.weather.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import javax.inject.Inject

sealed interface MainUiState {
    data object Idle : MainUiState
    data object Loading : MainUiState
    data class Success(
        val airQuality: DataResult<AirQuality>,
        val weather: DataResult<WeatherData>
    ) : MainUiState
    data class Error(val message: String) : MainUiState
}

data class DataResult<T>(
    val data: T? = null,
    val error: String? = null
)

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val airQualityRepository: AirQualityRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            
            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.value = MainUiState.Error("위치를 확인할 수 없어요.\n위치 권한을 확인해주세요.")
                return@launch
            }
            
            val lat = location.latitude
            val lng = location.longitude
            
            val airResult = airQualityRepository.fetch(lat, lng)
            val weatherResult = weatherRepository.fetch(lat, lng)
            
            _uiState.value = MainUiState.Success(
                airQuality = DataResult(
                    data = airResult.getOrNull(),
                    error = airResult.exceptionOrNull()?.message
                ),
                weather = DataResult(
                    data = weatherResult.getOrNull(),
                    error = weatherResult.exceptionOrNull()?.message
                )
            )
        }
    }
}
