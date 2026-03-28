package seriouschoi.aircheck

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import seriouschoi.aircheck.core.domain.usecase.CheckVersionUseCase
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val checkVersionUseCase: CheckVersionUseCase
) : AndroidViewModel(application) {
    
    private val _versionState = MutableStateFlow<VersionState>(VersionState.Checking)
    val versionState: StateFlow<VersionState> = _versionState.asStateFlow()
    
    init {
        checkVersion()
    }
    
    private fun checkVersion() {
        viewModelScope.launch {
            val versionCode = getVersionCode()
            checkVersionUseCase(versionCode)
                .onSuccess { result ->
                    _versionState.value = if (result.needUpdate) {
                        VersionState.UpdateRequired(result.updateUrl)
                    } else {
                        VersionState.Ok
                    }
                }
                .onFailure {
                    // 실패 시에도 앱 사용 가능
                    _versionState.value = VersionState.Ok
                }
        }
    }
    
    private fun getVersionCode(): Int {
        return try {
            val context = getApplication<Application>()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }
}

sealed interface VersionState {
    data object Checking : VersionState
    data object Ok : VersionState
    data class UpdateRequired(val updateUrl: String?) : VersionState
}
