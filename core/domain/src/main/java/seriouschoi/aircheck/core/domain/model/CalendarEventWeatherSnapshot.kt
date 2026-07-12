package seriouschoi.aircheck.core.domain.model

/**
 * 한 캘린더 일정에 대해 도메인 레이어가 모은 모든 날씨 컨텍스트의 묶음.
 *
 * - [event]                — 시스템 캘린더에서 읽어온 일정 본체
 * - [resolvedLocation]     — 이 일정에 대해 결정된 법정동 (매핑/유추/현재위치)
 * - [weatherData]          — 결정된 위치의 날씨 데이터. 못 가져왔으면 null
 * - [airQuality]           — 결정된 위치의 공기질. 못 가져왔으면 null
 *
 * UI 레이어는 이 스냅샷을 받아서 화면 표시용 모델로 변환한다.
 * 도메인 레이어는 이 스냅샷의 *생성* 까지만 책임지고, "어떤 시간대 forecast 를 뽑을지",
 * "어떤 추천 아이템을 보여줄지" 등은 후속 use case 가 담당한다 (Phase 3).
 */
data class CalendarEventWeatherSnapshot(
    val event: CalendarEvent,
    val resolvedLocation: ResolvedEventLocation,
    val weatherData: WeatherData?,
    val airQuality: AirQuality?,
)
