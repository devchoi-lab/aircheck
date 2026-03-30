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
 * 법정동 코드를 기반으로 centroid 좌표를 조회한 뒤 API를 호출하여,
 * 같은 동네의 요청이 동일한 캐시 키를 사용하도록 보장한다.
 */
class FetchWeatherUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val weatherRepository: CombinedWeatherRepository
) {
    private val unavailableOutput = FetchWeatherOutput(
        smartLocation = null,
        result = CombinedWeatherResult(
            weather = Result.failure(DataError.LocationUnavailable),
            airQuality = Result.failure(DataError.LocationUnavailable)
        )
    )

    /**
     * 법정동 코드 기반 단일 위치 날씨 조회
     */
    suspend operator fun invoke(bjdCode: String): FetchWeatherOutput {
        val smartLocation = locationProvider.findByBjdCode(bjdCode)
            ?: return unavailableOutput

        val result = weatherRepository.fetch(smartLocation.lat, smartLocation.lng)
        return FetchWeatherOutput(smartLocation = smartLocation, result = result)
    }

    /**
     * 법정동 코드 기반 다중 위치 일괄 조회
     */
    suspend fun fetchBatch(bjdCodes: List<String>): List<FetchWeatherOutput> {
        val smartLocations = bjdCodes.map { locationProvider.findByBjdCode(it) }

        // 변환 성공한 것만 배치 요청
        val validEntries = smartLocations.mapIndexedNotNull { index, loc ->
            loc?.let { index to it }
        }

        val batchResults = if (validEntries.isNotEmpty()) {
            weatherRepository.fetchBatch(validEntries.map { it.second.lat to it.second.lng })
        } else {
            emptyList()
        }

        // 원래 순서대로 결과 매핑
        var batchIndex = 0
        return smartLocations.map { smartLocation ->
            if (smartLocation != null) {
                FetchWeatherOutput(
                    smartLocation = smartLocation,
                    result = batchResults[batchIndex++]
                )
            } else {
                unavailableOutput
            }
        }
    }
}

data class FetchWeatherOutput(
    val smartLocation: SmartLocation?,
    val result: CombinedWeatherResult
)
