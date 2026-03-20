package seriouschoi.aircheck.core.domain.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Haversine 공식으로 두 좌표 간 거리 (km) 계산
 */
fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0 // 지구 반지름 (km)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
