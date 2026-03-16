package seriouschoi.aircheck

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

/**
 * 날씨/공기질 각각 독립적인 성공/실패 처리
 * 하나가 실패해도 나머지는 표시
 */
data class DataResult<T>(
    val data: T? = null,
    val error: String? = null
) {
    val isSuccess get() = data != null
}

sealed interface MainUiState {
    data object Idle : MainUiState
    data object Loading : MainUiState
    data class Success(
        val airQuality: DataResult<AirQuality>,
        val weather: DataResult<WeatherData>
    ) : MainUiState
    data class Error(val message: String) : MainUiState  // 위치 실패 등 치명적 오류
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val airRepo = AirQualityRepository()
    private val weatherRepo = WeatherRepository()
    private val fusedLocation = LocationServices.getFusedLocationProviderClient(app)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState = _uiState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun load() {
        _uiState.update { MainUiState.Loading }
        viewModelScope.launch {
            try {
                val cts = CancellationTokenSource()
                val location = fusedLocation.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()

                if (location == null) {
                    _uiState.update {
                        MainUiState.Error("위치를 가져올 수 없어요.\n위치 서비스가 켜져 있는지 확인해주세요.")
                    }
                    return@launch
                }

                // 공기질 + 날씨 병렬 요청 — 각각 독립 처리
                val airDeferred = async { airRepo.fetch(location.latitude, location.longitude) }
                val weatherDeferred = async { weatherRepo.fetch(location.latitude, location.longitude) }

                val airResult = airDeferred.await()
                val weatherResult = weatherDeferred.await()

                _uiState.update {
                    MainUiState.Success(
                        airQuality = DataResult(
                            data = airResult.getOrNull(),
                            error = airResult.exceptionOrNull()?.message
                        ),
                        weather = DataResult(
                            data = weatherResult.getOrNull(),
                            error = weatherResult.exceptionOrNull()?.message
                        )
                    )
                }

            } catch (e: Exception) {
                _uiState.update { MainUiState.Error("위치 가져오기 실패: ${e.message}") }
            }
        }
    }
}
