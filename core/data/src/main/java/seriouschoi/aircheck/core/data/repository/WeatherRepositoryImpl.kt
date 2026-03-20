@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.core.network.api.AircheckApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val api: AircheckApi
) : WeatherRepository {

    override suspend fun fetch(lat: Double, lng: Double): Result<WeatherData> {
        return try {
            val response = api.getWeather(lat, lng)
            WeatherParsingUtil.parseWeatherData(response.weather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
