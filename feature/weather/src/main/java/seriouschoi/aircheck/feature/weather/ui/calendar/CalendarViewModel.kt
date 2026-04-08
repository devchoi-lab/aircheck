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
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.HourlyForecast
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.feature.weather.ui.calendar.CalendarContract as Contract
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val locationProvider: LocationProvider,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(Contract.State())
    val state: StateFlow<Contract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Contract.Effect>()
    val effect = _effect.asSharedFlow()

    private var weatherData: WeatherData? = null
    private var airQuality: AirQuality? = null

    init {
        checkPermissionAndLoad()
        loadWeatherData()
    }

    // ── Intent 처리 ─────────────────────────────────────────────────────────

    fun processIntent(intent: Contract.Intent) {
        when (intent) {
            is Contract.Intent.Refresh -> refresh()
            is Contract.Intent.AddEvent -> addEvent()
            is Contract.Intent.EditEvent -> editEvent(intent.event)
            is Contract.Intent.OnPermissionResult -> onPermissionResult(intent.granted)
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

    // ── 날씨 데이터 ────────────────────────────────────────────────────────

    private fun loadWeatherData() {
        viewModelScope.launch {
            val smartLocation = withContext(Dispatchers.IO) {
                locationProvider.getCurrentSmartLocation()
            } ?: return@launch

            _state.update { it.copy(currentLocationName = smartLocation.bjdName) }

            val output = withContext(Dispatchers.IO) {
                fetchWeatherUseCase(smartLocation.bjdCode)
            }

            weatherData = output.result.weather.getOrNull()
            airQuality = output.result.airQuality.getOrNull()

            _state.update {
                it.copy(worstLevel = airQuality?.worstLevel)
            }

            if (_state.value.hasCalendarPermission) {
                loadCalendarEvents()
            }
        }
    }

    // ── 권한 & 데이터 ───────────────────────────────────────────────────────

    private fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasCalendarPermission = granted) }
        if (granted) {
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
            loadWeatherData()
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
                val forecasts = findForecastsForEvent(event)
                val maxPrecip = forecasts.maxByOrNull { it.precipitationProbability }
                CalendarEventWithWeather(
                    event = event,
                    minFeelsLike = forecasts.minOfOrNull { it.feelsLike.toInt() },
                    maxFeelsLike = forecasts.maxOfOrNull { it.feelsLike.toInt() },
                    hasPrecipitation = forecasts.any { it.condition.isRainy() || it.condition.isSnowy() },
                    maxPrecipitationProbability = maxPrecip?.precipitationProbability,
                    maxPrecipitationHour = maxPrecip?.hour,
                    recommendations = getRecommendationsForEvent(event),
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

    private fun findForecastsForEvent(event: CalendarEvent): List<HourlyForecast> {
        val hourlyForecast = weatherData?.hourlyForecast ?: return emptyList()
        if (hourlyForecast.isEmpty()) return emptyList()

        val startHour = if (event.isAllDay) 0 else event.startTime.hour
        val endHour = if (event.isAllDay) 24 else event.endTime.hour.coerceAtLeast(startHour + 1)

        val forecasts = hourlyForecast.filter { forecast ->
            val forecastHour = forecast.hour.replace("시", "").toIntOrNull() ?: 0
            forecastHour in startHour until endHour
        }

        // 범위에 정확히 맞는 예보가 없으면 가장 가까운 1개
        if (forecasts.isEmpty()) {
            val targetHour = if (event.isAllDay) 12 else event.startTime.hour
            return listOfNotNull(
                hourlyForecast.minByOrNull { forecast ->
                    val forecastHour = forecast.hour.replace("시", "").toIntOrNull() ?: 0
                    kotlin.math.abs(forecastHour - targetHour)
                }
            )
        }
        return forecasts
    }

    /**
     * 일정 시간대 기준 추천 아이템 계산
     * - 의상: 해당 시간 체감온도 기준
     * - 우산/눈: 해당 시간부터 일정 종료까지의 예보 기준
     * - 마스크: 당일 공기질 기준 (전역)
     */
    private fun getRecommendationsForEvent(event: CalendarEvent): List<Recommendation> {
        val eventForecasts = findForecastsForEvent(event)
        if (eventForecasts.isEmpty()) return emptyList()

        return buildList {
            // 1. 체감온도 기반 의상 (최저/최고 체감온도 각각)
            val minFeelsLike = eventForecasts.minOf { it.feelsLike }
            val maxFeelsLike = eventForecasts.maxOf { it.feelsLike }
            val minClothing = RecommendedClothing.fromFeelsLike(minFeelsLike)
            val maxClothing = RecommendedClothing.fromFeelsLike(maxFeelsLike)
            add(Recommendation.Clothing(minClothing))
            if (maxClothing != minClothing) {
                add(Recommendation.Clothing(maxClothing))
            }

            // 2. 우산 (일정 시간대 비 예보)
            eventForecasts
                .filter { it.condition.isRainy() }
                .maxOfOrNull { it.precipitationProbability }
                ?.let { add(Recommendation.Umbrella(it)) }

            // 3. 눈 (일정 시간대 눈 예보)
            eventForecasts
                .filter { it.condition.isSnowy() }
                .sumOf { it.snowfall }
                .takeIf { it > 0 }
                ?.let { add(Recommendation.Snow(it)) }

            // 4. 마스크 (공기질은 당일 전역)
            airQuality?.worstLevel
                ?.takeIf { it.ordinal >= AqiLevel.UNHEALTHY_SENSITIVE.ordinal && it != AqiLevel.UNKNOWN }
                ?.let { add(Recommendation.Mask(it)) }
        }
    }
}
