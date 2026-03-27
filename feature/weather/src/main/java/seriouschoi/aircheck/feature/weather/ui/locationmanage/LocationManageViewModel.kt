package seriouschoi.aircheck.feature.weather.ui.locationmanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import javax.inject.Inject

@HiltViewModel
class LocationManageViewModel @Inject constructor(
    private val savedLocationRepository: SavedLocationRepository,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LocationManageContract.UiState())
    val uiState: StateFlow<LocationManageContract.UiState> = _uiState.asStateFlow()
    
    init {
        loadLocations()
    }
    
    private fun loadLocations() {
        viewModelScope.launch {
            savedLocationRepository.getAll().collect { locations ->
                _uiState.update { 
                    it.copy(
                        savedLocations = locations.sortedBy { loc -> loc.order },
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun processIntent(intent: LocationManageContract.Intent) {
        when (intent) {
            is LocationManageContract.Intent.MoveLocation -> moveLocation(intent.fromIndex, intent.toIndex)
            is LocationManageContract.Intent.DeleteLocation -> deleteLocation(intent.id)
        }
    }
    
    private fun moveLocation(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.savedLocations.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentList.size) return
        if (toIndex < 0 || toIndex >= currentList.size) return
        
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        
        // Update local state immediately for responsive UI
        _uiState.update { it.copy(savedLocations = currentList) }
        
        // Persist the new order
        viewModelScope.launch {
            savedLocationRepository.updateOrder(currentList)
        }
    }
    
    private fun deleteLocation(id: Long) {
        viewModelScope.launch {
            savedLocationRepository.delete(id)
        }
    }
}
