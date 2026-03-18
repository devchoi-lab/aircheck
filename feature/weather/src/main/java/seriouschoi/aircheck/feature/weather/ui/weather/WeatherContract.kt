package seriouschoi.aircheck.feature.weather.ui.weather

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.resource.UiText

/**
 * Weather 화면 MVI Contract
 */
object WeatherContract {

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    sealed interface Intent {
        /** 초기 로드 또는 새로고침 */
        data object Load : Intent

        /** Pull-to-refresh */
        data object Refresh : Intent

        /** 공기질 기준 안내 다이얼로그 표시 */
        data object ShowInfoDialog : Intent

        /** 공기질 기준 안내 다이얼로그 닫기 */
        data object DismissInfoDialog : Intent

        /** 저장된 위치 삭제 요청 (확인 다이얼로그 표시) */
        data class RequestDeleteLocation(val locationId: Long, val locationName: String) : Intent

        /** 삭제 확인 */
        data object ConfirmDeleteLocation : Intent

        /** 삭제 취소 (다이얼로그 닫기) */
        data object DismissDeleteDialog : Intent
    }

    // ── State (UI 상태) ─────────────────────────────────────────────────────

    /** 화면 전체 UI 상태 (단일 data class — 캐스팅 불필요) */
    data class UiState(
        val isLoading: Boolean = true,
        val airQuality: DataResult<AirQuality> = DataResult(),
        val weather: DataResult<WeatherData> = DataResult(),
        val isRefreshing: Boolean = false,
        val error: ErrorState? = null,
        val showInfoDialog: Boolean = false,
        /** 삭제 확인 다이얼로그 (null이면 비표시) */
        val deleteConfirm: DeleteConfirmState? = null
    )

    /** 삭제 확인 다이얼로그 상태 */
    data class DeleteConfirmState(
        val locationId: Long,
        val locationName: String
    )

    /** 에러 상태 (메시지 + 버튼 액션) */
    data class ErrorState(
        val message: UiText,
        val action: Intent = Intent.Refresh
    )

    /** 데이터 결과 (성공/에러) */
    data class DataResult<T>(
        val data: T? = null,
        val error: UiText? = null
    )

    // ── Side Effect (일회성 이벤트) ──────────────────────────────────────────

    sealed interface Effect {
        data class ShowToast(val message: UiText) : Effect
        data object ScrollToTop : Effect
    }
}
