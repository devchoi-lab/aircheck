package seriouschoi.aircheck.feature.weather.ui.calendar

import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.CalendarEvent
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.domain.model.ResolvedEventLocation
import java.time.LocalDate

/**
 * Calendar 화면 MVI Contract
 */
object CalendarContract {

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    sealed interface Intent {
        data object Refresh : Intent
        data object AddEvent : Intent
        data class EditEvent(val event: CalendarEvent) : Intent
        data class OnPermissionResult(val granted: Boolean) : Intent
    }

    // ── State ──────────────────────────────────────────────────────────────

    data class State(
        val date: LocalDate = LocalDate.now(),
        val events: List<CalendarEventWithWeather> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val hasCalendarPermission: Boolean = false,
        val currentLocationName: String = "",
        val worstLevel: AqiLevel? = null,
    )

    // ── Side Effect (일회성 이벤트) ──────────────────────────────────────────

    sealed interface Effect {
        data class LaunchCalendarIntent(val intent: android.content.Intent) : Effect
    }
}

// ── UI 모델 ─────────────────────────────────────────────────────────────

/**
 * 화면 렌더링용 모델 — 도메인 [CalendarEvent] 에 시간대별 날씨/추천/위치 라벨을 덧붙인 뷰 모델.
 */
data class CalendarEventWithWeather(
    val event: CalendarEvent,
    val minFeelsLike: Int?,
    val maxFeelsLike: Int?,
    val hasPrecipitation: Boolean = false,
    val maxPrecipitationProbability: Int? = null,
    val maxPrecipitationHour: String? = null,
    val recommendations: List<Recommendation> = emptyList(),
    /** 이 일정의 날씨가 어느 법정동 기준으로 계산됐는지 표시용 (예: "서울특별시 강남구 역삼동") */
    val locationDisplayName: String = "",
    /** 위 위치가 어떻게 결정됐는지 (사용자 매핑 / 텍스트 유추 / 현재위치 fallback) */
    val locationSource: ResolvedEventLocation.Source = ResolvedEventLocation.Source.CURRENT_LOCATION,
)
