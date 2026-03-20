@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.domain.repository.CombinedWeatherRepository
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
import seriouschoi.aircheck.core.network.api.AircheckApi
import seriouschoi.aircheck.core.network.api.BatchWeatherRequest
import seriouschoi.aircheck.core.network.api.LocationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedWeatherRepositoryImpl @Inject constructor(
    private val api: AircheckApi
) : CombinedWeatherRepository {

    override suspend fun fetch(lat: Double, lng: Double): CombinedWeatherResult {
        return try {
            val response = api.getWeather(lat, lng)

            CombinedWeatherResult(
                weather = WeatherParsingUtil.parseWeatherData(response.weather),
                airQuality = WeatherParsingUtil.parseAirQualityData(response.airQuality)
            )
        } catch (e: Exception) {
            CombinedWeatherResult(
                weather = Result.failure(e),
                airQuality = Result.failure(e)
            )
        }
    }

    override suspend fun fetchBatch(locations: List<Pair<Double, Double>>): List<CombinedWeatherResult> {
        if (locations.isEmpty()) return emptyList()

        return try {
            val request = BatchWeatherRequest(
                locations = locations.map { (lat, lng) -> LocationRequest(lat, lng) }
            )
            val items = api.getBatchWeather(request)

            items.map { item ->
                CombinedWeatherResult(
                    weather = WeatherParsingUtil.parseWeatherData(item.weather),
                    airQuality = WeatherParsingUtil.parseAirQualityData(item.airQuality)
                )
            }
        } catch (e: Exception) {
            // 배치 실패 시 개별 호출로 fallback
            locations.map { (lat, lng) -> fetch(lat, lng) }
        }
    }
}
