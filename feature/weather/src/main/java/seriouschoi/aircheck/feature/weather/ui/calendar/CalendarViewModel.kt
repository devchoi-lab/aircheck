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
import seriouschoi.aircheck.core.domain.model.CalendarEvent
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.usecase.ComputeCalendarEventWeatherInsightUseCase
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.LoadTodayCalendarEventsWithWeatherUseCase
import seriouschoi.aircheck.feature.weather.ui.calendar.CalendarContract as Contract
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationProvider: LocationProvider,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val loadTodayCalendarEventsWithWeatherUseCase: LoadTodayCalendarEventsWithWeatherUseCase,
    private val computeCalendarEventWeatherInsightUseCase: ComputeCalendarEventWeatherInsightUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(Contract.State())
    val state: StateFlow<Contract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Contract.Effect>()
    val effect = _effect.asSharedFlow()

    /** 헤더(현재 위치 카드)용 날씨/공기질. 일정별 계산에는 사용하지 않는다. */
    private var headerWeatherData: WeatherData? = null
    private var headerAirQuality: AirQuality? = null

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

            headerWeatherData = output.result.weather.getOrNull()
            headerAirQuality = output.result.airQuality.getOrNull()

            _state.update {
                it.copy(worstLevel = headerAirQuality?.worstLevel)
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

            // 도메인 use case 가 (이벤트 + 결정된 위치 + 위치별 날씨/공기질) 묶음을 모두 만들어 준다.
            // ViewModel 은 그 묶음을 받아 시간대별 forecast / 추천 아이템 / UI 라벨을 계산해
            // 화면 모델로 변환한다 — 이 변환 부분은 후속 phase 에서 도메인으로 추가 이동된다.
            val snapshots = withContext(Dispatchers.IO) {
                loadTodayCalendarEventsWithWeatherUseCase()
            }

            val eventsWithWeather = snapshots.map { snapshot ->
                val insight = computeCalendarEventWeatherInsightUseCase(snapshot)
                CalendarEventWithWeather(
                    event = insight.event,
                    minFeelsLike = insight.minFeelsLike,
                    maxFeelsLike = insight.maxFeelsLike,
                    hasPrecipitation = insight.hasPrecipitation,
                    maxPrecipitationProbability = insight.maxPrecipitationProbability,
                    maxPrecipitationHour = insight.maxPrecipitationHour,
                    recommendations = insight.recommendations,
                    locationDisplayName = insight.resolvedLocation.bjdName,
                    locationSource = insight.resolvedLocation.source,
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

}
