package seriouschoi.aircheck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 캘린더 일정(시스템 캘린더 eventId)과 사용자가 지정한 법정동(bjdCode)의 매핑.
 *
 * - 시스템 캘린더는 읽기 전용으로 다루므로, 일정의 위치 메타데이터는 이 테이블에만 저장한다.
 * - 매일 1회, 현재 시각 기준 48시간보다 오래된 매핑은 일괄 삭제한다 (#147 참고).
 *   절대 경계(예: "오늘 자정 UTC")를 쓰지 않는 이유는 사용자의 로컬 자정 / 다른 시간대 이동 시
 *   경계 계산이 어긋날 수 있기 때문이다.
 */
@Entity(tableName = "event_location_mapping")
data class EventLocationMappingEntity(
    /** 시스템 캘린더 eventId */
    @PrimaryKey val eventId: Long,

    /** 매핑된 법정동 코드 */
    val bjdCode: String,

    /** 표시용 법정동명 캐시 (예: "서울특별시 강남구 역삼동") */
    val bjdName: String,

    /**
     * 매핑이 만들어진 시점의 EVENT_LOCATION 원문.
     * 다른 캘린더 앱에서 일정의 장소가 수정되었는지 감지해 매핑을 무효화하는 데 사용한다.
     * 일정에 장소가 비어 있던 경우 null.
     */
    val locationTextSnapshot: String?,

    /** 일정 시작 시각 (UTC millis) — 청소 시 기준점 */
    val eventStartUtcMillis: Long,

    /** 매핑이 만들어지거나 갱신된 시각 (UTC millis) */
    val updatedAtUtcMillis: Long,
)
