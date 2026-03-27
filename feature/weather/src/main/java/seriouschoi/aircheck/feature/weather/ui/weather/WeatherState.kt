package seriouschoi.aircheck.feature.weather.ui.weather

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData

/**
 * 관심사별 분리된 상태 정의
 */

/** 위치 정보 (UI 전용) */
internal data class UiLocationInfo(
    val name: String,
    val isCurrentLocation: Boolean,
    /** 저장된 위치의 ID (현재 위치는 null) — 삭제 시 사용 */
    val savedLocationId: Long? = null,
    /** 날씨 조회용 좌표 */
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    /** 위치 권한 필요 (현재 위치 전용) */
    val needsPermission: Boolean = false,
)

/** 위치별 날씨 데이터 */
internal data class UiLocationWeatherData(
    val locationInfo: UiLocationInfo,
    val airQuality: WeatherContract.DataResult<AirQuality> = WeatherContract.DataResult(),
    val weather: WeatherContract.DataResult<WeatherData> = WeatherContract.DataResult(),
    /** 개별 새로고침 중 */
    val isRefreshing: Boolean = false,
)

/** 위치 목록 상태 */
internal data class UiLocationsState(
    val locations: List<UiLocationWeatherData> = emptyList(),
    val isLoading: Boolean = true
)

/** UI 컨트롤 상태 (다이얼로그, 로딩 인디케이터 등) */
internal data class UiControlState(
    val showInfoDialog: Boolean = false,
)

/** 화면 전체 통합 상태 (Screen에서 관찰) */
internal data class UiWeatherScreenState(
    val locations: UiLocationsState = UiLocationsState(),
    val uiControl: UiControlState = UiControlState()
) {
    /** 로딩 중 여부 (위치 로딩 또는 리프레시) */
    val isLoading: Boolean get() = locations.isLoading
}
