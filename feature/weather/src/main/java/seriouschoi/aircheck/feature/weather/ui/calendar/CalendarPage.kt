package seriouschoi.aircheck.feature.weather.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import seriouschoi.aircheck.core.ui.theme.AirCheckPreview
import seriouschoi.aircheck.core.ui.theme.AirCheckPreviewWrapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarPage(
    state: CalendarUiState,
    onIntent: (CalendarIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            !state.hasCalendarPermission -> {
                PermissionRequired(
                    onRequestPermission = { onIntent(CalendarIntent.RequestPermission) }
                )
            }
            state.events.isEmpty() -> {
                EmptyCalendar(date = state.date)
            }
            else -> {
                CalendarContent(
                    date = state.date,
                    events = state.events,
                    currentLocationName = state.currentLocationName,
                )
            }
        }
    }
}

@Composable
private fun PermissionRequired(
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "캘린더 권한이 필요합니다",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "일정에 맞는 날씨 정보를 보여드려요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("권한 허용하기")
        }
    }
}

@Composable
private fun EmptyCalendar(date: LocalDate) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = formatDate(date),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "오늘 일정이 없습니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun CalendarContent(
    date: LocalDate,
    events: List<CalendarEventWithWeather>,
    currentLocationName: String,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 날짜 헤더
        Text(
            text = formatDate(date),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        
        // 현재 위치 표시
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "현재 위치 기준: $currentLocationName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        
        // 일정 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(events) { eventWithWeather ->
                CalendarEventCard(eventWithWeather = eventWithWeather)
            }
        }
    }
}

@Composable
private fun CalendarEventCard(
    eventWithWeather: CalendarEventWithWeather,
) {
    val event = eventWithWeather.event
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 시간 + 제목
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = formatEventTime(event),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 오른쪽: 날씨 정보
            if (eventWithWeather.temperature != null) {
                WeatherInfo(
                    temperature = eventWithWeather.temperature,
                    weatherIcon = eventWithWeather.weatherIcon,
                    airQualityLabel = eventWithWeather.airQualityLabel,
                )
            }
        }
    }
}

@Composable
private fun WeatherInfo(
    temperature: Int,
    weatherIcon: String?,
    airQualityLabel: String?,
) {
    Column(
        horizontalAlignment = Alignment.End,
    ) {
        // 온도
        Text(
            text = "${temperature}°",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        
        // 미세먼지 (있으면)
        if (airQualityLabel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = airQualityLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    return date.format(formatter)
}

private fun formatEventTime(event: CalendarEvent): String {
    return if (event.isAllDay) {
        "하루 종일"
    } else {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
        event.startTime.format(formatter)
    }
}

// Previews

@AirCheckPreview
@Composable
private fun CalendarPagePreviewLoading() {
    AirCheckPreviewWrapper {
        CalendarPage(
            state = CalendarUiState(isLoading = true),
            onIntent = {},
        )
    }
}

@AirCheckPreview
@Composable
private fun CalendarPagePreviewNoPermission() {
    AirCheckPreviewWrapper {
        CalendarPage(
            state = CalendarUiState(
                isLoading = false,
                hasCalendarPermission = false,
            ),
            onIntent = {},
        )
    }
}

@AirCheckPreview
@Composable
private fun CalendarPagePreviewEmpty() {
    AirCheckPreviewWrapper {
        CalendarPage(
            state = CalendarUiState(
                isLoading = false,
                hasCalendarPermission = true,
                events = emptyList(),
            ),
            onIntent = {},
        )
    }
}

@AirCheckPreview
@Composable
private fun CalendarPagePreviewWithEvents() {
    AirCheckPreviewWrapper {
        CalendarPage(
            state = CalendarUiState(
                isLoading = false,
                hasCalendarPermission = true,
                currentLocationName = "서울 강남구",
                events = listOf(
                    CalendarEventWithWeather(
                        event = CalendarEvent(
                            id = 1,
                            title = "치과 예약",
                            location = "서울 강남구 역삼동",
                            startTime = LocalDateTime.of(2024, 4, 2, 9, 0),
                            endTime = LocalDateTime.of(2024, 4, 2, 10, 0),
                            isAllDay = false,
                        ),
                        temperature = 12,
                        weatherIcon = "CLEAR_DAY",
                        airQualityLevel = 1,
                        airQualityLabel = "좋음",
                    ),
                    CalendarEventWithWeather(
                        event = CalendarEvent(
                            id = 2,
                            title = "점심 약속",
                            location = null,
                            startTime = LocalDateTime.of(2024, 4, 2, 12, 30),
                            endTime = LocalDateTime.of(2024, 4, 2, 13, 30),
                            isAllDay = false,
                        ),
                        temperature = 18,
                        weatherIcon = "PARTLY_CLOUDY_DAY",
                        airQualityLevel = 2,
                        airQualityLabel = "보통",
                    ),
                    CalendarEventWithWeather(
                        event = CalendarEvent(
                            id = 3,
                            title = "엄마 생신",
                            location = null,
                            startTime = LocalDateTime.of(2024, 4, 2, 0, 0),
                            endTime = LocalDateTime.of(2024, 4, 3, 0, 0),
                            isAllDay = true,
                        ),
                        temperature = 15,
                        weatherIcon = "CLOUDY",
                        airQualityLevel = 2,
                        airQualityLabel = "보통",
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
