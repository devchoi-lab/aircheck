@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.network.api.AircheckApiClient
import seriouschoi.aircheck.core.network.api.AircheckWeatherResponse
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
    private val apiClient: AircheckApiClient
) : AirQualityRepository {

    override suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> = withContext(Dispatchers.IO) {
        try {
            val body = apiClient.getWeather(lat, lng)
            val response = apiClient.json.decodeFromString<AircheckWeatherResponse>(body)
            
            val air = response.airQuality
                ?: return@withContext Result.failure(Exception("대기질 데이터 없음"))
            
            // 밀도 → 등급 계산 (WHO 2021 기준)
            val mainStation = air.stationName
            
            val pm10Data = air.pm10?.let { value ->
                val isFromFallback = air.pm10Station != mainStation
                PmData(
                    value = value.toDouble(),
                    level = AqiLevel.fromPm10(value.toDouble()),
                    stationName = air.pm10Station,
                    isFromFallback = isFromFallback
                )
            }
            
            val pm25Data = air.pm25?.let { value ->
                val isFromFallback = air.pm25Station != mainStation
                PmData(
                    value = value.toDouble(),
                    level = AqiLevel.fromPm25(value.toDouble()),
                    stationName = air.pm25Station,
                    isFromFallback = isFromFallback
                )
            }
            
            // 최악 등급 계산
            val levels = listOfNotNull(pm25Data?.level, pm10Data?.level)
            val worstLevel = levels.maxByOrNull { it.ordinal } ?: AqiLevel.UNKNOWN
            
            Result.success(
                AirQuality(
                    aqi = 0,  // 서버에서 더 이상 제공 안 함
                    pm25 = pm25Data,
                    pm10 = pm10Data,
                    mainStationName = "${air.stationName} (${air.sidoName})",
                    updatedAt = air.dataTime,
                    level = worstLevel,
                    worstLevel = worstLevel
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
