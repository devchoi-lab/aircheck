@file:OptIn(ExperimentalCoroutinesApi::class)

package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
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

private typealias WeathersReducer = (Map<String, UiWeatherResult>) -> Map<String, UiWeatherResult>

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

    // ── 날씨 새로고침 요청 (사용자 액션 전용) ─────────────────────────────────

    private sealed interface WeatherRefreshRequest {
        val index: Int

        data class LoadIfNeeded(override val index: Int) : WeatherRefreshRequest
        data class ForceRefresh(override val index: Int) : WeatherRefreshRequest
    }

    private val refreshRequests = MutableSharedFlow<WeatherRefreshRequest>(extraBufferCapacity = 64)

    // ── 날씨 상태 (refresh 파이프라인) ────────────────────────────────────
    //
    // 날씨가 언제·왜 갱신되는지 한 흐름으로 표현:
    //  Branch 1 (_locations) : 위치 목록 변경 → 삭제된 키 정리 + 첫 위치 자동 로드
    //  Branch 2 (refreshRequests) : 사용자 액션 (OnPageVisible, RefreshLocation)
    // 두 브랜치 모두 WeathersReducer를 방출하고, scan이 현재 map에 누적 적용한다.

    private val _weathers: StateFlow<Map<String, UiWeatherResult>> = merge(
        // Branch 1: 위치 목록이 바뀌면 삭제된 키를 정리하고, 첫 번째 위치를 자동 로드한다.
        // flatMapLatest를 사용해 위치 목록이 다시 바뀌면 진행 중인 로드를 취소하고 재시작한다.
        _locations.flatMapLatest { locations ->
            val validKeys = locations.map { it.bjdCode }.toSet()
            val cleanup: WeathersReducer = { weathers ->
                weathers.filterKeys { it in validKeys }
            }
            flow {
                emit(cleanup)
                emitAll(handleRefreshRequest(WeatherRefreshRequest.LoadIfNeeded(0)))
            }
        },
        // Branch 2: 사용자가 직접 트리거하는 새로고침 (OnPageVisible, pull-to-refresh)
        refreshRequests.flatMapMerge { request -> handleRefreshRequest(request) }
    ).scan(emptyMap()) { acc: Map<String, UiWeatherResult>, reducer: WeathersReducer ->
        reducer(acc)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )


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
            _locations.value = newLocations.map { it.toUiLocationInfo() }
            _isLocationsLoading.value = false
        }.launchIn(viewModelScope)
    }

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: WeatherContract.Intent) {
        when (intent) {
            is WeatherContract.Intent.RefreshLocation -> viewModelScope.launch {
                refreshRequests.emit(WeatherRefreshRequest.ForceRefresh(intent.index))
            }

            is WeatherContract.Intent.OnPageVisible -> viewModelScope.launch {
                refreshRequests.emit(WeatherRefreshRequest.LoadIfNeeded(intent.index))
            }

            is WeatherContract.Intent.ShowInfoDialog -> updateUiControl { copy(showInfoDialog = true) }
            is WeatherContract.Intent.DismissInfoDialog -> updateUiControl { copy(showInfoDialog = false) }
        }
    }

    // ── 날씨 fetch 핸들러 ────────────────────────────────────────────────────

    private fun handleRefreshRequest(request: WeatherRefreshRequest): Flow<WeathersReducer> = flow {
        // request가 location범위가 아니면, 발행하지 않음.
        val locations = _locations.value
        if (request.index !in locations.indices) return@flow

        // 위치가 권한이 필요하거나, 법정동 코드를 알 수 없으면, 발행하지 않음.
        val location = locations[request.index]
        if (location.needsPermission || location.bjdCode.isEmpty()) return@flow

        var bjdCode = location.bjdCode

        when (request) {
            is WeatherRefreshRequest.LoadIfNeeded -> {
                //새로고침이 필요한지 여부 체크.(IfNeeded)
                val current = _weathers.value[bjdCode]
                if (current?.isRefreshing == true) return@flow
                if (current?.weather?.data != null && !current.isStale()) return@flow
            }

            is WeatherRefreshRequest.ForceRefresh -> {
                if (location.isCurrentLocation) {
                    //현재 위치를 새로고침 한 경우, 현재 위치 갱신.
                    locationProvider.forceRefresh()
                    locationProvider.getCurrentSmartLocation()?.let { smartLocation ->
                        updateCurrentLocation(smartLocation)
                        bjdCode = smartLocation.bjdCode
                    }
                }
            }
        }

        emit { weathers: Map<String, UiWeatherResult> ->
            val current = weathers[bjdCode] ?: UiWeatherResult()
            weathers + (bjdCode to current.copy(isRefreshing = true))
        }

        val output = fetchWeatherUseCase(bjdCode)

        emit { weathers ->
            weathers + (bjdCode to buildWeatherResult(output))
        }
    }

    private fun UiWeatherResult.isStale(): Boolean {
        val lastRefresh = lastRefreshedAt ?: return true
        return Duration.between(lastRefresh, Instant.now()) > Duration.ofMinutes(30)
    }

    private fun updateCurrentLocation(smartLocation: SmartLocation) {
        val updatedInfo = UiLocationInfo(
            name = shortenBjdName(smartLocation.bjdName),
            isCurrentLocation = true,
            bjdCode = smartLocation.bjdCode,
        )
        _locations.update { locations ->
            locations.map { loc -> if (loc.isCurrentLocation) updatedInfo else loc }
        }
    }

    private fun buildWeatherResult(output: FetchWeatherOutput): UiWeatherResult {
        val weatherData = output.result.weather.getOrNull()
        val airQualityData = output.result.airQuality.getOrNull()
        return UiWeatherResult(
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
