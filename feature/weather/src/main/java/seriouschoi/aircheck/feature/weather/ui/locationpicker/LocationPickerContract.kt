package seriouschoi.aircheck.feature.weather.ui.locationpicker

import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.RegionSearchResult
import seriouschoi.aircheck.core.resource.UiText

/**
 * LocationPicker 화면 MVI Contract
 */
object LocationPickerContract {

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    sealed interface Intent {
        /** 검색어 변경 */
        data class SearchQueryChanged(val query: String) : Intent

        /** 탐색 모드: 지역 클릭 (하위 탐색 or 선택) */
        data class RegionClicked(val region: Region) : Intent

        /** 검색 모드: 검색 결과 클릭 */
        data class SearchResultClicked(val result: RegionSearchResult) : Intent

        /** 뒤로가기 (상위 지역으로) */
        data object BackClicked : Intent

        /** 알림 다이얼로그 닫기 */
        data object DismissAlert : Intent
    }

    // ── State (UI 상태) ─────────────────────────────────────────────────────

    data class UiState(
        /** 탐색 모드: 현재 depth의 지역 목록 */
        val regions: List<Region> = emptyList(),
        /** 탐색 모드: 현재 경로 */
        val currentPath: List<String> = emptyList(),
        /** 검색어 (비어있으면 탐색 모드, 있으면 검색 모드) */
        val searchQuery: String = "",
        /** 검색 모드: 전체 계층 검색 결과 */
        val searchResults: List<RegionSearchResult> = emptyList(),
        val isLoading: Boolean = true,
        val error: UiText? = null,
        /** 알림 다이얼로그 메시지 (null이면 비표시) */
        val alertMessage: UiText? = null
    ) {
        /** 검색 모드 여부 */
        val isSearching: Boolean get() = searchQuery.isNotBlank()
    }

    // ── Side Effect (일회성 이벤트) ──────────────────────────────────────────

    sealed interface Effect {
        /** 위치 저장 완료 → 이전 화면으로 돌아가기 */
        data object LocationSaved : Effect
    }
}
