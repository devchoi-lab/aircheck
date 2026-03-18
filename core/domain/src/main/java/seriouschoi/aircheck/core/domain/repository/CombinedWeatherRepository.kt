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
    
    /**
     * 다중 위치 일괄 조회
     * @param locations (lat, lng) 쌍 목록
     * @return 위치별 결과 목록 (입력 순서 유지)
     */
    suspend fun fetchBatch(locations: List<Pair<Double, Double>>): List<CombinedWeatherResult>
}

data class CombinedWeatherResult(
    val weather: Result<WeatherData>,
    val airQuality: Result<AirQuality>
)
