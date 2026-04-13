package seriouschoi.aircheck.core.domain.model

import java.time.LocalDateTime

/**
 * 시스템 캘린더에서 읽어온 일정 한 건을 나타내는 도메인 모델.
 *
 * 안드로이드 시스템 캘린더(`CalendarContract`) 의 row 를 그대로 1:1 매핑한 것이 아니라,
 * 도메인 레이어가 사용할 형태로 정제된 결과다. 시스템 캘린더 API 호출은
 * `core/system-calendar` 모듈의 어댑터(`CalendarEventRepository` 구현체) 가 담당한다.
 */
data class CalendarEvent(
    /** 시스템 캘린더 eventId. 매핑/캐시/식별의 1차 키. */
    val id: Long,
    val title: String,
    /** EVENT_LOCATION 원문. 비어 있을 수 있다. */
    val location: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean,
    val calendarName: String? = null,
)
