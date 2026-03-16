package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.AirQuality

/** 공기질 데이터 저장소 (Port) */
interface AirQualityRepository {
    suspend fun fetch(lat: Double, lng: Double): Result<AirQuality>
}
