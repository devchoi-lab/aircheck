package seriouschoi.aircheck.core.domain.model

/**
 * 한 캘린더 일정에 대해 도메인 레이어가 *계산* 한 결과.
 *
 * [CalendarEventWeatherSnapshot] 가 \"원본 데이터를 모은 묶음\" 이라면,
 * 이 클래스는 그 묶음에서 사용자에게 의미 있는 인사이트(체감온도 범위,
 * 강수 정보, 추천 아이템 등) 를 추출한 결과다.
 *
 * UI 레이어는 이 클래스를 받아 화면 표시용 모델로 매핑한다 (Phase 4 에서 분리).
 *
 * @see seriouschoi.aircheck.core.domain.usecase.ComputeCalendarEventWeatherInsightUseCase
 */
data class CalendarEventWeatherInsight(
    val event: CalendarEvent,
    val resolvedLocation: ResolvedEventLocation,

    /** 일정 시간대 안의 forecast 들 중 최저 체감온도 (°C, 정수). 데이터 없으면 null. */
    val minFeelsLike: Int?,

    /** 일정 시간대 안의 forecast 들 중 최고 체감온도 (°C, 정수). 데이터 없으면 null. */
    val maxFeelsLike: Int?,

    /** 일정 시간대에 비/눈 예보가 하나라도 있는지. */
    val hasPrecipitation: Boolean,

    /** 일정 시간대 강수확률 중 최댓값. 비 예보 없으면 null. */
    val maxPrecipitationProbability: Int?,

    /** 위 최댓값이 발생한 시간 (예보 hour 라벨). 비 예보 없으면 null. */
    val maxPrecipitationHour: String?,

    /** 의상/우산/눈/마스크 등 사용자에게 보여줄 추천 아이템들. */
    val recommendations: List<Recommendation>,
)
