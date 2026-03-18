package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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
    val airQuality: WeatherContract.DataResult<seriouschoi.aircheck.core.domain.model.AirQuality> = WeatherContract.DataResult(),
    val weather: WeatherContract.DataResult<seriouschoi.aircheck.core.domain.model.WeatherData> = WeatherContract.DataResult()
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

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    private val _intent = Channel<WeatherContract.Intent>(Channel.UNLIMITED)

    // ── State ───────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(WeatherContract.UiState())
    val uiState: StateFlow<WeatherContract.UiState> = _uiState.asStateFlow()

    private val _multiState = MutableStateFlow(MultiLocationState())
    val multiState: StateFlow<MultiLocationState> = _multiState.asStateFlow()

    // ── Effect (일회성 이벤트) ───────────────────────────────────────────────

    private val _effect = Channel<WeatherContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Intent Flow 구독
        _intent.receiveAsFlow()
            .onEach { intent -> handleIntent(intent) }
            .launchIn(viewModelScope)

        // 초기 로드
        processIntent(WeatherContract.Intent.Load)
    }

    // ── Intent 발송 ─────────────────────────────────────────────────────────

    fun processIntent(intent: WeatherContract.Intent) {
        viewModelScope.launch {
            _intent.send(intent)
        }
    }

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    private fun handleIntent(intent: WeatherContract.Intent) {
        when (intent) {
            is WeatherContract.Intent.Load -> load(isRefresh = false)
            is WeatherContract.Intent.Refresh -> load(isRefresh = true)
            is WeatherContract.Intent.ShowInfoDialog -> _uiState.value = _uiState.value.copy(showInfoDialog = true)
            is WeatherContract.Intent.DismissInfoDialog -> _uiState.value = _uiState.value.copy(showInfoDialog = false)
        }
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            // 리프레시 중이면 현재 데이터 유지하면서 인디케이터만 표시
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            _multiState.value = _multiState.value.copy(isLoading = true)

            val locationDataList = mutableListOf<LocationWeatherData>()

            // 1. 현재 위치
            val currentLocation = locationProvider.getCurrentLocation()
            if (currentLocation != null) {
                val (airResult, weatherResult) = fetchWeather(currentLocation.latitude, currentLocation.longitude)
                locationDataList.add(
                    LocationWeatherData(
                        locationName = "현재 위치",
                        isCurrentLocation = true,
                        airQuality = airResult,
                        weather = weatherResult
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    airQuality = airResult,
                    weather = weatherResult,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = WeatherContract.ErrorState(
                        message = UiText.StringResource(R.string.error_location_unavailable),
                        action = WeatherContract.Intent.Refresh
                    )
                )
            }

            // 2. 저장된 위치들
            val savedLocations = savedLocationRepository.getAll().first()
            savedLocations.forEach { saved ->
                val (airResult, weatherResult) = fetchWeather(saved.lat, saved.lng)
                locationDataList.add(
                    LocationWeatherData(
                        locationName = saved.displayName,
                        isCurrentLocation = false,
                        airQuality = airResult,
                        weather = weatherResult
                    )
                )
            }

            _multiState.value = MultiLocationState(
                locations = locationDataList,
                isLoading = false
            )
        }
    }

    private data class FetchResult(
        val airQuality: WeatherContract.DataResult<seriouschoi.aircheck.core.domain.model.AirQuality>,
        val weather: WeatherContract.DataResult<seriouschoi.aircheck.core.domain.model.WeatherData>
    )

    private suspend fun fetchWeather(lat: Double, lng: Double): FetchResult {
        val airResult = airQualityRepository.fetch(lat, lng)
        val weatherResult = weatherRepository.fetch(lat, lng)

        return FetchResult(
            airQuality = WeatherContract.DataResult(
                data = airResult.getOrNull(),
                error = airResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            ),
            weather = WeatherContract.DataResult(
                data = weatherResult.getOrNull(),
                error = weatherResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            )
        )
    }

    // ── Effect 발송 ─────────────────────────────────────────────────────────

    private fun sendEffect(effect: WeatherContract.Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
