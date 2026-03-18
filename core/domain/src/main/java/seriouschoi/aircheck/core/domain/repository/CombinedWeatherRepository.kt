package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData

/**
 * 날씨 + 대기질 통합 조회 Repository
 * 
 * 서버 API를 1회만 호출하여 두 데이터를 함께 반환
 */
interface CombinedWeatherRepository {
    suspend fun fetch(lat: Double, lng: Double): CombinedWeatherResult
}

data class CombinedWeatherResult(
    val weather: Result<WeatherData>,
    val airQuality: Result<AirQuality>
)
