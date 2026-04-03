package seriouschoi.aircheck.feature.weather.ui.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarContract.State())
    val state: StateFlow<CalendarContract.State> = _state.asStateFlow()

    private var hourlyForecast: List<HourlyForecast> = emptyList()

    init {
        checkPermissionAndLoad()
    }

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: CalendarContract.Intent) {
        when (intent) {
            is CalendarContract.Intent.Refresh -> checkPermissionAndLoad()
            is CalendarContract.Intent.OnPermissionResult -> onPermissionResult(intent.granted)
            is CalendarContract.Intent.UpdateWeatherData -> updateWeatherData(
                intent.hourlyForecast,
                intent.locationName,
            )
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasCalendarPermission = granted) }
        if (granted) {
            loadCalendarEvents()
        }
    }

    private fun updateWeatherData(hourlyForecast: List<HourlyForecast>, locationName: String) {
        this.hourlyForecast = hourlyForecast
        _state.update { it.copy(currentLocationName = locationName) }
        if (_state.value.hasCalendarPermission) {
            loadCalendarEvents()
        }
    }

    private fun checkPermissionAndLoad() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        _state.update { it.copy(hasCalendarPermission = hasPermission) }

        if (hasPermission) {
            loadCalendarEvents()
        } else {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadCalendarEvents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val events = calendarRepository.getTodayEvents()
            val eventsWithWeather = events.map { event ->
                val weather = findWeatherForTime(event)
                CalendarEventWithWeather(
                    event = event,
                    temperature = weather?.temperature?.toInt(),
                    weatherIcon = weather?.condition?.name,
                    airQualityLevel = null,
                    airQualityLabel = null,
                )
            }

            _state.update {
                it.copy(
                    events = eventsWithWeather,
                    isLoading = false,
                )
            }
        }
    }

    private fun findWeatherForTime(event: CalendarEvent): HourlyForecast? {
        if (hourlyForecast.isEmpty()) return null

        val eventHour = if (event.isAllDay) {
            12
        } else {
            event.startTime.hour
        }

        return hourlyForecast.minByOrNull { forecast ->
            val forecastHour = forecast.hour.replace("시", "").toIntOrNull() ?: 0
            kotlin.math.abs(forecastHour - eventHour)
        }
    }
}
