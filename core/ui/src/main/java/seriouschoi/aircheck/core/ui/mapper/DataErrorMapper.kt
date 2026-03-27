package seriouschoi.aircheck.core.ui.mapper

import seriouschoi.aircheck.core.domain.model.DataError
import seriouschoi.aircheck.core.resource.R
import seriouschoi.aircheck.core.resource.UiText

/**
 * DataError → UiText 변환
 */
fun DataError.toUiText(): UiText = when (this) {
    is DataError.WeatherUnavailable -> UiText.StringResource(R.string.error_weather_data)
    is DataError.AirQualityUnavailable -> UiText.StringResource(R.string.error_air_quality_data)
    is DataError.LocationUnavailable -> UiText.StringResource(R.string.error_location_matching)
    is DataError.LocationPermissionDenied -> UiText.StringResource(R.string.permission_title)
    is DataError.DuplicateLocation -> UiText.StringResource(R.string.error_duplicate_location)
    is DataError.SavedLocationLimitExceeded -> UiText.StringResource(R.string.error_saved_location_limit, limit)
}

/**
 * Throwable → UiText 변환
 * DataError이면 변환, 아니면 DynamicString으로 메시지 반환
 */
fun Throwable.toUiText(): UiText = when (this) {
    is DataError -> this.toUiText()
    else -> UiText.DynamicString(message ?: "Unknown error")
}
