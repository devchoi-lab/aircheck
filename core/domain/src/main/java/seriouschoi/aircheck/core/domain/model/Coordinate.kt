package seriouschoi.aircheck.core.domain.model

/**
 * 좌표 (위도, 경도)
 * 
 * Android 프레임워크 의존성 없이 순수 Kotlin으로 위치를 표현
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)
