package seriouschoi.aircheck.feature.weather.ui.calendar

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getTodayEvents(): List<CalendarEvent> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        return getEvents(startOfDay, endOfDay)
    }
    
    private fun getEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
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
                sortOrder
            )
        } catch (e: SecurityException) {
            // Permission not granted
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
                val startMillis = it.getLong(startIndex)
                val endMillis = it.getLong(endIndex)
                val isAllDay = it.getInt(allDayIndex) == 1
                val calendarName = it.getString(calendarNameIndex)
                
                val startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startMillis),
                    ZoneId.systemDefault()
                )
                val endTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(endMillis),
                    ZoneId.systemDefault()
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
                    )
                )
            }
        }
        
        return events
    }
}
