package seriouschoi.aircheck.feature.weather.ui.locationpicker

import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.resource.UiText

/**
 * LocationPicker 화면 MVI Contract
 */
object LocationPickerContract {

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    sealed interface Intent {
        /** 검색어 변경 */
        data class SearchQueryChanged(val query: String) : Intent

        /** 지역 클릭 (하위 탐색 or 선택) */
        data class RegionClicked(val region: Region) : Intent

        /** 뒤로가기 (상위 지역으로) */
        data object BackClicked : Intent
    }

    // ── State (UI 상태) ─────────────────────────────────────────────────────

    data class UiState(
        val regions: List<Region> = emptyList(),
        val currentPath: List<String> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = true,
        val error: UiText? = null
    )

    // ── Side Effect (일회성 이벤트) ──────────────────────────────────────────

    sealed interface Effect {
        /** 위치 저장 완료 → 이전 화면으로 돌아가기 */
        data object LocationSaved : Effect
    }
}
