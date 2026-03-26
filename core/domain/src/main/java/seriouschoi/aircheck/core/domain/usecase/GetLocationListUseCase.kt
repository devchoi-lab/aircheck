@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package seriouschoi.aircheck.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import seriouschoi.aircheck.core.domain.model.LocationItem
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import javax.inject.Inject

/**
 * 위치 목록 조회 UseCase
 *
 * 현재 위치 + 저장된 위치들을 통합하여 반환한다.
 * 저장 위치가 변경되면 자동으로 다시 조회된다.
 */
class GetLocationListUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val savedLocationRepository: SavedLocationRepository,
) {
    operator fun invoke(): Flow<List<LocationItem>> {
        return savedLocationRepository.getAll()
            .flatMapLatest { savedLocations ->
                flow {
                    val locations = mutableListOf<LocationItem>()

                    // 1. 현재 위치
                    val smartLocation = locationProvider.getCurrentSmartLocation()
                    if (smartLocation != null) {
                        val parts = smartLocation.bjdName.split(" ")
                        val shortName = parts.takeLast(2).joinToString(" ").ifEmpty { "현재 위치" }
                        locations.add(
                            LocationItem(
                                name = shortName,
                                isCurrentLocation = true,
                                lat = smartLocation.lat,
                                lng = smartLocation.lng,
                            )
                        )
                    } else {
                        val needsPermission = !locationProvider.hasLocationPermission()
                        locations.add(
                            LocationItem(
                                name = "현재 위치",
                                isCurrentLocation = true,
                                lat = 0.0,
                                lng = 0.0,
                                needsPermission = needsPermission,
                            )
                        )
                    }

                    // 2. 저장된 위치들 (raw 좌표 — centroid 변환은 FetchWeatherUseCase가 담당)
                    for (saved in savedLocations) {
                        locations.add(
                            LocationItem(
                                name = saved.displayName,
                                isCurrentLocation = false,
                                savedLocationId = saved.id,
                                lat = saved.lat,
                                lng = saved.lng,
                            )
                        )
                    }

                    emit(locations)
                }
            }
    }
}
