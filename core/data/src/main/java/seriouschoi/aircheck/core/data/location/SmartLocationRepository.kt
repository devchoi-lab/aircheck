package seriouschoi.aircheck.core.data.location

import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 스마트 위치 Repository
 * 
 * GPS 좌표 → 법정동 centroid 변환 + 30분 갱신 로직
 */
@Singleton
class SmartLocationRepository @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val bjdMatcher: BjdMatcher
) {
    companion object {
        private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L // 30분
        private const val LOCATION_CACHE_TTL_MS = 10 * 60 * 1000L // 10분
    }

    private var lastFetch: LastFetch? = null
    private var cachedLocation: CachedLocation? = null
    
    /**
     * 위치 업데이트 Flow
     * 
     * 법정동이 바뀌거나 30분이 지나면 새 위치 emit
     */
    fun smartLocationFlow(): Flow<SmartLocationResult> {
        return locationDataSource.locationFlow()
            .mapNotNull { location ->
                bjdMatcher.findNearest(location.lat, location.lng)
            }
            .filter { bjdResult ->
                shouldFetch(bjdResult.code)
            }
            .map { bjdResult ->
                SmartLocationResult(
                    bjdCode = bjdResult.code,
                    bjdName = bjdResult.name,
                    centroidLat = bjdResult.centroidLat,
                    centroidLng = bjdResult.centroidLng
                )
            }
    }
    
    /**
     * 현재 위치의 법정동 centroid 조회 (1회, 10분 캐싱)
     */
    suspend fun getCurrentSmartLocation(): SmartLocationResult? {
        cachedLocation?.let { cache ->
            if (System.currentTimeMillis() - cache.timestamp < LOCATION_CACHE_TTL_MS) {
                return cache.result
            }
        }

        val location = locationDataSource.getCurrentLocation() ?: return null
        val bjdResult = bjdMatcher.findNearest(location.lat, location.lng) ?: return null

        val result = SmartLocationResult(
            bjdCode = bjdResult.code,
            bjdName = bjdResult.name,
            centroidLat = bjdResult.centroidLat,
            centroidLng = bjdResult.centroidLng
        )
        cachedLocation = CachedLocation(result, System.currentTimeMillis())
        return result
    }
    
    /**
     * 갱신 조건 체크
     * - 법정동 변경됨 → true
     * - 30분 경과 → true
     * - 그 외 → false
     */
    private fun shouldFetch(bjdCode: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastFetch
        
        val shouldFetch = last == null ||
            last.bjdCode != bjdCode ||
            (now - last.timestamp) > REFRESH_INTERVAL_MS
        
        if (shouldFetch) {
            lastFetch = LastFetch(bjdCode, now)
        }
        
        return shouldFetch
    }
    
    /**
     * 좌표 → 법정동 centroid 변환
     */
    fun toCentroid(lat: Double, lng: Double): SmartLocationResult? {
        val bjdResult = bjdMatcher.findNearest(lat, lng) ?: return null
        return SmartLocationResult(
            bjdCode = bjdResult.code,
            bjdName = bjdResult.name,
            centroidLat = bjdResult.centroidLat,
            centroidLng = bjdResult.centroidLng
        )
    }

    /**
     * 강제 갱신 (pull-to-refresh 등)
     */
    fun forceRefresh() {
        lastFetch = null
    }
}

/**
 * 마지막 fetch 정보
 */
private data class LastFetch(
    val bjdCode: String,
    val timestamp: Long
)

/**
 * 위치 캐시
 */
private data class CachedLocation(
    val result: SmartLocationResult,
    val timestamp: Long
)

/**
 * 스마트 위치 결과
 */
data class SmartLocationResult(
    val bjdCode: String,      // 법정동 코드
    val bjdName: String,      // 법정동 이름 (예: 서울특별시 강남구 역삼동)
    val centroidLat: Double,  // centroid 위도
    val centroidLng: Double   // centroid 경도
)
