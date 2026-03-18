package seriouschoi.aircheck.feature.weather.ui.locationpicker

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.RegionSearchResult
import seriouschoi.aircheck.core.domain.model.SavedLocation
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import seriouschoi.aircheck.core.domain.usecase.AddSavedLocationUseCase
import seriouschoi.aircheck.core.domain.usecase.DuplicateLocationException
import seriouschoi.aircheck.core.domain.usecase.LimitExceededException
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.resource.UiText

import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract.Effect
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract.Intent
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract.UiState
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
internal class LocationPickerViewModel @Inject constructor(
    private val regionRepository: RegionRepository,
    private val addSavedLocation: AddSavedLocationUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    // ── Intent ───────────────────────────────────────────────────────────────

    private val _intent = Channel<Intent>(Channel.UNLIMITED)

    // ── State ────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Effect ───────────────────────────────────────────────────────────────

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** 현재 위치 (init 시 한 번 조회) */
    private var currentLocation: Location? = null

    init {
        _intent.receiveAsFlow()
            .onEach { intent -> handleIntent(intent) }
            .launchIn(viewModelScope)

        loadRegions()
    }

    // ── Intent 발송 ──────────────────────────────────────────────────────────

    fun processIntent(intent: Intent) {
        viewModelScope.launch {
            _intent.send(intent)
        }
    }

    // ── Intent 처리 ──────────────────────────────────────────────────────────

    private fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.SearchQueryChanged -> onSearchQueryChange(intent.query)
            is Intent.RegionClicked -> onRegionClick(intent.region)
            is Intent.SearchResultClicked -> onSearchResultClick(intent.result)
            is Intent.BackClicked -> onBackClick()
            is Intent.DismissAlert -> _uiState.value = _uiState.value.copy(alertMessage = null)
        }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun loadRegions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                currentLocation = locationProvider.getCurrentLocation()
                val regions = regionRepository.getTopLevelRegions()
                _uiState.value = _uiState.value.copy(
                    regions = regions.sortedByDistance(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.DynamicString(e.message ?: "지역 데이터 로드 실패")
                )
            }
        }
    }

    private fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                // 검색어 비우면 → 탐색 모드로 복귀 (현재 경로 기준)
                val currentPath = _uiState.value.currentPath
                val regions = if (currentPath.isEmpty()) {
                    regionRepository.getTopLevelRegions()
                } else {
                    regionRepository.getRegionByPath(currentPath)?.children ?: emptyList()
                }
                _uiState.value = _uiState.value.copy(
                    regions = regions.sortedByDistance(),
                    searchResults = emptyList()
                )
            } else {
                // 전체 계층 재귀 검색
                val results = regionRepository.deepSearchRegions(query)
                _uiState.value = _uiState.value.copy(
                    searchResults = results.sortedBySearchResultDistance()
                )
            }
        }
    }

    /** 탐색 모드: 지역 클릭 */
    private fun onRegionClick(region: Region) {
        if (region.hasChildren) {
            viewModelScope.launch {
                val newPath = _uiState.value.currentPath + region.name
                val children = regionRepository.getRegionByPath(newPath)?.children ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    currentPath = newPath,
                    regions = children.sortedByDistance(),
                    searchQuery = ""
                )
            }
        } else {
            saveLocation(region, _uiState.value.currentPath)
        }
    }

    /** 검색 모드: 검색 결과 클릭 */
    private fun onSearchResultClick(result: RegionSearchResult) {
        val region = result.region
        if (region.hasChildren) {
            // 하위 목록으로 진입 + 검색 모드 해제
            val newPath = result.path + region.name
            _uiState.value = _uiState.value.copy(
                currentPath = newPath,
                regions = region.children.sortedByDistance(),
                searchQuery = "",
                searchResults = emptyList()
            )
        } else {
            saveLocation(region, result.path)
        }
    }

    private fun onBackClick() {
        val currentPath = _uiState.value.currentPath
        if (currentPath.isEmpty()) return

        viewModelScope.launch {
            val newPath = currentPath.dropLast(1)
            val regions = if (newPath.isEmpty()) {
                regionRepository.getTopLevelRegions()
            } else {
                regionRepository.getRegionByPath(newPath)?.children ?: emptyList()
            }
            _uiState.value = _uiState.value.copy(
                currentPath = newPath,
                regions = regions.sortedByDistance(),
                searchQuery = ""
            )
        }
    }

    private fun saveLocation(region: Region, path: List<String>) {
        viewModelScope.launch {
            val displayName = (path + region.name).joinToString(" ")
            val location = SavedLocation(
                name = region.name,
                displayName = displayName,
                lat = region.lat,
                lng = region.lng
            )
            addSavedLocation(location)
                .onSuccess { _effect.send(Effect.LocationSaved) }
                .onFailure { e ->
                    val message = when (e) {
                        is DuplicateLocationException -> UiText.StringResource(
                            CoreR.string.error_duplicate_location
                        )
                        is LimitExceededException -> UiText.StringResource(
                            CoreR.string.error_saved_location_limit,
                            AddSavedLocationUseCase.MAX_SAVED_LOCATIONS
                        )
                        else -> UiText.StringResource(CoreR.string.error_save_failed)
                    }
                    _uiState.value = _uiState.value.copy(alertMessage = message)
                }
        }
    }

    // ── 거리 기반 정렬 ───────────────────────────────────────────────────────

    /** Region 리스트를 현재 위치와 가까운 순으로 정렬 */
    private fun List<Region>.sortedByDistance(): List<Region> {
        val loc = currentLocation ?: return this
        return sortedBy { region ->
            haversineDistance(loc.latitude, loc.longitude, region.lat, region.lng)
        }
    }

    /** SearchResult 리스트를 현재 위치와 가까운 순으로 정렬 */
    private fun List<RegionSearchResult>.sortedBySearchResultDistance(): List<RegionSearchResult> {
        val loc = currentLocation ?: return this
        return sortedBy { result ->
            haversineDistance(loc.latitude, loc.longitude, result.region.lat, result.region.lng)
        }
    }

    /** Haversine 공식으로 두 좌표 간 거리 (km) 계산 */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // 지구 반지름 (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
