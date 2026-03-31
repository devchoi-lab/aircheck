package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.model.LocationItem
import seriouschoi.aircheck.core.domain.model.getRecommendations
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SmartLocation
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherOutput
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.GetLocationListUseCase
import seriouschoi.aircheck.core.domain.util.shortenBjdName
import seriouschoi.aircheck.core.resource.ErrorMapper
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val getLocationListUseCase: GetLocationListUseCase,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    // ── UI 컨트롤 상태 ──────────────────────────────────────────────────────

    private val _uiControlState = MutableStateFlow(UiControlState())

    // ── 위치 상태 ──────────────────────────────────────────────────────────
    private val _locations: MutableStateFlow<List<UiLocationInfo>> = MutableStateFlow(emptyList())
    private val _isLocationsLoading = MutableStateFlow(true)

    // ── 날씨 상태 ──────────────────────────────────────────────────────────

    private val _weathers = MutableStateFlow<Map<String, UiWeatherResult>>(emptyMap())

    // ── 통합 상태 (Screen에서 관찰) ──────────────────────────────────────────

    val screenState: StateFlow<UiWeatherScreenState> = combine(
        _locations,
        _weathers,
        _isLocationsLoading,
        _uiControlState,
    ) { locations, weathers, isLoading, uiControl ->
        UiWeatherScreenState(locations, weathers, isLoading, uiControl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiWeatherScreenState()
    )

    // ── 위치 목록 관찰 ──────────────────────────────────────────────────────

    init {
        getLocationListUseCase().onEach { newLocations ->
            val newLocationInfos = newLocations.map { it.toUiLocationInfo() }

            // 유효한 위치의 날씨만 유지 (삭제된 위치 제거)
            val validKeys = newLocationInfos.map { it.bjdCode }.toSet()
            _weathers.update { weathers ->
                weathers.filterKeys { it in validKeys }
            }

            _locations.value = newLocationInfos
            _isLocationsLoading.value = false

            // 첫 번째 위치 로드 (필요시)
            loadLocationIfNeeded(0)
        }.launchIn(viewModelScope)
    }

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: WeatherContract.Intent) {
        when (intent) {
            is WeatherContract.Intent.RefreshLocation -> refreshSingleLocation(intent.index)
            is WeatherContract.Intent.OnPageVisible -> loadLocationIfNeeded(intent.index)
            is WeatherContract.Intent.ShowInfoDialog -> updateUiControl { copy(showInfoDialog = true) }
            is WeatherContract.Intent.DismissInfoDialog -> updateUiControl { copy(showInfoDialog = false) }
            is WeatherContract.Intent.OnPermissionGranted -> onPermissionGranted()
        }
    }
    
    private fun onPermissionGranted() {
        viewModelScope.launch {
            // 권한 승인 시 위치 새로고침 트리거
            locationProvider.forceRefresh()
            // 현재 위치 정보 갱신
            locationProvider.getCurrentSmartLocation()?.let { smartLocation ->
                val updatedInfo = UiLocationInfo(
                    name = shortenBjdName(smartLocation.bjdName),
                    isCurrentLocation = true,
                    bjdCode = smartLocation.bjdCode,
                )
                _locations.update { locations ->
                    locations.map { loc ->
                        if (loc.isCurrentLocation) updatedInfo else loc
                    }
                }
                // 날씨 데이터 로드
                updateWeatherRefreshing(smartLocation.bjdCode, true)
                val output = fetchWeatherUseCase(smartLocation.bjdCode)
                updateWeather(smartLocation.bjdCode, output)
            }
        }
    }

    private fun loadLocationIfNeeded(index: Int) {
        val locations = _locations.value
        if (index !in locations.indices) return

        val location = locations[index]
        if (location.needsPermission || location.bjdCode.isEmpty()) return

        val bjdCode = location.bjdCode
        val weatherResult = _weathers.value[bjdCode]

        // 로딩 중이면 스킵
        if (weatherResult?.isRefreshing == true) return

        // 데이터가 없거나 30분 이상 지났으면 로드
        val needsRefresh = weatherResult?.weather?.data == null || weatherResult.isStale()
        if (!needsRefresh) return

        viewModelScope.launch {
            updateWeatherRefreshing(bjdCode, true)
            val output = fetchWeatherUseCase(bjdCode)
            updateWeather(bjdCode, output)
        }
    }

    private fun UiWeatherResult.isStale(): Boolean {
        val lastRefresh = lastRefreshedAt ?: return true
        val staleThreshold = Duration.ofMinutes(30)
        return Duration.between(lastRefresh, Instant.now()) > staleThreshold
    }

    private fun updateCurrentLocation(smartLocation: SmartLocation) {
        val updatedInfo = UiLocationInfo(
            name = shortenBjdName(smartLocation.bjdName),
            isCurrentLocation = true,
            bjdCode = smartLocation.bjdCode,
        )
        _locations.update { locations ->
            locations.map { loc ->
                if (loc.isCurrentLocation) updatedInfo else loc
            }
        }
    }

    private fun refreshSingleLocation(index: Int) {
        val locations = _locations.value
        if (index !in locations.indices) return

        val location = locations[index]
        if (location.needsPermission || location.bjdCode.isEmpty()) return

        val bjdCode = location.bjdCode
        viewModelScope.launch {
            // 현재 위치 새로고침 시 GPS 갱신 + 위치 정보 업데이트
            if (location.isCurrentLocation) {
                locationProvider.forceRefresh()
                locationProvider.getCurrentSmartLocation()?.let { updateCurrentLocation(it) }
            }
            updateWeatherRefreshing(bjdCode, true)
            val output = fetchWeatherUseCase(bjdCode)
            updateWeather(bjdCode, output)
        }
    }

    private fun updateWeatherRefreshing(key: String, isRefreshing: Boolean) {
        _weathers.update { weathers ->
            val current = weathers[key] ?: UiWeatherResult()
            weathers + (key to current.copy(isRefreshing = isRefreshing))
        }
    }

    private fun updateWeather(key: String, output: FetchWeatherOutput) {
        val weatherData = output.result.weather.getOrNull()
        val airQualityData = output.result.airQuality.getOrNull()
        val result = UiWeatherResult(
            weather = WeatherContract.DataResult(
                data = weatherData,
                error = ErrorMapper.toUiText(output.result.weather.exceptionOrNull())
            ),
            airQuality = WeatherContract.DataResult(
                data = airQualityData,
                error = ErrorMapper.toUiText(output.result.airQuality.exceptionOrNull())
            ),
            recommendations = getRecommendations(weatherData, airQualityData),
            isRefreshing = false,
            lastRefreshedAt = Instant.now(),
        )
        _weathers.update { weathers -> weathers + (key to result) }
    }

    private inline fun updateUiControl(transform: UiControlState.() -> UiControlState) {
        _uiControlState.value = _uiControlState.value.transform()
    }

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun LocationItem.toUiLocationInfo(): UiLocationInfo {
        return UiLocationInfo(
            name = name,
            isCurrentLocation = isCurrentLocation,
            bjdCode = bjdCode,
            needsPermission = needsPermission,
        )
    }
}