package seriouschoi.aircheck.feature.weather.ui.weather

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData

/**
 * 관심사별 분리된 상태 정의
 */

/** 위치별 날씨 데이터 */
internal data class LocationWeatherData(
    val locationName: String,
    val isCurrentLocation: Boolean,
    /** 저장된 위치의 ID (현재 위치는 null) — 삭제 시 사용 */
    val savedLocationId: Long? = null,
    /** 날씨 조회용 좌표 */
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val airQuality: WeatherContract.DataResult<AirQuality> = WeatherContract.DataResult(),
    val weather: WeatherContract.DataResult<WeatherData> = WeatherContract.DataResult(),
    /** 위치 권한 필요 (현재 위치 전용) */
    val needsPermission: Boolean = false,
    /** 개별 새로고침 중 */
    val isRefreshing: Boolean = false
)

/** 위치 목록 상태 */
internal data class LocationsState(
    val locations: List<LocationWeatherData> = emptyList(),
    val isLoading: Boolean = true
)

/** UI 컨트롤 상태 (다이얼로그, 로딩 인디케이터 등) */
internal data class UiControlState(
    val isRefreshing: Boolean = false,
    val showInfoDialog: Boolean = false,
    val deleteConfirm: WeatherContract.DeleteConfirmState? = null,
    val needsLocationPermission: Boolean = false,
    val error: WeatherContract.ErrorState? = null
)

/** 화면 전체 통합 상태 (Screen에서 관찰) */
internal data class WeatherScreenState(
    val locations: LocationsState = LocationsState(),
    val uiControl: UiControlState = UiControlState()
) {
    /** 로딩 중 여부 (위치 로딩 또는 리프레시) */
    val isLoading: Boolean get() = locations.isLoading
    
    /** 리프레시 중 여부 */
    val isRefreshing: Boolean get() = uiControl.isRefreshing
    
    /** 현재 위치 데이터 (첫 번째) */
    val currentLocation: LocationWeatherData? get() = locations.locations.firstOrNull { it.isCurrentLocation }
}
