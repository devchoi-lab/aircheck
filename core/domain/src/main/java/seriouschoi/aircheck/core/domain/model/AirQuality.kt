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
 *
 * @param pm25Max PM2.5 상한 (μg/m³), HAZARDOUS는 무한대
 * @param pm10Max PM10 상한 (μg/m³), HAZARDOUS는 무한대
 */
enum class AqiLevel(
    val pm25Min: Double,
    val pm25Max: Double,
    val pm10Min: Double,
    val pm10Max: Double
) {
    GOOD(0.0, 15.0, 0.0, 45.0),
    MODERATE(16.0, 25.0, 46.0, 50.0),
    UNHEALTHY_SENSITIVE(26.0, 37.5, 51.0, 75.0),
    UNHEALTHY(37.6, 50.0, 76.0, 100.0),
    VERY_UNHEALTHY(51.0, 75.0, 101.0, 150.0),
    HAZARDOUS(76.0, Double.MAX_VALUE, 151.0, Double.MAX_VALUE),
    UNKNOWN(0.0, 0.0, 0.0, 0.0);

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
            v <= GOOD.pm25Max -> GOOD
            v <= MODERATE.pm25Max -> MODERATE
            v <= UNHEALTHY_SENSITIVE.pm25Max -> UNHEALTHY_SENSITIVE
            v <= UNHEALTHY.pm25Max -> UNHEALTHY
            v <= VERY_UNHEALTHY.pm25Max -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }

        /** PM10 (μg/m³) - WHO 2021 기준 */
        fun fromPm10(v: Double): AqiLevel = when {
            v <= GOOD.pm10Max -> GOOD
            v <= MODERATE.pm10Max -> MODERATE
            v <= UNHEALTHY_SENSITIVE.pm10Max -> UNHEALTHY_SENSITIVE
            v <= UNHEALTHY.pm10Max -> UNHEALTHY
            v <= VERY_UNHEALTHY.pm10Max -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}
