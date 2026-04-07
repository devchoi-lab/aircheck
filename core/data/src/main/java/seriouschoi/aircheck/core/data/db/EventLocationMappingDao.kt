package seriouschoi.aircheck.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventLocationMappingDao {

    /** eventId 단건 조회. 없으면 null. */
    @Query("SELECT * FROM event_location_mapping WHERE eventId = :eventId")
    suspend fun findByEventId(eventId: Long): EventLocationMappingEntity?

    /** 여러 eventId를 한 번에 조회 (캘린더 화면에서 다건 일정 로드 시 사용). */
    @Query("SELECT * FROM event_location_mapping WHERE eventId IN (:eventIds)")
    suspend fun findByEventIds(eventIds: List<Long>): List<EventLocationMappingEntity>

    /** 매핑 upsert. PrimaryKey 충돌 시 덮어쓴다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: EventLocationMappingEntity)

    /** eventId로 매핑 삭제. */
    @Query("DELETE FROM event_location_mapping WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: Long)

    /**
     * 청소 쿼리. 일정 시작 시각이 [thresholdUtcMillis]보다 오래된 매핑을 모두 삭제한다.
     * 호출자는 보통 `now - 48h`를 넘긴다.
     */
    @Query("DELETE FROM event_location_mapping WHERE eventStartUtcMillis < :thresholdUtcMillis")
    suspend fun deleteOlderThan(thresholdUtcMillis: Long): Int
}
