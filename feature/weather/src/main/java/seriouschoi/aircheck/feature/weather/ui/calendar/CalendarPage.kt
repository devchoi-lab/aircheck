package seriouschoi.aircheck.feature.weather.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.CalendarEvent
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.domain.model.ResolvedEventLocation
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.ui.mapper.toColor
import seriouschoi.aircheck.core.ui.mapper.toDisplayLabel
import seriouschoi.aircheck.core.ui.mapper.toEmoji
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.ui.theme.AirCheckPreview
import seriouschoi.aircheck.core.ui.theme.AirCheckPreviewWrapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarPage(
    state: CalendarContract.State,
    onIntent: (CalendarContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(CalendarContract.Intent.Refresh) },
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            !state.hasCalendarPermission || state.events.isEmpty() -> {
                EmptyCalendar(date = state.date)
            }
            else -> {
                CalendarContent(
                    date = state.date,
                    events = state.events,
                    currentLocationName = state.currentLocationName,
                    worstLevel = state.worstLevel,
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun EmptyCalendar(date: LocalDate) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
            text = stringResource(CoreR.string.calendar_empty_message),
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
    worstLevel: AqiLevel?,
    onIntent: (CalendarContract.Intent) -> Unit,
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
                text = stringResource(CoreR.string.calendar_current_location, currentLocationName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        // 공기질
        if (worstLevel != null && worstLevel != AqiLevel.UNKNOWN) {
            AirQualityBadge(worstLevel = worstLevel)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 일정 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(events) { eventWithWeather ->
                CalendarEventCard(
                    eventWithWeather = eventWithWeather,
                    onClick = { onIntent(CalendarContract.Intent.EditEvent(eventWithWeather.event)) },
                )
            }
        }
    }
}

@Composable
private fun AirQualityBadge(worstLevel: AqiLevel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        Text(
            text = worstLevel.toEmoji(),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(CoreR.string.air_quality_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(worstLevel.toLabelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = worstLevel.toColor(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationRow(
    recommendations: List<Recommendation>,
    maxPrecipitationHour: String? = null,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    val iconSize = (28 * fontScale).dp

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recommendations.forEach { recommendation ->
            val label = recommendation.toDisplayLabel()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(recommendation.toIconRes()),
                    contentDescription = label,
                    modifier = Modifier.size(iconSize),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                // 우산 아이콘 아래에 최대 강수확률 시간 표시
                if (recommendation is Recommendation.Umbrella && maxPrecipitationHour != null) {
                    Text(
                        text = maxPrecipitationHour,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEventCard(
    eventWithWeather: CalendarEventWithWeather,
    onClick: () -> Unit = {},
) {
    val event = eventWithWeather.event

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = formatEventTime(event, stringResource(CoreR.string.calendar_all_day)),
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
                event.location?.takeIf { it.isNotBlank() }?.let { locationText ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eventLocationLabel(
                        source = eventWithWeather.locationSource,
                        bjdName = eventWithWeather.locationDisplayName,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 오른쪽: 날씨 정보
            if (eventWithWeather.minFeelsLike != null) {
                WeatherInfo(eventWithWeather)
            }
        }

        // 추천 아이템 (일정 시간대 기준)
        if (eventWithWeather.recommendations.isNotEmpty()) {
            RecommendationRow(
                recommendations = eventWithWeather.recommendations,
                maxPrecipitationHour = eventWithWeather.maxPrecipitationHour,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun WeatherInfo(
    eventWithWeather: CalendarEventWithWeather,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 체감온도 범위
        val min = eventWithWeather.minFeelsLike
        val max = eventWithWeather.maxFeelsLike
        if (min != null && max != null) {
            Text(
                text = stringResource(CoreR.string.calendar_feels_like_range),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = if (min == max) "${min}°" else "${min}°~${max}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun eventLocationLabel(
    source: ResolvedEventLocation.Source,
    bjdName: String,
): String = when (source) {
    ResolvedEventLocation.Source.CURRENT_LOCATION ->
        stringResource(CoreR.string.calendar_event_location_label_current)
    ResolvedEventLocation.Source.MAPPING,
    ResolvedEventLocation.Source.INFERRED ->
        if (bjdName.isBlank()) {
            stringResource(CoreR.string.calendar_event_location_label_current)
        } else {
            stringResource(CoreR.string.calendar_event_location_label_resolved, bjdName)
        }
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    return date.format(formatter)
}

private fun formatEventTime(event: CalendarEvent, allDayLabel: String): String {
    return if (event.isAllDay) {
        allDayLabel
    } else {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
        "${event.startTime.format(formatter)} ~ ${event.endTime.format(formatter)}"
    }
}

// Previews

@AirCheckPreview
@Composable
private fun CalendarPagePreviewLoading() {
    AirCheckPreviewWrapper {
        CalendarPage(
            state = CalendarContract.State(isLoading = true),
            onIntent = {},
        )
    }
}

@AirCheckPreview
@Composable
private fun CalendarPagePreviewEmpty() {
    AirCheckPreviewWrapper {
        CalendarPage(
            state = CalendarContract.State(
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
            state = CalendarContract.State(
                isLoading = false,
                hasCalendarPermission = true,
                currentLocationName = "서울 강남구",
                worstLevel = AqiLevel.MODERATE,
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
                        minFeelsLike = 10,
                        maxFeelsLike = 12,
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
                        minFeelsLike = 16,
                        maxFeelsLike = 18,
                        hasPrecipitation = true,
                        maxPrecipitationProbability = 60,
                        maxPrecipitationHour = "13시",
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
                        minFeelsLike = 8,
                        maxFeelsLike = 19,
                        hasPrecipitation = true,
                        maxPrecipitationProbability = 40,
                        maxPrecipitationHour = "15시",
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
