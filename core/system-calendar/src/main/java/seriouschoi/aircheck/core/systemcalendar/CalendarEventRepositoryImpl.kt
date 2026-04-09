package seriouschoi.aircheck.core.systemcalendar

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import seriouschoi.aircheck.core.domain.model.CalendarEvent
import seriouschoi.aircheck.core.domain.repository.CalendarEventRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `CalendarEventRepository` 의 안드로이드 시스템 캘린더 어댑터.
 *
 * `ContentResolver` 와 `CalendarContract` 를 직접 호출해 사용자의 캘린더에서
 * 일정을 읽어와 도메인 모델 [CalendarEvent] 로 매핑한다. core/domain 레이어는
 * 이 클래스의 존재를 모르고, 포트 [CalendarEventRepository] 만 안다.
 *
 * 캘린더 권한이 없으면 `SecurityException` 이 발생하는데, 이 어댑터는 그것을
 * 잡아 빈 리스트로 변환한다 — 권한 처리는 UI 레이어 책임이다.
 */
@Singleton
class CalendarEventRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : CalendarEventRepository {

    override suspend fun getTodayEvents(): List<CalendarEvent> {
        // 시스템 캘린더의 DTSTART/DTEND 컬럼은 UTC epoch milliseconds 로 저장되어 있다.
        // SQL 비교를 같은 단위로 맞추기 위해 "오늘"의 경계도 epoch ms 로 환산해야 한다.
        //
        // 변환 흐름:
        //   LocalDate.now()                          → 사용자 기기 시간대의 "오늘" 날짜 (시각 정보 없음)
        //   .atStartOfDay(ZoneId.systemDefault())    → 그 날짜의 자정을 *기기 시간대* 에서 본 ZonedDateTime
        //                                              (예: KST 라면 2026-04-09T00:00:00+09:00)
        //   .toInstant()                             → 같은 순간을 절대 시각(Instant) 로 표현
        //                                              (UTC 기준; 위 예시는 2026-04-08T15:00:00Z)
        //   .toEpochMilli()                          → 1970-01-01T00:00:00Z 부터의 milliseconds (Long)
        //
        // 결과: startOfDay 는 "사용자의 로컬 자정" 이라는 절대 시점을 epoch ms 로 표현한 값.
        // endOfDay 는 그 다음 날 자정. 두 값으로 SQL DTSTART >= ? AND DTSTART < ? 비교 가능.
        //
        // 종일(all-day) 이벤트 주의: 안드로이드는 종일 이벤트의 DTSTART 를
        // "UTC 자정의 해당 캘린더 날짜" 로 저장한다(시간대 무관). 이 함수의 startOfDay/endOfDay 는
        // 로컬 자정 기반이라 종일 이벤트와의 경계가 시간대만큼 어긋날 수 있다. 현재 호출자
        // (CalendarViewModel) 가 isAllDay 분기로 결과를 보정하고 있어 동작은 맞지만,
        // 추후 종일 이벤트를 더 정확히 다루려면 이 부분의 시간대 처리 정책을 별도로 검토해야 한다.
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return queryEvents(startOfDay, endOfDay)
    }

    private fun queryEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val contentResolver: ContentResolver = context.contentResolver
        val cursor: Cursor? = try {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )
        } catch (e: SecurityException) {
            // 권한 없음 → 빈 리스트로 처리. 권한 요청은 UI 레이어 책임.
            null
        }

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
            val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
            val locationIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
            val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
            val allDayIndex = it.getColumnIndex(CalendarContract.Events.ALL_DAY)
            val calendarNameIndex = it.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val title = it.getString(titleIndex) ?: ""
                val location = it.getString(locationIndex)
                val rowStart = it.getLong(startIndex)
                val rowEnd = it.getLong(endIndex)
                val isAllDay = it.getInt(allDayIndex) == 1
                val calendarName = it.getString(calendarNameIndex)

                // DB 에 저장된 epoch ms 를 *기기 시간대* 의 LocalDateTime 으로 변환한다.
                // 일반(시간이 정해진) 이벤트는 이 변환이 정확히 사용자 시계와 맞는다.
                //
                // 종일 이벤트는 epoch ms 가 "UTC 자정" 을 가리키므로, 이 변환을 거치면
                // KST 사용자 기준으로 LocalDateTime 의 hour 가 0 이 아닌 9 가 되는 미묘한
                // 어긋남이 생긴다. 현재는 호출 측(CalendarViewModel)에서 isAllDay 분기로
                // hour 값을 무시해 우회 처리 중이다. 자세한 노트는 getTodayEvents() 의
                // 코멘트를 참고할 것.
                val startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(rowStart),
                    ZoneId.systemDefault(),
                )
                val endTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(rowEnd),
                    ZoneId.systemDefault(),
                )

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        location = location,
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = isAllDay,
                        calendarName = calendarName,
                    ),
                )
            }
        }

        return events
    }
}
