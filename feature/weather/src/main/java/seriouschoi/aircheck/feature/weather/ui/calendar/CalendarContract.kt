package seriouschoi.aircheck.feature.weather.ui.calendar

import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime

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

// ── 데이터 모델 ─────────────────────────────────────────────────────────

data class CalendarEvent(
    val id: Long,
    val title: String,
    val location: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean,
    val calendarName: String? = null,
)

data class CalendarEventWithWeather(
    val event: CalendarEvent,
    val temperature: Int?,
    val weather: WeatherCondition?,
    val recommendations: List<Recommendation> = emptyList(),
)
