@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.network.api.AircheckApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 API를 통해 대기질 정보 조회
 *
 * 서버는 밀도(μg/m³)만 반환하고,
 * 앱에서 WHO 2021 기준으로 등급 계산
 */
@Singleton
class AirQualityRepositoryImpl @Inject constructor(
    private val api: AircheckApi
) : AirQualityRepository {

    override suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> {
        return try {
            val response = api.getWeather(lat, lng)
            WeatherParsingUtil.parseAirQualityData(response.airQuality)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
