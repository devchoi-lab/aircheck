package seriouschoi.aircheck.core.domain.repository

/**
 * 캘린더 일정(`eventId`) ↔ 사용자 지정 법정동(`bjdCode`) 매핑 저장소.
 *
 * core/data에서 Room DAO를 감싸 구현된다. core/domain 레이어에서는
 * 이 포트만 알면 되므로 Room/DB 세부사항이 노출되지 않는다.
 */
interface EventLocationMappingRepository {

    /** eventId 단건 조회. 없으면 null. */
    suspend fun findByEventId(eventId: Long): EventLocationMapping?

    /** 매핑 upsert. 같은 eventId가 있으면 덮어쓴다. */
    suspend fun upsert(mapping: EventLocationMapping)

    /** eventId로 매핑 삭제. */
    suspend fun deleteByEventId(eventId: Long)

    /**
     * 일정 종료 시각이 [thresholdUtcMillis]보다 오래된 매핑을 모두 삭제한다.
     * 호출자는 보통 `now - 48h` 같은 상대 시각을 넘긴다.
     * @return 삭제된 행 수
     */
    suspend fun deleteOlderThan(thresholdUtcMillis: Long): Int
}

/** 도메인 모델 (DB 엔티티의 복제) */
data class EventLocationMapping(
    val eventId: Long,
    val bjdCode: String,
    val bjdName: String,
    val locationTextSnapshot: String?,
    val eventEndUtcMillis: Long,
    val updatedAtUtcMillis: Long,
)
