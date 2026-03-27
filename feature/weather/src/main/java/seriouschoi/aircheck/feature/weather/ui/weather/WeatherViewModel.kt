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
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.GetLocationListUseCase
import seriouschoi.aircheck.core.resource.UiText
import javax.inject.Inject

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val getLocationListUseCase: GetLocationListUseCase,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val locationProvider: seriouschoi.aircheck.core.domain.repository.LocationProvider,
) : ViewModel() {

    // ── UI 컨트롤 상태 ──────────────────────────────────────────────────────

    private val _uiControlState = MutableStateFlow(UiControlState())

    // ── 위치 + 날씨 상태 ─────────────────────────────────────────────────────

    private val _locationsState = MutableStateFlow(UiLocationsState())

    // ── 위치 목록 관찰 → 날씨 개별 로드 ──────────────────────────────────────

    init {
        getLocationListUseCase().onEach { locations ->
            // 1. 위치 페이지 즉시 표시 (날씨는 빈 상태)
            _locationsState.value = UiLocationsState(
                locations = locations.map { it.toUiLocationWeatherData() },
                isLoading = false,
            )
            // 2. 각 위치별 날씨를 병렬로 fetch
            fetchWeatherForAll()
        }.launchIn(viewModelScope)
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
            is WeatherContract.Intent.ShowInfoDialog -> updateUiControl { copy(showInfoDialog = true) }
            is WeatherContract.Intent.DismissInfoDialog -> updateUiControl { copy(showInfoDialog = false) }
        }
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private fun refresh() {
        viewModelScope.launch {
            // 캐시 초기화 후 현재 위치를 즉시 재조회
            locationProvider.forceRefresh()
            val smartLocation = locationProvider.getCurrentSmartLocation()

            // 현재 위치 정보 갱신
            if (smartLocation != null) {
                _locationsState.update { state ->
                    state.copy(
                        locations = state.locations.map { loc ->
                            if (loc.locationInfo.isCurrentLocation) {
                                loc.copy(
                                    locationInfo = loc.locationInfo.copy(
                                        name = shortenBjdName(smartLocation.bjdName),
                                        lat = smartLocation.lat,
                                        lng = smartLocation.lng,
                                        needsPermission = false,
                                    )
                                )
                            } else loc
                        }
                    )
                }
            }

            // 전체 날씨 다시 fetch
            fetchWeatherForAll()
        }
    }

    private fun shortenBjdName(fullName: String): String {
        val parts = fullName.split(" ")
        return when {
            parts.size <= 2 -> fullName
            else -> "${parts[1]} ${parts.last()}"
        }
    }

    private fun fetchWeatherForAll() {
        val currentLocations = _locationsState.value.locations
        currentLocations.forEachIndexed { index, location ->
            if (location.locationInfo.needsPermission) return@forEachIndexed
            viewModelScope.launch {
                updateLocationRefreshing(index, true)
                val output = fetchWeatherUseCase(location.locationInfo.lat, location.locationInfo.lng)
                updateLocationWeather(index, output)
            }
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

    private fun updateLocationWeather(index: Int, output: seriouschoi.aircheck.core.domain.usecase.FetchWeatherOutput) {
        _locationsState.update { state ->
            state.copy(
                locations = state.locations.mapIndexed { i, loc ->
                    if (i == index) {
                        loc.copy(
                            weather = WeatherContract.DataResult(
                                data = output.result.weather.getOrNull(),
                                error = output.result.weather.exceptionOrNull()?.message
                                    ?.let { UiText.DynamicString(it) }
                            ),
                            airQuality = WeatherContract.DataResult(
                                data = output.result.airQuality.getOrNull(),
                                error = output.result.airQuality.exceptionOrNull()?.message
                                    ?.let { UiText.DynamicString(it) }
                            ),
                            isRefreshing = false,
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
