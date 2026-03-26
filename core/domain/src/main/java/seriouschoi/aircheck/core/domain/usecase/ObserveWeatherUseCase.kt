@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package seriouschoi.aircheck.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherRepository
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치별 날씨 데이터
 */
data class LocationWeather(
    val locationName: String,
    val isCurrentLocation: Boolean,
    val savedLocationId: Long? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val weather: Result<WeatherData>,
    val airQuality: Result<AirQuality>,
    val needsPermission: Boolean = false
)

/**
 * 전체 날씨 데이터 (모든 위치 포함)
 */
data class WeatherState(
    val locations: List<LocationWeather> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * 날씨 데이터를 Flow로 관찰하는 UseCase
 * 
 * - 저장된 위치가 변경되면 자동으로 다시 로드
 * - 현재 위치 + 저장된 위치들의 날씨를 통합 제공
 */
@Singleton
class ObserveWeatherUseCase @Inject constructor(
    private val weatherRepository: CombinedWeatherRepository,
    private val locationProvider: LocationProvider,
    private val savedLocationRepository: SavedLocationRepository
) {
    /**
     * 모든 위치의 날씨를 Flow로 반환
     * savedLocationRepository 변경 시 자동 재조회
     */
    operator fun invoke(): Flow<WeatherState> {
        return savedLocationRepository.getAll()
            .flatMapLatest { savedLocations ->
                flow {
                    emit(WeatherState(isLoading = true))
                    
                    val locations = mutableListOf<LocationWeather>()
                    
                    // 1. 현재 위치
                    val smartLocation = locationProvider.getCurrentSmartLocation()
                    val needsPermission = !locationProvider.hasLocationPermission()
                    
                    if (smartLocation != null) {
                        // 법정동 하위 2단계만 표시 (예: "강남구 역삼동")
                        val parts = smartLocation.bjdName.split(" ")
                        val shortName = parts.takeLast(2).joinToString(" ").ifEmpty { "현재 위치" }
                        val centroid = locationProvider.toCentroid(smartLocation.lat, smartLocation.lng)
                        val result = centroid?.let { weatherRepository.fetch(it.lat, it.lng) }
                            ?: CombinedWeatherResult(
                                weather = Result.failure(Exception("현재 위치를 확인할 수 없어요")),
                                airQuality = Result.failure(Exception("현재 위치를 확인할 수 없어요"))
                            )
                        locations.add(
                            LocationWeather(
                                locationName = shortName,
                                isCurrentLocation = true,
                                lat = smartLocation.lat,
                                lng = smartLocation.lng,
                                weather = result.weather,
                                airQuality = result.airQuality
                            )
                        )
                    } else {
                        locations.add(
                            LocationWeather(
                                locationName = "현재 위치",
                                isCurrentLocation = true,
                                weather = Result.failure(Exception("위치를 가져올 수 없습니다")),
                                airQuality = Result.failure(Exception("위치를 가져올 수 없습니다")),
                                needsPermission = needsPermission
                            )
                        )
                    }
                    
                    // 2. 저장된 위치들 (개별 로드 - 캐시 히트 시 빠른 응답)
                    for (saved in savedLocations) {
                        val centroid = locationProvider.toCentroid(saved.lat, saved.lng)
                        val result = centroid?.let { weatherRepository.fetch(it.lat, it.lng) }
                            ?: CombinedWeatherResult(
                                weather = Result.failure(Exception("현재 위치를 확인할 수 없어요")),
                                airQuality = Result.failure(Exception("현재 위치를 확인할 수 없어요"))
                            )
                        locations.add(
                            LocationWeather(
                                locationName = saved.displayName,
                                isCurrentLocation = false,
                                savedLocationId = saved.id,
                                lat = saved.lat,
                                lng = saved.lng,
                                weather = result.weather,
                                airQuality = result.airQuality
                            )
                        )
                    }
                    
                    emit(WeatherState(locations = locations, isLoading = false))
                }
            }
    }
    
    /**
     * 강제 새로고침 트리거용 Flow
     */
    fun refresh(): Flow<WeatherState> = invoke()
}
