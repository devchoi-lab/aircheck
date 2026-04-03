package seriouschoi.aircheck.feature.weather.ui.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.feature.weather.ui.calendar.CalendarContract as Contract
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(Contract.State())
    val state: StateFlow<Contract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Contract.Effect>()
    val effect = _effect.asSharedFlow()

    private var hourlyForecast: List<HourlyForecast> = emptyList()

    init {
        checkPermissionAndLoad()
    }

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: Contract.Intent) {
        when (intent) {
            is Contract.Intent.Refresh -> refresh()
            is Contract.Intent.AddEvent -> addEvent()
            is Contract.Intent.EditEvent -> editEvent(intent.event)
            is Contract.Intent.OnPermissionResult -> onPermissionResult(intent.granted)
            is Contract.Intent.UpdateWeatherData -> updateWeatherData(
                intent.hourlyForecast,
                intent.locationName,
            )
        }
    }

    // ── 캘린더 액션 ─────────────────────────────────────────────────────────

    private fun addEvent() {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        emitEffect(Contract.Effect.LaunchCalendarIntent(intent))
    }

    private fun editEvent(event: CalendarEvent) {
        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = eventUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        emitEffect(Contract.Effect.LaunchCalendarIntent(intent))
    }

    private fun emitEffect(effect: Contract.Effect) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    // ── 권한 & 데이터 ───────────────────────────────────────────────────────

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

    private fun refresh() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        _state.update { it.copy(hasCalendarPermission = hasPermission) }

        if (hasPermission) {
            loadCalendarEvents(isRefresh = true)
        } else {
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun checkPermissionAndLoad() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        _state.update { it.copy(hasCalendarPermission = hasPermission) }

        if (hasPermission) {
            loadCalendarEvents(isRefresh = false)
        } else {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadCalendarEvents(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true) }
            } else {
                _state.update { it.copy(isLoading = true) }
            }

            val events = withContext(Dispatchers.IO) {
                calendarRepository.getTodayEvents()
            }
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
                    isRefreshing = false,
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
