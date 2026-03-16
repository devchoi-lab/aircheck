package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.WeatherData

/** 날씨 데이터 저장소 (Port) */
interface WeatherRepository {
    suspend fun fetch(lat: Double, lng: Double): Result<WeatherData>
}
