package seriouschoi.aircheck.feature.weather.ui.locationpicker

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
import seriouschoi.aircheck.core.domain.model.SavedLocation
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.resource.UiText
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract.Effect
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract.Intent
import seriouschoi.aircheck.feature.weather.ui.locationpicker.LocationPickerContract.UiState
import javax.inject.Inject

@HiltViewModel
internal class LocationPickerViewModel @Inject constructor(
    private val regionRepository: RegionRepository,
    private val savedLocationRepository: SavedLocationRepository
) : ViewModel() {

    // ── Intent ───────────────────────────────────────────────────────────────

    private val _intent = Channel<Intent>(Channel.UNLIMITED)

    // ── State ────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Effect ───────────────────────────────────────────────────────────────

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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
            is Intent.BackClicked -> onBackClick()
        }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun loadRegions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val regions = regionRepository.getTopLevelRegions()
                _uiState.value = _uiState.value.copy(
                    regions = regions,
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
            val filtered = regionRepository.searchRegions(query, _uiState.value.currentPath)
            _uiState.value = _uiState.value.copy(regions = filtered)
        }
    }

    private fun onRegionClick(region: Region) {
        if (region.hasChildren) {
            viewModelScope.launch {
                val newPath = _uiState.value.currentPath + region.name
                val children = regionRepository.getRegionByPath(newPath)?.children ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    currentPath = newPath,
                    regions = children,
                    searchQuery = ""
                )
            }
        } else {
            saveLocation(region)
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
                regions = regions,
                searchQuery = ""
            )
        }
    }

    private fun saveLocation(region: Region) {
        val lat = region.lat ?: return
        val lng = region.lng ?: return

        viewModelScope.launch {
            val displayName = (_uiState.value.currentPath + region.name).joinToString(" ")
            val location = SavedLocation(
                name = region.name,
                displayName = displayName,
                lat = lat,
                lng = lng
            )
            savedLocationRepository.add(location)
            _effect.send(Effect.LocationSaved)
        }
    }
}
