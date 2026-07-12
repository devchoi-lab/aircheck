package seriouschoi.aircheck.core.domain.usecase

import seriouschoi.aircheck.core.domain.model.CalendarEventWeatherSnapshot
import seriouschoi.aircheck.core.domain.model.ResolvedEventLocation
import seriouschoi.aircheck.core.domain.repository.CalendarEventRepository
import javax.inject.Inject

/**
 * 오늘의 캘린더 일정 + 각 일정에 대응되는 날씨/공기질 묶음을 만든다.
 *
 * 이전에는 이 orchestration 이 `CalendarViewModel.loadCalendarEvents` 안에
 * 들어 있었는데, 그건 도메인 로직이라 ViewModel 의 책임이 아니어서 도메인 use case 로 분리한다.
 *
 * 흐름:
 * 1. [CalendarEventRepository] 에서 오늘 일정을 읽는다.
 * 2. 각 일정에 대해 [ResolveEventLocationUseCase] 를 호출해 표시할 법정동을 결정한다.
 * 3. 결정된 bjdCode 들을 distinct 해 [FetchWeatherUseCase.fetchBatch] 로 일괄 조회한다.
 *    - 빈 코드(현재 위치도 못 얻은 경우) 는 제외한다.
 *    - 같은 bjdCode 의 일정이 여러 개여도 한 번만 호출된다 (N+1 회피).
 * 4. 일정 → (위치 + 날씨 + 공기질) 형태로 묶어 반환한다.
 *
 * 캘린더 권한이 없으면 [CalendarEventRepository] 가 빈 리스트를 반환하므로
 * 이 use case 도 빈 리스트를 반환한다.
 */
class LoadTodayCalendarEventsWithWeatherUseCase @Inject constructor(
    private val calendarEventRepository: CalendarEventRepository,
    private val resolveEventLocationUseCase: ResolveEventLocationUseCase,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
) {
    suspend operator fun invoke(): List<CalendarEventWeatherSnapshot> {
        // 1. 오늘 일정
        val events = calendarEventRepository.getTodayEvents()
        if (events.isEmpty()) return emptyList()

        // 2. 각 이벤트별 위치 결정
        val resolvedByEventId: Map<Long, ResolvedEventLocation> = events.associate { event ->
            event.id to resolveEventLocationUseCase(event.id, event.location)
        }

        // 3. distinct bjdCode 일괄 날씨 조회
        val distinctBjdCodes = resolvedByEventId.values
            .map { it.bjdCode }
            .filter { it.isNotEmpty() }
            .distinct()

        val weatherByBjdCode: Map<String, WeatherSnapshotPart> = if (distinctBjdCodes.isEmpty()) {
            emptyMap()
        } else {
            val outputs = fetchWeatherUseCase.fetchBatch(distinctBjdCodes)
            distinctBjdCodes.zip(outputs).associate { (code, output) ->
                code to WeatherSnapshotPart(
                    weather = output.result.weather.getOrNull(),
                    airQuality = output.result.airQuality.getOrNull(),
                )
            }
        }

        // 4. 일정별 묶음 조립
        return events.map { event ->
            val resolved = resolvedByEventId.getValue(event.id)
            val part = weatherByBjdCode[resolved.bjdCode]
            CalendarEventWeatherSnapshot(
                event = event,
                resolvedLocation = resolved,
                weatherData = part?.weather,
                airQuality = part?.airQuality,
            )
        }
    }

    /** bjdCode 별로 잠깐 들고 있는 weather + airQuality 짝. 외부 노출 안 함. */
    private data class WeatherSnapshotPart(
        val weather: seriouschoi.aircheck.core.domain.model.WeatherData?,
        val airQuality: seriouschoi.aircheck.core.domain.model.AirQuality?,
    )
}
