package seriouschoi.aircheck.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.SavedLocation
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import javax.inject.Inject

data class LocationPickerState(
    val regions: List<Region> = emptyList(),
    val currentPath: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val locationSaved: Boolean = false
)

@HiltViewModel
internal class LocationPickerViewModel @Inject constructor(
    private val regionRepository: RegionRepository,
    private val savedLocationRepository: SavedLocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LocationPickerState())
    val state: StateFlow<LocationPickerState> = _state.asStateFlow()

    init {
        loadRegions()
    }

    private fun loadRegions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val regions = regionRepository.getTopLevelRegions()
            _state.value = _state.value.copy(
                regions = regions,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            val filtered = regionRepository.searchRegions(query, _state.value.currentPath)
            _state.value = _state.value.copy(regions = filtered)
        }
    }

    fun onRegionClick(region: Region) {
        if (region.hasChildren) {
            // 하위 지역으로 이동
            viewModelScope.launch {
                val newPath = _state.value.currentPath + region.name
                val children = regionRepository.getRegionByPath(newPath)?.children ?: emptyList()
                _state.value = _state.value.copy(
                    currentPath = newPath,
                    regions = children,
                    searchQuery = ""
                )
            }
        } else {
            // 최하위 지역 선택 - 저장
            saveLocation(region)
        }
    }

    fun onBackClick() {
        val currentPath = _state.value.currentPath
        if (currentPath.isEmpty()) return
        
        viewModelScope.launch {
            val newPath = currentPath.dropLast(1)
            val regions = if (newPath.isEmpty()) {
                regionRepository.getTopLevelRegions()
            } else {
                regionRepository.getRegionByPath(newPath)?.children ?: emptyList()
            }
            _state.value = _state.value.copy(
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
            val displayName = (_state.value.currentPath + region.name).joinToString(" ")
            val location = SavedLocation(
                name = region.name,
                displayName = displayName,
                lat = lat,
                lng = lng
            )
            savedLocationRepository.add(location)
            _state.value = _state.value.copy(locationSaved = true)
        }
    }
}
