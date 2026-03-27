package seriouschoi.aircheck.core.domain.repository

import kotlinx.coroutines.flow.Flow
import seriouschoi.aircheck.core.domain.model.Coordinate

/** 위치 정보 제공자 (Port) */
interface LocationProvider {
    suspend fun getCurrentLocation(): Coordinate?
    
    /**
     * 스마트 위치 (법정동 centroid 기반)
     * 법정동 변경 또는 30분 경과 시에만 emit
     */
    fun smartLocationFlow(): Flow<SmartLocation>
    
    /**
     * 현재 위치의 스마트 위치 조회 (1회)
     */
    suspend fun getCurrentSmartLocation(): SmartLocation?
    
    /**
     * 좌표 → 법정동 centroid 변환
     * 임의의 좌표를 가장 가까운 법정동의 대표 좌표로 변환
     */
    suspend fun toCentroid(lat: Double, lng: Double): SmartLocation?

    /**
     * 강제 갱신 (pull-to-refresh)
     */
    fun forceRefresh()
    
    /**
     * 위치 권한이 있는지 확인
     */
    fun hasLocationPermission(): Boolean
}

/**
 * 스마트 위치 (법정동 centroid)
 */
data class SmartLocation(
    val bjdCode: String,      // 법정동 코드
    val bjdName: String,      // 법정동 이름
    val lat: Double,          // centroid 위도
    val lng: Double           // centroid 경도
)
