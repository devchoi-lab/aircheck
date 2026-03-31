package seriouschoi.aircheck.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 데이터 소스
 * 
 * FusedLocationProviderClient를 사용하여 위치 업데이트를 Flow로 제공
 */
@Singleton
class LocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * 위치 업데이트 Flow
     * 
     * 100m 이상 이동 시에만 이벤트 발생
     */
    @SuppressLint("MissingPermission")
    fun locationFlow(): Flow<LocationData> = callbackFlow {
        // 권한 체크 - 권한 없으면 빈 flow로 종료 (예외 발생 안 함)
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        // 구독 시작 시 마지막 위치를 즉시 emit
        try {
            fusedLocationClient.lastLocation.await()?.let { location ->
                trySend(LocationData(location.latitude, location.longitude))
            }
        } catch (_: Exception) { /* 실패해도 업데이트 콜백으로 받으면 됨 */ }

        val priority = if (isEmulator()) Priority.PRIORITY_HIGH_ACCURACY
            else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val locationRequest = LocationRequest.Builder(
            priority,
            60_000L  // 1분 간격
        ).apply {
            setMinUpdateDistanceMeters(100f)  // 100m 이상 이동 시에만
            setMinUpdateIntervalMillis(30_000L)  // 최소 30초 간격
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(LocationData(location.latitude, location.longitude))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    /**
     * 현재 위치 1회 조회 (항상 최신 위치 요청)
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) return null

        return try {
            val freshLocation = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
            freshLocation?.let { LocationData(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.contains("generic") ||
            android.os.Build.FINGERPRINT.contains("emulator") ||
            android.os.Build.MODEL.contains("Emulator") ||
            android.os.Build.MODEL.contains("sdk_gphone")
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Task를 suspend로 변환
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result) {}
            }
            addOnFailureListener {
                continuation.resume(null) {}
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
    }
}

/**
 * 위치 데이터
 */
data class LocationData(
    val lat: Double,
    val lng: Double
)
