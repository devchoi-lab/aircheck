package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.resource.R
import seriouschoi.aircheck.core.resource.UiText
import javax.inject.Inject

@HiltViewModel
internal class WeatherViewModel @Inject constructor(
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val locationProvider: LocationProvider,
    private val savedLocationRepository: SavedLocationRepository
) : ViewModel() {

    // ── Intent Channel ──────────────────────────────────────────────────────

    private val _intent = Channel<WeatherContract.Intent>(Channel.UNLIMITED)

    // ── 관심사별 분리된 상태 ─────────────────────────────────────────────────

    /** 위치 목록 상태 */
    private val _locationsState = MutableStateFlow(LocationsState())
    
    /** UI 컨트롤 상태 */
    private val _uiControlState = MutableStateFlow(UiControlState())

    // ── 통합 상태 (Screen에서 관찰) ──────────────────────────────────────────

    val screenState: StateFlow<WeatherScreenState> = combine(
        _locationsState,
        _uiControlState
    ) { locations, uiControl ->
        WeatherScreenState(locations, uiControl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherScreenState()
    )

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

        // 저장된 위치 변경 감지 → 자동 리로드 (첫 emit은 초기 로드와 중복이므로 스킵)
        savedLocationRepository.getAll()
            .distinctUntilChangedBy { it.map { loc -> loc.id } }
            .drop(1)
            .onEach { processIntent(WeatherContract.Intent.Refresh) }
            .launchIn(viewModelScope)
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
            is WeatherContract.Intent.RefreshLocation -> refreshLocation(intent.index)
            is WeatherContract.Intent.ShowInfoDialog -> updateUiControl { copy(showInfoDialog = true) }
            is WeatherContract.Intent.DismissInfoDialog -> updateUiControl { copy(showInfoDialog = false) }
            is WeatherContract.Intent.RequestDeleteLocation -> updateUiControl {
                copy(deleteConfirm = WeatherContract.DeleteConfirmState(intent.locationId, intent.locationName))
            }
            is WeatherContract.Intent.ConfirmDeleteLocation -> confirmDelete()
            is WeatherContract.Intent.DismissDeleteDialog -> updateUiControl { copy(deleteConfirm = null) }
        }
    }

    // ── 상태 업데이트 헬퍼 ───────────────────────────────────────────────────

    private inline fun updateLocations(transform: LocationsState.() -> LocationsState) {
        _locationsState.value = _locationsState.value.transform()
    }

    private inline fun updateUiControl(transform: UiControlState.() -> UiControlState) {
        _uiControlState.value = _uiControlState.value.transform()
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            // 리프레시 중이면 현재 데이터 유지하면서 인디케이터만 표시
            if (isRefresh) {
                updateUiControl { copy(isRefreshing = true) }
            } else {
                updateLocations { copy(isLoading = true) }
                updateUiControl { copy(error = null) }
            }

            // 1. 먼저 위치 목록을 즉시 표시 (데이터 로딩 전)
            val savedLocations = savedLocationRepository.getAll().first()
            // 스마트 위치 사용 (법정동 centroid)
            val smartLocation = locationProvider.getCurrentSmartLocation()
            
            val initialLocationList = mutableListOf<LocationWeatherData>()
            val needsPermission = !locationProvider.hasLocationPermission()
            
            // 현재 위치 항상 추가 (권한 없어도 페이지는 유지)
            if (smartLocation != null) {
                // 법정동 이름에서 동 이름만 추출 (예: "서울특별시 강남구 역삼동" → "역삼동")
                val shortName = smartLocation.bjdName.split(" ").lastOrNull() ?: "현재 위치"
                initialLocationList.add(
                    LocationWeatherData(
                        locationName = shortName,
                        isCurrentLocation = true,
                        lat = smartLocation.lat,
                        lng = smartLocation.lng
                    )
                )
            } else {
                // 권한 없거나 위치 못 가져올 때도 "현재 위치" 페이지 유지
                initialLocationList.add(
                    LocationWeatherData(
                        locationName = "현재 위치",
                        isCurrentLocation = true,
                        needsPermission = needsPermission
                    )
                )
            }
            
            // 저장된 위치들 추가 (로딩 상태)
            savedLocations.forEach { saved ->
                initialLocationList.add(
                    LocationWeatherData(
                        locationName = saved.displayName,
                        isCurrentLocation = false,
                        savedLocationId = saved.id,
                        lat = saved.lat,
                        lng = saved.lng
                    )
                )
            }
            
            // 즉시 위치 목록 표시
            updateLocations { copy(locations = initialLocationList, isLoading = true) }

            // 2. 현재 위치 날씨 데이터 로드 (centroid 좌표 사용)
            if (smartLocation != null) {
                val output = fetchWeatherUseCase(smartLocation.lat, smartLocation.lng)
                val (airResult, weatherResult) = toDataResults(output.result)
                updateLocationData(0, airResult, weatherResult)
                updateLocations { copy(isLoading = false) }
                updateUiControl { copy(isRefreshing = false, needsLocationPermission = false, error = null) }
            } else {
                updateLocations { copy(isLoading = false) }
                updateUiControl {
                    copy(
                        isRefreshing = false,
                        needsLocationPermission = needsPermission,
                        error = if (!needsPermission) WeatherContract.ErrorState(
                            message = UiText.StringResource(R.string.error_location_unavailable),
                            action = WeatherContract.Intent.Refresh
                        ) else null
                    )
                }
            }

            // 3. 저장된 위치들 날씨 데이터 일괄 로드 (배치 API, centroid 변환 포함)
            if (savedLocations.isNotEmpty()) {
                val startIndex = if (smartLocation != null) 1 else 0
                val locations = savedLocations.map { it.lat to it.lng }
                val batchOutputs = fetchWeatherUseCase.fetchBatch(locations)

                batchOutputs.forEachIndexed { index, output ->
                    val (airResult, weatherResult) = toDataResults(output.result)
                    updateLocationData(startIndex + index, airResult, weatherResult)
                }
            }

            updateLocations { copy(isLoading = false) }
        }
    }
    
    /** 특정 위치만 개별 새로고침 */
    private fun refreshLocation(index: Int) {
        val location = _locationsState.value.locations.getOrNull(index) ?: return

        viewModelScope.launch {
            // 해당 위치만 refreshing 표시
            updateLocationRefreshing(index, true)

            val lat: Double
            val lng: Double

            if (location.isCurrentLocation) {
                val smart = locationProvider.getCurrentSmartLocation()
                if (smart == null) {
                    updateLocationRefreshing(index, false)
                    return@launch
                }
                lat = smart.lat
                lng = smart.lng
            } else {
                lat = location.lat
                lng = location.lng
            }

            val output = fetchWeatherUseCase(lat, lng)
            val (airResult, weatherResult) = toDataResults(output.result)
            updateLocationData(index, airResult, weatherResult)
            updateLocationRefreshing(index, false)
        }
    }

    private fun updateLocationRefreshing(index: Int, isRefreshing: Boolean) {
        updateLocations {
            val newLocations = locations.toMutableList()
            if (index < newLocations.size) {
                newLocations[index] = newLocations[index].copy(isRefreshing = isRefreshing)
            }
            copy(locations = newLocations)
        }
    }

    /** 특정 위치의 날씨 데이터 업데이트 */
    private fun updateLocationData(
        index: Int,
        airResult: WeatherContract.DataResult<AirQuality>,
        weatherResult: WeatherContract.DataResult<WeatherData>
    ) {
        updateLocations {
            val newLocations = locations.toMutableList()
            if (index < newLocations.size) {
                newLocations[index] = newLocations[index].copy(
                    airQuality = airResult,
                    weather = weatherResult
                )
            }
            copy(locations = newLocations)
        }
    }

    private fun toDataResults(
        result: seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
    ): Pair<WeatherContract.DataResult<AirQuality>, WeatherContract.DataResult<WeatherData>> {
        val airResult = WeatherContract.DataResult(
            data = result.airQuality.getOrNull(),
            error = result.airQuality.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
        )
        val weatherResult = WeatherContract.DataResult(
            data = result.weather.getOrNull(),
            error = result.weather.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
        )
        return airResult to weatherResult
    }

    private fun confirmDelete() {
        val deleteState = _uiControlState.value.deleteConfirm ?: return
        viewModelScope.launch {
            savedLocationRepository.delete(deleteState.locationId)
            updateUiControl { copy(deleteConfirm = null) }
            // 리로드는 savedLocationRepository.getAll() observe에서 자동 처리
        }
    }

    // ── Effect 발송 ─────────────────────────────────────────────────────────

    private fun sendEffect(effect: WeatherContract.Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
