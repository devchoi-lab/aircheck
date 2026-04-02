package seriouschoi.aircheck.feature.weather.ui.calendar

import java.time.LocalDate
import java.time.LocalDateTime

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

data class CalendarUiState(
    val date: LocalDate = LocalDate.now(),
    val events: List<CalendarEventWithWeather> = emptyList(),
    val isLoading: Boolean = true,
    val hasCalendarPermission: Boolean = false,
    val currentLocationName: String = "",
)

sealed interface CalendarIntent {
    data object RequestPermission : CalendarIntent
    data object Refresh : CalendarIntent
}

sealed interface CalendarEffect {
    data object RequestCalendarPermission : CalendarEffect
}
