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
