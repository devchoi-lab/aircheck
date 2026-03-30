package seriouschoi.aircheck.feature.weather.ui.weather

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.domain.model.WeatherData
import java.time.Instant

/**
 * 관심사별 분리된 상태 정의
 */

/** 위치 정보 (UI 전용) */
internal data class UiLocationInfo(
    val name: String,
    val isCurrentLocation: Boolean,

    /** 법정동 코드 — 날씨 Map 키 및 조회에 사용 */
    val bjdCode: String = "",

    /** 위치 권한 필요 (현재 위치 전용) */
    val needsPermission: Boolean = false,
)

/** 위치별 날씨 결과 */
internal data class UiWeatherResult(
    val airQuality: WeatherContract.DataResult<AirQuality> = WeatherContract.DataResult(),
    val weather: WeatherContract.DataResult<WeatherData> = WeatherContract.DataResult(),
    /** 추천 아이템 (의류, 우산, 적설, 마스크) */
    val recommendations: List<Recommendation> = emptyList(),
    /** 개별 새로고침 중 */
    val isRefreshing: Boolean = false,
    /** 마지막 새로고침 시각 */
    val lastRefreshedAt: Instant? = null,
)

/** UI 컨트롤 상태 (다이얼로그, 로딩 인디케이터 등) */
internal data class UiControlState(
    val showInfoDialog: Boolean = false,
)

/** 화면 전체 통합 상태 (Screen에서 관찰) */
internal data class UiWeatherScreenState(
    val locations: List<UiLocationInfo> = emptyList(),
    val weathers: Map<String, UiWeatherResult> = emptyMap(),
    val isLoading: Boolean = true,
    val uiControl: UiControlState = UiControlState(),
)