package seriouschoi.aircheck.feature.weather.ui

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
    }
    
    // ── State (UI 상태) ─────────────────────────────────────────────────────
    
    sealed interface State {
        data object Loading : State
        
        data class Success(
            val airQuality: DataResult<AirQuality>,
            val weather: DataResult<WeatherData>,
            val isRefreshing: Boolean = false
        ) : State
        
        data class Error(val message: UiText) : State
    }
    
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
