package seriouschoi.aircheck.core.systemcalendar

import android.content.ContentUris
import android.content.Context
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
 * `ContentResolver` 와 `CalendarContract.Instances` 를 사용해 사용자의 캘린더에서
 * 일정을 읽어와 도메인 모델 [CalendarEvent] 로 매핑한다. core/domain 레이어는
 * 이 클래스의 존재를 모르고, 포트 [CalendarEventRepository] 만 안다.
 *
 * `Instances` 테이블은 `Events` 의 원본 정의를 시간축 위에 펼친 가상 테이블로,
 * 반복 이벤트 전개·종일(all-day) 이벤트 시간대 보정·시간 범위 필터링을
 * CalendarProvider 가 내부적으로 처리해 준다.
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

        return queryInstances(startOfDay, endOfDay)
    }

    private fun queryInstances(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

        // Instances 테이블은 URI 에 시간 범위를 인코딩하여 조회한다.
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val contentResolver = context.contentResolver
        val cursor = try {
            contentResolver.query(builder.build(), projection, null, null, sortOrder)
        } catch (e: SecurityException) {
            null
        }

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val locationIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
            val startIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
            val allDayIndex = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val calendarNameIndex = it.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val title = it.getString(titleIndex) ?: ""
                val location = it.getString(locationIndex)
                val rowStart = it.getLong(startIndex)
                val rowEnd = it.getLong(endIndex)
                val isAllDay = it.getInt(allDayIndex) == 1
                val calendarName = it.getString(calendarNameIndex)

                // Instances 의 BEGIN/END 는 시간대가 보정된 UTC epoch ms 이므로,
                // 기기 시간대로 변환하면 종일 이벤트도 자정(hour=0)이 정확히 나온다.
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
