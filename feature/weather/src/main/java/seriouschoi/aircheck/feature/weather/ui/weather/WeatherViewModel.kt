@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.ObserveWeatherUseCase
import seriouschoi.aircheck.core.resource.UiText
import javax.inject.Inject

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val observeWeatherUseCase: ObserveWeatherUseCase,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val savedLocationRepository: SavedLocationRepository,
) : ViewModel() {

    // ── Refresh 트리거 ──────────────────────────────────────────────────────

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit) // 초기 로드
    }

    // ── UI 컨트롤 상태 ──────────────────────────────────────────────────────

    private val _uiControlState = MutableStateFlow(UiControlState())

    // ── 날씨 데이터 (로컬 캐시) ──────────────────────────────────────────────

    private val _locationsState = MutableStateFlow(UiLocationsState())

    // ── 날씨 데이터 Flow (UseCase에서 관찰) ─────────────────────────────────

    init {
        refreshTrigger.flatMapLatest {
            observeWeatherUseCase().map { state ->
                state.toUiLocationsState()
            }
        }.onEach {newState ->
            _locationsState.value = newState
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
            is WeatherContract.Intent.RequestDeleteLocation -> updateUiControl {
                copy(
                    deleteConfirm = WeatherContract.DeleteConfirmState(
                        intent.locationId,
                        intent.locationName
                    )
                )
            }

            is WeatherContract.Intent.ConfirmDeleteLocation -> confirmDelete()
            is WeatherContract.Intent.DismissDeleteDialog -> updateUiControl { copy(deleteConfirm = null) }
        }
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private fun refresh() {
        val currentLocations = _locationsState.value.locations
        if (currentLocations.isEmpty()) {
            viewModelScope.launch { refreshTrigger.emit(Unit) }
            return
        }
        currentLocations.indices.forEach { refreshSingleLocation(it) }
    }

    /**
     * 개별 위치 새로고침 (당겨서 새로고침)
     */
    private fun refreshSingleLocation(index: Int) {
        val currentLocations = _locationsState.value.locations
        if (index !in currentLocations.indices) return

        val location = currentLocations[index]

        viewModelScope.launch {
            val output = fetchWeatherUseCase(location.locationInfo.lat, location.locationInfo.lng)

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
                                )
                            )
                        } else {
                            loc
                        }
                    }
                )
            }
        }
    }

    private inline fun updateUiControl(transform: UiControlState.() -> UiControlState) {
        _uiControlState.value = _uiControlState.value.transform()
    }

    private fun confirmDelete() {
        val deleteState = _uiControlState.value.deleteConfirm ?: return
        viewModelScope.launch {
            savedLocationRepository.delete(deleteState.locationId)
            updateUiControl { copy(deleteConfirm = null) }
            // savedLocationRepository 변경 시 자동 새로고침 (Flow 연결)
        }
    }

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun seriouschoi.aircheck.core.domain.usecase.WeatherState.toUiLocationsState(): UiLocationsState {
        return UiLocationsState(
            locations = locations.map { it.toUiLocationWeatherData() },
            isLoading = isLoading
        )
    }

    private fun seriouschoi.aircheck.core.domain.usecase.LocationWeather.toUiLocationWeatherData(): UiLocationWeatherData {
        return UiLocationWeatherData(
            locationInfo = UiLocationInfo(
                name = locationName,
                isCurrentLocation = isCurrentLocation,
                savedLocationId = savedLocationId,
                lat = lat,
                lng = lng,
                needsPermission = needsPermission,
            ),
            weather = WeatherContract.DataResult(
                data = weather.getOrNull(),
                error = weather.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            ),
            airQuality = WeatherContract.DataResult(
                data = airQuality.getOrNull(),
                error = airQuality.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
            ),
        )
    }
}
