package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.CalendarEvent

/**
 * 시스템 캘린더의 일정을 읽어오는 도메인 포트.
 *
 * 구현체는 `core/system-calendar` 모듈에 위치하며, 안드로이드의
 * `ContentResolver` 와 `CalendarContract` 를 사용해 사용자의 캘린더 데이터를 조회한다.
 *
 * core/domain 레이어에서는 이 포트만 알면 되므로 안드로이드 시스템 API 가 노출되지 않는다.
 */
interface CalendarEventRepository {

    /**
     * 오늘(로컬 기준) 의 일정 목록을 시작 시각 오름차순으로 반환한다.
     *
     * 캘린더 권한이 없으면 빈 리스트를 반환한다 (예외를 던지지 않는다).
     */
    suspend fun getTodayEvents(): List<CalendarEvent>
}
