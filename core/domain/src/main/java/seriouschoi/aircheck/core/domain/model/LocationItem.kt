package seriouschoi.aircheck.core.domain.model

/**
 * 위치 정보 (날씨 데이터 없음)
 *
 * 모든 위치는 법정동 코드(bjdCode)로 식별된다.
 * 날씨 조회는 bjdCode를 통해 centroid 좌표를 도출하여 수행된다.
 */
data class LocationItem(
    val name: String,
    val isCurrentLocation: Boolean,
    val bjdCode: String = "",
    val savedLocationId: Long? = null,
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
