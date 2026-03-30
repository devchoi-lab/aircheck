package seriouschoi.aircheck.core.data.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
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

    /** 강제 갱신 시 즉시 위치를 emit하기 위한 트리거 */
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * 위치 업데이트 Flow
     *
     * 법정동이 바뀌거나 30분이 지나면 새 위치 emit.
     * forceRefresh() 호출 시 즉시 현재 위치를 가져와 emit.
     */
    fun smartLocationFlow(): Flow<SmartLocationResult> {
        val periodicFlow = locationDataSource.locationFlow()
            .mapNotNull { location ->
                bjdMatcher.findNearest(location.lat, location.lng)
            }
            .filter { bjdResult ->
                shouldFetch(bjdResult.code)
            }

        val refreshFlow = refreshTrigger
            .mapNotNull {
                val location = locationDataSource.getCurrentLocation() ?: return@mapNotNull null
                bjdMatcher.findNearest(location.lat, location.lng)
            }

        return merge(periodicFlow, refreshFlow)
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
     * 법정동 코드 → SmartLocationResult 조회
     */
    fun findByCode(bjdCode: String): SmartLocationResult? {
        val bjdResult = bjdMatcher.findByCode(bjdCode) ?: return null
        return SmartLocationResult(
            bjdCode = bjdResult.code,
            bjdName = bjdResult.name,
            centroidLat = bjdResult.centroidLat,
            centroidLng = bjdResult.centroidLng
        )
    }

    /**
     * 강제 갱신 (pull-to-refresh 등)
     * shouldFetch 필터를 초기화하고, 즉시 현재 위치를 가져와 emit
     */
    fun forceRefresh() {
        lastFetch = null
        cachedLocation = null
        refreshTrigger.tryEmit(Unit)
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
