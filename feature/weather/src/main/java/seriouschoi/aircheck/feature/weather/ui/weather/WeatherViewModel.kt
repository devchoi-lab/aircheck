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
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherOutput
import seriouschoi.aircheck.core.domain.usecase.GetLocationListUseCase
import seriouschoi.aircheck.core.domain.util.shortenBjdName
import seriouschoi.aircheck.core.resource.ErrorMapper
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

    // ── 위치 + 날씨 상태 (분리 관리) ─────────────────────────────────────────

    private val _locationsState = MutableStateFlow(UiLocationsState())

    // ── 위치 목록 관찰 ──────────────────────────────────────────────────────

    init {
        getLocationListUseCase().onEach { newLocations ->
            val oldLocations = _locationsState.value.locations
            val newLocationInfos = newLocations.map { it.toUiLocationInfo() }

            // 좌표가 변경된 위치의 날씨 무효화
            val keysToInvalidate = newLocations.mapNotNull { newLoc ->
                val key = newLoc.weatherKey()
                val old = oldLocations.find { it.weatherKey() == key }
                if (old != null && isLocationChanged(old, newLoc)) key else null
            }.toSet()

            // 유효한 위치의 날씨만 유지 (삭제된 위치, 좌표 변경된 위치 제거)
            val validKeys = newLocationInfos.map { it.weatherKey() }.toSet()
            val updatedWeathers = _locationsState.value.weathers
                .filterKeys { it in validKeys && it !in keysToInvalidate }

            _locationsState.value = UiLocationsState(
                locations = newLocationInfos,
                weathers = updatedWeathers,
                isLoading = false,
            )

            // 첫 번째 위치 로드 (필요시)
            loadLocationIfNeeded(0)
        }.launchIn(viewModelScope)
    }

    private fun isLocationChanged(old: UiLocationInfo, newLoc: LocationItem): Boolean {
        val latDiff = kotlin.math.abs(old.lat - newLoc.lat)
        val lngDiff = kotlin.math.abs(old.lng - newLoc.lng)
        return latDiff > 0.001 || lngDiff > 0.001
    }

    // ── 통합 상태 (Screen에서 관찰) ──────────────────────────────────────────

    val screenState: StateFlow<UiWeatherScreenState> = combine(
        _locationsState,
        _uiControlState
    ) { locations, uiControl ->
        UiWeatherScreenState(locations, uiControl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiWeatherScreenState()
    )

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: WeatherContract.Intent) {
        when (intent) {
            is WeatherContract.Intent.Load,
            is WeatherContract.Intent.Refresh,
                -> refresh()

            is WeatherContract.Intent.RefreshLocation -> refreshSingleLocation(intent.index)
            is WeatherContract.Intent.OnPageVisible -> loadLocationIfNeeded(intent.index)
            is WeatherContract.Intent.ShowInfoDialog -> updateUiControl { copy(showInfoDialog = true) }
            is WeatherContract.Intent.DismissInfoDialog -> updateUiControl { copy(showInfoDialog = false) }
        }
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private fun refresh() {
        viewModelScope.launch {
            locationProvider.forceRefresh()
            locationProvider.getCurrentSmartLocation()?.let { updateCurrentLocation(it) }
            // 현재 보이는 모든 위치 다시 로드
            _locationsState.value.locations.indices.forEach { index ->
                refreshSingleLocation(index)
            }
        }
    }

    private fun loadLocationIfNeeded(index: Int) {
        val locations = _locationsState.value.locations
        if (index !in locations.indices) return

        val location = locations[index]
        if (location.needsPermission) return

        val key = location.weatherKey()
        val weatherResult = _locationsState.value.weathers[key]

        // 로딩 중이면 스킵
        if (weatherResult?.isRefreshing == true) return

        // 데이터가 없거나 30분 이상 지났으면 로드
        val needsRefresh = weatherResult?.weather?.data == null || weatherResult.isStale()
        if (!needsRefresh) return

        viewModelScope.launch {
            updateWeatherRefreshing(key, true)
            val output = fetchWeatherUseCase(location.lat, location.lng)
            updateWeather(key, output)
        }
    }

    private fun UiWeatherResult.isStale(): Boolean {
        val lastRefresh = lastRefreshedAt ?: return true
        val staleThreshold = java.time.Duration.ofMinutes(30)
        return java.time.Duration.between(lastRefresh, Instant.now()) > staleThreshold
    }

    private fun updateCurrentLocation(smartLocation: SmartLocation) {
        val updatedInfo = UiLocationInfo(
            name = shortenBjdName(smartLocation.bjdName),
            isCurrentLocation = true,
            lat = smartLocation.lat,
            lng = smartLocation.lng,
        )
        _locationsState.update { state ->
            state.copy(
                locations = state.locations.map { loc ->
                    if (loc.isCurrentLocation) updatedInfo else loc
                }
            )
        }
    }

    private fun refreshSingleLocation(index: Int) {
        val locations = _locationsState.value.locations
        if (index !in locations.indices) return

        val location = locations[index]
        if (location.needsPermission) return

        val key = location.weatherKey()
        viewModelScope.launch {
            updateWeatherRefreshing(key, true)
            val output = fetchWeatherUseCase(location.lat, location.lng)
            updateWeather(key, output)
        }
    }

    private fun updateWeatherRefreshing(key: String, isRefreshing: Boolean) {
        _locationsState.update { state ->
            val current = state.weathers[key] ?: UiWeatherResult()
            state.copy(weathers = state.weathers + (key to current.copy(isRefreshing = isRefreshing)))
        }
    }

    private fun updateWeather(key: String, output: FetchWeatherOutput) {
        _locationsState.update { state ->
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
            state.copy(weathers = state.weathers + (key to result))
        }
    }

    private inline fun updateUiControl(transform: UiControlState.() -> UiControlState) {
        _uiControlState.value = _uiControlState.value.transform()
    }

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun LocationItem.toUiLocationInfo(): UiLocationInfo {
        return UiLocationInfo(
            name = name,
            isCurrentLocation = isCurrentLocation,
            savedLocationId = savedLocationId,
            lat = lat,
            lng = lng,
            needsPermission = needsPermission,
        )
    }

    private fun LocationItem.weatherKey(): String =
        if (isCurrentLocation) "current" else savedLocationId.toString()
}