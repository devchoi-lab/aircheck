package seriouschoi.aircheck.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import seriouschoi.aircheck.core.domain.model.LocationItem
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.repository.SmartLocation
import seriouschoi.aircheck.core.domain.util.shortenBjdName
import javax.inject.Inject

/**
 * 위치 목록 조회 UseCase
 *
 * 현재 위치 + 저장된 위치들을 통합하여 반환한다.
 * 현재 위치(법정동 변경/30분 경과) 또는 저장 위치가 변경되면 자동으로 다시 조회된다.
 */
class GetLocationListUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val savedLocationRepository: SavedLocationRepository,
) {
    operator fun invoke(): Flow<List<LocationItem>> {
        val smartLocationFlow = locationProvider.smartLocationFlow()
            .onStart<SmartLocation?> { emit(locationProvider.getCurrentSmartLocation()) }
            .distinctUntilChanged()

        return combine(smartLocationFlow, savedLocationRepository.getAll()) { smartLocation, savedLocations ->
            buildList {
                // 1. 현재 위치
                if (smartLocation != null) {
                    add(
                        LocationItem(
                            name = shortenBjdName(smartLocation.bjdName),
                            isCurrentLocation = true,
                            lat = smartLocation.lat,
                            lng = smartLocation.lng,
                        )
                    )
                } else {
                    add(
                        LocationItem(
                            name = LocationItem.CURRENT_LOCATION_PLACEHOLDER,
                            isCurrentLocation = true,
                            lat = 0.0,
                            lng = 0.0,
                            needsPermission = !locationProvider.hasLocationPermission(),
                        )
                    )
                }

                // 2. 저장된 위치들 (raw 좌표 — centroid 변환은 FetchWeatherUseCase가 담당)
                for (saved in savedLocations) {
                    add(
                        LocationItem(
                            name = shortenBjdName(saved.displayName),
                            isCurrentLocation = false,
                            savedLocationId = saved.id,
                            lat = saved.lat,
                            lng = saved.lng,
                        )
                    )
                }
            }
        }
    }

}
