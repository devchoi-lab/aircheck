package seriouschoi.aircheck.core.domain.model

/**
 * 데이터 조회 에러 타입
 * UI에서 localized 문자열로 변환
 */
sealed class DataError : Exception() {
    data object WeatherUnavailable : DataError()
    data object AirQualityUnavailable : DataError()
    data object LocationUnavailable : DataError()
    data object LocationPermissionDenied : DataError()
    data object DuplicateLocation : DataError()
    data class SavedLocationLimitExceeded(val limit: Int) : DataError()
}
