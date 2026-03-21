package seriouschoi.aircheck.core.domain.model

data class WeatherData(
    val temperature: Double,
    val feelsLike: Double,
    val precipitation: Double,
    val precipitationProbability: Int,
    val snowfallProbability: Int,
    val condition: WeatherCondition,
    val cloudCover: Int,
    val isDay: Boolean,
    val hourlyForecast: List<HourlyForecast> = emptyList()
)

data class HourlyForecast(
    val hour: String,
    val dayLabel: String,  // "오늘", "내일", "모레"
    val temperature: Double,
    val feelsLike: Double,
    val precipitationProbability: Int,
    val snowfall: Double,
    val condition: WeatherCondition
)

enum class WeatherCondition {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SNOW,
    SNOW_SHOWER,
    THUNDERSTORM,
    UNKNOWN;

    companion object {
        /**
         * 서버에서 보내는 WeatherCondition 문자열로 변환
         */
        fun fromString(condition: String, isDay: Boolean): WeatherCondition = when (condition) {
            "CLEAR" -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            "PARTLY_CLOUDY" -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
            "CLOUDY" -> CLOUDY
            "FOG" -> FOG
            "DRIZZLE" -> DRIZZLE
            "RAIN" -> RAIN
            "HEAVY_RAIN" -> HEAVY_RAIN
            "SNOW" -> SNOW
            "HEAVY_SNOW" -> SNOW  // 서버의 HEAVY_SNOW도 처리
            "SNOW_SHOWER" -> SNOW_SHOWER
            "THUNDERSTORM" -> THUNDERSTORM
            else -> UNKNOWN
        }

        @Deprecated("Use fromString instead", ReplaceWith("fromString(condition, isDay)"))
        fun from(code: Int, isDay: Boolean): WeatherCondition = when (code) {
            0 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            1, 2 -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            61, 63 -> RAIN
            65, 80, 81, 82 -> HEAVY_RAIN
            71, 73, 75 -> SNOW
            85, 86 -> SNOW_SHOWER
            95, 96, 99 -> THUNDERSTORM
            else -> UNKNOWN
        }
    }
}
