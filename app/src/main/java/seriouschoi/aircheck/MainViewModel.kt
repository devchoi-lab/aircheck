package seriouschoi.aircheck

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

sealed interface MainUiState {
    data object Idle : MainUiState
    data object Loading : MainUiState
    data class Success(val airQuality: AirQuality) : MainUiState
    data class Error(val message: String) : MainUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AirQualityRepository()
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
                    _uiState.update { MainUiState.Error("위치를 가져올 수 없어요.\n위치 서비스가 켜져 있는지 확인해주세요.") }
                    return@launch
                }

                repo.fetch(location.latitude, location.longitude)
                    .onSuccess { air -> _uiState.update { MainUiState.Success(air) } }
                    .onFailure { e -> _uiState.update { MainUiState.Error(e.message ?: "알 수 없는 오류") } }

            } catch (e: Exception) {
                _uiState.update { MainUiState.Error("위치 가져오기 실패: ${e.message}") }
            }
        }
    }
}
