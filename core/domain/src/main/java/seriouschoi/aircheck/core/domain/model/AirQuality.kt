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

/**
 * WHO Global Air Quality Guidelines 2021 기준
 * ISBN: 978-92-4-003422-8
 * 
 * PM2.5 (24시간): AQG 15, IT-4 25, IT-3 37.5, IT-2 50, IT-1 75
 * PM10 (24시간): AQG 45, IT-4 50, IT-3 75, IT-2 100, IT-1 150
 */
enum class AqiLevel(val label: String, val emoji: String, val colorHex: Long) {
    GOOD("좋음", "😊", 0xFF00C853),           // AQG 이하
    MODERATE("보통", "🙂", 0xFFFFD600),       // IT-4 이하
    UNHEALTHY_SENSITIVE("민감군 주의", "😐", 0xFFFF9100), // IT-3 이하
    UNHEALTHY("나쁨", "😷", 0xFFFF5722),      // IT-2 이하
    VERY_UNHEALTHY("매우 나쁨", "🤢", 0xFF9C27B0), // IT-1 이하
    HAZARDOUS("위험", "☠️", 0xFF7E0023),      // IT-1 초과
    UNKNOWN("알 수 없음", "❓", 0xFF9E9E9E);

    companion object {
        /** 에어코리아 통합대기환경지수(KHAI) 등급 */
        fun fromKhaiGrade(grade: String): AqiLevel = when (grade) {
            "1" -> GOOD
            "2" -> MODERATE
            "3" -> UNHEALTHY
            "4" -> VERY_UNHEALTHY
            else -> UNKNOWN
        }
        
        /** PM2.5 (μg/m³) - WHO 2021 기준 */
        fun fromPm25(v: Double): AqiLevel = when {
            v <= 15.0 -> GOOD              // AQG
            v <= 25.0 -> MODERATE          // IT-4
            v <= 37.5 -> UNHEALTHY_SENSITIVE // IT-3
            v <= 50.0 -> UNHEALTHY         // IT-2
            v <= 75.0 -> VERY_UNHEALTHY    // IT-1
            else -> HAZARDOUS
        }
        
        /** PM10 (μg/m³) - WHO 2021 기준 */
        fun fromPm10(v: Double): AqiLevel = when {
            v <= 45.0 -> GOOD              // AQG
            v <= 50.0 -> MODERATE          // IT-4
            v <= 75.0 -> UNHEALTHY_SENSITIVE // IT-3
            v <= 100.0 -> UNHEALTHY        // IT-2
            v <= 150.0 -> VERY_UNHEALTHY   // IT-1
            else -> HAZARDOUS
        }
    }
}
