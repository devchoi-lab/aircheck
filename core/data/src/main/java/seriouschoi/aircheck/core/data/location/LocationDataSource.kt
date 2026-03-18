package seriouschoi.aircheck.core.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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
    fun locationFlow(): Flow<LocationData> = callbackFlow {
        // 권한 체크
        if (!hasLocationPermission()) {
            close(SecurityException("위치 권한이 없습니다"))
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
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
     * 현재 위치 1회 조회
     */
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) return null
        
        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let { LocationData(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
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
