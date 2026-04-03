package seriouschoi.aircheck.feature.weather.ui.calendar

import seriouschoi.aircheck.core.domain.model.HourlyForecast
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Calendar 화면 MVI Contract
 */
object CalendarContract {

    // ── Intent (사용자 액션) ────────────────────────────────────────────────

    sealed interface Intent {
        data object Refresh : Intent
        data class OnPermissionResult(val granted: Boolean) : Intent
        data class UpdateWeatherData(
            val hourlyForecast: List<HourlyForecast>,
            val locationName: String,
        ) : Intent
    }

    // ── State ──────────────────────────────────────────────────────────────

    data class State(
        val date: LocalDate = LocalDate.now(),
        val events: List<CalendarEventWithWeather> = emptyList(),
        val isLoading: Boolean = true,
        val hasCalendarPermission: Boolean = false,
        val currentLocationName: String = "",
    )
}

// ── 데이터 모델 ─────────────────────────────────────────────────────────

data class CalendarEvent(
    val id: Long,
    val title: String,
    val location: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean,
)

data class CalendarEventWithWeather(
    val event: CalendarEvent,
    val temperature: Int?,
    val weatherIcon: String?,
    val airQualityLevel: Int?,
    val airQualityLabel: String?,
)
