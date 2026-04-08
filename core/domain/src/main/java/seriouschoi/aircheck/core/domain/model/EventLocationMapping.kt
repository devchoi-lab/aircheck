package seriouschoi.aircheck.core.domain.model

/**
 * 캘린더 일정(`eventId`) 와 사용자가 지정한 법정동(`bjdCode`) 의 도메인 매핑.
 *
 * `EventLocationMappingRepository` 의 입출력 타입으로 사용되며,
 * core/data 의 Room 엔티티(`EventLocationMappingEntity`) 와는 별도로 유지된다.
 * core/domain 레이어에서 Room/DB 세부사항을 노출하지 않기 위함이다.
 */
data class EventLocationMapping(
    val eventId: Long,
    val bjdCode: String,
    val bjdName: String,
    val locationTextSnapshot: String?,
    val eventEndUtcMillis: Long,
    val updatedAtUtcMillis: Long,
)
