package seriouschoi.aircheck.feature.weather.ui.locationmanage

import seriouschoi.aircheck.core.domain.model.SavedLocation

/**
 * 위치 관리 화면 Contract
 */
object LocationManageContract {
    
    data class UiState(
        val savedLocations: List<SavedLocation> = emptyList(),
        val isLoading: Boolean = true,
    )
    
    sealed interface Intent {
        data class MoveLocation(val fromIndex: Int, val toIndex: Int) : Intent
        data class DeleteLocation(val id: Long) : Intent
    }
    
    sealed interface Effect {
        data object NavigateBack : Effect
    }
}
