package seriouschoi.aircheck.feature.weather.ui.weather

import seriouschoi.aircheck.core.resource.UiText

/**
 * Weather 화면 MVI Contract
 */
object WeatherContract {

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    sealed interface Intent {
        /** 특정 위치 개별 새로고침 */
        data class RefreshLocation(val index: Int) : Intent

        /** 공기질 기준 안내 다이얼로그 표시 */
        data object ShowInfoDialog : Intent

        /** 공기질 기준 안내 다이얼로그 닫기 */
        data object DismissInfoDialog : Intent

        /** 페이지 변경 시 해당 위치 로드 (lazy loading) */
        data class OnPageVisible(val index: Int) : Intent
    }

    // ── 공통 데이터 타입 ─────────────────────────────────────────────────────

    /** 데이터 결과 (성공/에러) */
    data class DataResult<T>(
        val data: T? = null,
        val error: UiText? = null
    )

    // ── Side Effect (일회성 이벤트) ──────────────────────────────────────────

    sealed interface Effect {
    }
}
