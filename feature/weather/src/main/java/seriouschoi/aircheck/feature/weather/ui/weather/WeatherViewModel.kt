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
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SmartLocation
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherOutput
import seriouschoi.aircheck.core.domain.usecase.GetLocationListUseCase
import seriouschoi.aircheck.core.domain.util.shortenBjdName
import seriouschoi.aircheck.core.resource.ErrorMapper
import seriouschoi.aircheck.core.resource.UiText
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

    // ── 위치 + 날씨 상태 ─────────────────────────────────────────────────────

    private val _locationsState = MutableStateFlow(UiLocationsState())

    // ── 위치 목록 관찰 → 날씨 개별 로드 ──────────────────────────────────────

    init {
        getLocationListUseCase().onEach { newLocations ->
            val existingLocations = _locationsState.value.locations
            
            // 기존 날씨 데이터 유지하면서 위치 목록 업데이트
            val mergedLocations = newLocations.map { newLoc ->
                val existing = findExistingLocation(existingLocations, newLoc)
                if (existing != null && !isLocationChanged(existing, newLoc)) {
                    // 위치가 같으면 기존 날씨 데이터 유지
                    existing.copy(
                        locationInfo = newLoc.toUiLocationInfo()
                    )
                } else {
                    // 새 위치거나 좌표가 바뀌면 새로 생성
                    newLoc.toUiLocationWeatherData()
                }
            }
            
            _locationsState.value = UiLocationsState(
                locations = mergedLocations,
                isLoading = false,
            )
            
            // 첫 번째 위치 로드 (필요시)
            loadLocationIfNeeded(0)
        }.launchIn(viewModelScope)
    }
    
    private fun findExistingLocation(
        existingLocations: List<UiLocationWeatherData>,
        newLoc: LocationItem
    ): UiLocationWeatherData? {
        return if (newLoc.isCurrentLocation) {
            existingLocations.find { it.locationInfo.isCurrentLocation }
        } else {
            existingLocations.find { it.locationInfo.savedLocationId == newLoc.savedLocationId }
        }
    }
    
    private fun isLocationChanged(existing: UiLocationWeatherData, newLoc: LocationItem): Boolean {
        // 좌표가 유의미하게 바뀌었는지 확인 (약 100m 이상)
        val latDiff = kotlin.math.abs(existing.locationInfo.lat - newLoc.lat)
        val lngDiff = kotlin.math.abs(existing.locationInfo.lng - newLoc.lng)
        return latDiff > 0.001 || lngDiff > 0.001
    }
    
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

        // 로딩 중이면 스킵
        if (location.isRefreshing) return
        if (location.locationInfo.needsPermission) return

        // 데이터가 없거나 30분 이상 지났으면 로드
        val needsRefresh = location.weather.data == null || location.isStale()

        if (!needsRefresh) return

        viewModelScope.launch {
            updateLocationRefreshing(index, true)
            val output = fetchWeatherUseCase(location.locationInfo.lat, location.locationInfo.lng)
            updateLocationWeather(index, output)
        }
    }

    private fun UiLocationWeatherData.isStale(): Boolean {
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
                    if (loc.locationInfo.isCurrentLocation) loc.copy(locationInfo = updatedInfo) else loc
                }
            )
        }
    }

    private fun refreshSingleLocation(index: Int) {
        val currentLocations = _locationsState.value.locations
        if (index !in currentLocations.indices) return

        val location = currentLocations[index]
        if (location.locationInfo.needsPermission) return

        viewModelScope.launch {
            updateLocationRefreshing(index, true)
            val output = fetchWeatherUseCase(location.locationInfo.lat, location.locationInfo.lng)
            updateLocationWeather(index, output)
        }
    }

    private fun updateLocationRefreshing(index: Int, isRefreshing: Boolean) {
        _locationsState.update { state ->
            state.copy(
                locations = state.locations.mapIndexed { i, loc ->
                    if (i == index) loc.copy(isRefreshing = isRefreshing) else loc
                }
            )
        }
    }

    private fun updateLocationWeather(index: Int, output: FetchWeatherOutput) {
        _locationsState.update { state ->
            state.copy(
                locations = state.locations.mapIndexed { i, loc ->
                    if (i == index) {
                        loc.copy(
                            weather = WeatherContract.DataResult(
                                data = output.result.weather.getOrNull(),
                                error = ErrorMapper.toUiText(output.result.weather.exceptionOrNull())
                            ),
                            airQuality = WeatherContract.DataResult(
                                data = output.result.airQuality.getOrNull(),
                                error = ErrorMapper.toUiText(output.result.airQuality.exceptionOrNull())
                            ),
                            isRefreshing = false,
                            lastRefreshedAt = Instant.now(),
                        )
                    } else {
                        loc
                    }
                }
            )
        }
    }

    private inline fun updateUiControl(transform: UiControlState.() -> UiControlState) {
        _uiControlState.value = _uiControlState.value.transform()
    }

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun LocationItem.toUiLocationWeatherData(): UiLocationWeatherData {
        return UiLocationWeatherData(
            locationInfo = UiLocationInfo(
                name = name,
                isCurrentLocation = isCurrentLocation,
                savedLocationId = savedLocationId,
                lat = lat,
                lng = lng,
                needsPermission = needsPermission,
            ),
        )
    }
}
