package seriouschoi.aircheck.core.domain.model

/**
 * 위치 정보 (날씨 데이터 없음)
 *
 * 현재 위치의 좌표는 centroid 조정 완료 상태,
 * 저장 위치의 좌표는 raw 좌표 (centroid 변환은 FetchWeatherUseCase가 담당)
 */
data class LocationItem(
    val name: String,
    val isCurrentLocation: Boolean,
    val savedLocationId: Long? = null,
    val lat: Double,
    val lng: Double,
    val needsPermission: Boolean = false,
) {
    companion object {
        /**
         * 현재 위치 로딩/권한 없음 상태의 placeholder 이름
         * UI에서 이 값을 감지하여 적절한 localized 문자열로 대체해야 함
         */
        const val CURRENT_LOCATION_PLACEHOLDER = "CURRENT_LOCATION"
    }
}
