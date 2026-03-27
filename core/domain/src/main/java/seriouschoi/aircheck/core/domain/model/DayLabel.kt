package seriouschoi.aircheck.core.domain.model

/**
 * 날짜 라벨 (오늘, 내일, 모레, 또는 날짜)
 * UI에서 localized 문자열로 변환
 */
sealed class DayLabel {
    data object Today : DayLabel()
    data object Tomorrow : DayLabel()
    data object DayAfterTomorrow : DayLabel()
    data class Date(val month: Int, val day: Int) : DayLabel()
}
