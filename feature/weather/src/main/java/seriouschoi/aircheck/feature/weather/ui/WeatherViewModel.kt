package seriouschoi.aircheck.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.core.resource.R
import seriouschoi.aircheck.core.resource.UiText
import seriouschoi.aircheck.feature.weather.ui.WeatherContract.*
import javax.inject.Inject

/** 위치별 날씨 데이터 */
internal data class LocationWeatherData(
    val locationName: String,
    val isCurrentLocation: Boolean,
    val state: State
)

/** 전체 UI 상태 (다중 위치) */
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

    // ── State ───────────────────────────────────────────────────────────────
    
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _multiState = MutableStateFlow(MultiLocationState())
    val multiState: StateFlow<MultiLocationState> = _multiState.asStateFlow()
    
    // ── Effect (일회성 이벤트) ───────────────────────────────────────────────
    
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        processIntent(Intent.Load)
    }
    
    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: Intent) {
        when (intent) {
            is Intent.Load -> load(isRefresh = false)
            is Intent.Refresh -> load(isRefresh = true)
        }
    }
    
    // ── Private ─────────────────────────────────────────────────────────────

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            // 리프레시 중이면 현재 상태 유지하면서 인디케이터만 표시
            if (isRefresh && _state.value is State.Success) {
                _state.value = (_state.value as State.Success).copy(isRefreshing = true)
            } else {
                _state.value = State.Loading
            }
            
            _multiState.value = _multiState.value.copy(isLoading = true)
            
            val locationDataList = mutableListOf<LocationWeatherData>()
            
            // 1. 현재 위치
            val currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation != null) {
                val locationState = fetchWeather(currentLocation.latitude, currentLocation.longitude)
                locationDataList.add(
                    LocationWeatherData(
                        locationName = "현재 위치",
                        isCurrentLocation = true,
                        state = locationState
                    )
                )
                _state.value = locationState
            } else {
                _state.value = State.Error(
                    UiText.StringResource(R.string.error_location_unavailable)
                )
            }
            
            // 2. 저장된 위치들
            val savedLocations = savedLocationRepository.getAll().first()
            savedLocations.forEach { saved ->
                val locationState = fetchWeather(saved.lat, saved.lng)
                locationDataList.add(
                    LocationWeatherData(
                        locationName = saved.displayName,
                        isCurrentLocation = false,
                        state = locationState
                    )
                )
            }
            
            _multiState.value = MultiLocationState(
                locations = locationDataList,
                isLoading = false
            )
        }
    }
    
    private suspend fun fetchWeather(lat: Double, lng: Double): State {
        val airResult = airQualityRepository.fetch(lat, lng)
        val weatherResult = weatherRepository.fetch(lat, lng)
        
        return State.Success(
            airQuality = DataResult(
                data = airResult.getOrNull(),
                error = airResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            ),
            weather = DataResult(
                data = weatherResult.getOrNull(),
                error = weatherResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            ),
            isRefreshing = false
        )
    }
    
    // ── Effect 발송 ─────────────────────────────────────────────────────────
    
    private fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
