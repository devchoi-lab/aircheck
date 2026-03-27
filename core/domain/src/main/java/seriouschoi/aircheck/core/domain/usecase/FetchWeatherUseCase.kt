package seriouschoi.aircheck.core.domain.usecase

import seriouschoi.aircheck.core.domain.model.DataError
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherRepository
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SmartLocation
import javax.inject.Inject

/**
 * 날씨 조회 UseCase
 *
 * 모든 날씨 API 호출은 이 UseCase를 통해야 한다.
 * 좌표를 법정동 centroid로 변환한 뒤 API를 호출하여,
 * 같은 동네의 요청이 동일한 캐시 키를 사용하도록 보장한다.
 */
class FetchWeatherUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val weatherRepository: CombinedWeatherRepository
) {
    /**
     * 단일 위치 날씨 조회
     * @return centroid 변환된 SmartLocation + 날씨 결과
     */
    suspend operator fun invoke(lat: Double, lng: Double): FetchWeatherOutput {
        val centroid = locationProvider.toCentroid(lat, lng)
            ?: return FetchWeatherOutput(
                smartLocation = null,
                result = CombinedWeatherResult(
                    weather = Result.failure(DataError.LocationUnavailable),
                    airQuality = Result.failure(DataError.LocationUnavailable)
                )
            )

        val result = weatherRepository.fetch(centroid.lat, centroid.lng)
        return FetchWeatherOutput(smartLocation = centroid, result = result)
    }

    /**
     * 다중 위치 일괄 조회
     */
    suspend fun fetchBatch(locations: List<Pair<Double, Double>>): List<FetchWeatherOutput> {
        val centroids = locations.map { (lat, lng) -> locationProvider.toCentroid(lat, lng) }

        // centroid 변환 성공한 것만 배치 요청
        val validEntries = centroids.mapIndexedNotNull { index, centroid ->
            centroid?.let { index to it }
        }

        val batchResults = if (validEntries.isNotEmpty()) {
            weatherRepository.fetchBatch(validEntries.map { it.second.lat to it.second.lng })
        } else {
            emptyList()
        }

        // 원래 순서대로 결과 매핑
        var batchIndex = 0
        return centroids.map { centroid ->
            if (centroid != null) {
                FetchWeatherOutput(
                    smartLocation = centroid,
                    result = batchResults[batchIndex++]
                )
            } else {
                FetchWeatherOutput(
                    smartLocation = null,
                    result = CombinedWeatherResult(
                        weather = Result.failure(DataError.LocationUnavailable),
                        airQuality = Result.failure(DataError.LocationUnavailable)
                    )
                )
            }
        }
    }
}

data class FetchWeatherOutput(
    val smartLocation: SmartLocation?,
    val result: CombinedWeatherResult
)
