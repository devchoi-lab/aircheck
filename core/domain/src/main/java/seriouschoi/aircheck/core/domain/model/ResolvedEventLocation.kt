package seriouschoi.aircheck.core.domain.model

/**
 * 캘린더 일정에 대해 결정된 날씨 조회용 위치.
 *
 * 우선순위: 사용자 매핑(MAPPING) → 텍스트 자동 유추(INFERRED) → 현재위치(CURRENT_LOCATION)
 *
 * @see seriouschoi.aircheck.core.domain.usecase.ResolveEventLocationUseCase
 */
data class ResolvedEventLocation(
    val bjdCode: String,
    val bjdName: String,
    val source: Source,
) {
    enum class Source {
        /** 사용자가 직접 지정한 매핑이 DB에 있고, 일정의 장소 텍스트도 변하지 않은 경우 */
        MAPPING,

        /** 일정의 장소 텍스트로부터 법정동을 자동 추론한 경우 */
        INFERRED,

        /** 매핑도 자동 유추도 실패해 현재위치로 대체된 경우 */
        CURRENT_LOCATION,
    }
}
