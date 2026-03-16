package seriouschoi.aircheck.feature.weather.ui

import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.core.resource.UiText

/** 날씨 화면 UI 상태 */
internal sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(
        val airQuality: WeatherDataResult<AirQuality>,
        val weather: WeatherDataResult<WeatherData>
    ) : WeatherUiState
    data class Error(val message: UiText) : WeatherUiState
}

/** 날씨 데이터 결과 (성공/에러) */
internal data class WeatherDataResult<T>(
    val data: T? = null,
    val error: UiText? = null
)
