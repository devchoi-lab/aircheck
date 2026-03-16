package seriouschoi.aircheck.core.domain.model

/** PM 데이터 (값 + 출처 측정소) */
data class PmData(
    val value: Double,
    val level: AqiLevel,
    val stationName: String,
    val isFromFallback: Boolean
)

data class AirQuality(
    val aqi: Int,
    val pm25: PmData?,
    val pm10: PmData?,
    val mainStationName: String,
    val updatedAt: String,
    val level: AqiLevel,
    val worstLevel: AqiLevel
)

enum class AqiLevel(val label: String, val emoji: String, val colorHex: Long) {
    BEST("최고", "🌟", 0xFF00E5FF),
    GOOD("좋음", "😊", 0xFF00C8A0),
    FAIR("양호", "🙂", 0xFF8BC34A),
    MODERATE("보통", "😐", 0xFFFFD700),
    BAD("나쁨", "😮", 0xFFFF9800),
    QUITE_BAD("상당히 나쁨", "😷", 0xFFFF5722),
    VERY_BAD("매우 나쁨", "🤢", 0xFF9400D3),
    WORST("최악", "☠️", 0xFF7E0023),
    UNKNOWN("알 수 없음", "❓", 0xFF9E9E9E);

    companion object {
        fun fromKhaiGrade(grade: String): AqiLevel = when (grade) {
            "1" -> GOOD; "2" -> MODERATE; "3" -> BAD; "4" -> VERY_BAD
            else -> UNKNOWN
        }
        
        fun fromPm25(v: Double): AqiLevel = when {
            v <= 5.0 -> BEST
            v <= 9.0 -> GOOD
            v <= 12.0 -> FAIR
            v <= 15.0 -> MODERATE
            v <= 37.0 -> BAD
            v <= 50.0 -> QUITE_BAD
            v <= 75.0 -> VERY_BAD
            else -> WORST
        }
        
        fun fromPm10(v: Double): AqiLevel = when {
            v <= 10.0 -> BEST
            v <= 22.0 -> GOOD
            v <= 35.0 -> FAIR
            v <= 45.0 -> MODERATE
            v <= 75.0 -> BAD
            v <= 100.0 -> QUITE_BAD
            v <= 150.0 -> VERY_BAD
            else -> WORST
        }
    }
}
