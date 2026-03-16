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
    val temperature: Double,
    val feelsLike: Double,
    val precipitationProbability: Int,
    val snowfall: Double,
    val condition: WeatherCondition
)

enum class WeatherCondition(val label: String, val emoji: String) {
    CLEAR_DAY("맑음", "☀️"),
    CLEAR_NIGHT("맑음 (야간)", "🌙"),
    PARTLY_CLOUDY("구름 조금", "⛅"),
    CLOUDY("흐림", "☁️"),
    FOG("안개", "🌫️"),
    DRIZZLE("이슬비", "🌦️"),
    RAIN("비", "🌧️"),
    HEAVY_RAIN("폭우", "⛈️"),
    SNOW("눈", "❄️"),
    SNOW_SHOWER("소나기눈", "🌨️"),
    THUNDERSTORM("뇌우", "⚡"),
    UNKNOWN("알 수 없음", "❓");

    companion object {
        fun from(code: Int, isDay: Boolean): WeatherCondition = when (code) {
            0 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            1, 2 -> PARTLY_CLOUDY
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
