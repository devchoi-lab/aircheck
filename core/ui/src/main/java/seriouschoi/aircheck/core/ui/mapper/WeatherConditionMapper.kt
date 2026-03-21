package seriouschoi.aircheck.core.ui.mapper

import android.content.Context
import seriouschoi.aircheck.core.domain.model.WeatherCondition
import seriouschoi.aircheck.core.resource.R as CoreR

/** 날씨 상태 string resource ID */
fun WeatherCondition.toLabelRes(): Int = when (this) {
    WeatherCondition.CLEAR_DAY -> CoreR.string.weather_clear_day
    WeatherCondition.CLEAR_NIGHT -> CoreR.string.weather_clear_night
    WeatherCondition.PARTLY_CLOUDY_DAY -> CoreR.string.weather_partly_cloudy
    WeatherCondition.PARTLY_CLOUDY_NIGHT -> CoreR.string.weather_partly_cloudy_night
    WeatherCondition.CLOUDY -> CoreR.string.weather_cloudy
    WeatherCondition.FOG -> CoreR.string.weather_fog
    WeatherCondition.DRIZZLE -> CoreR.string.weather_drizzle
    WeatherCondition.RAIN -> CoreR.string.weather_rain
    WeatherCondition.HEAVY_RAIN -> CoreR.string.weather_heavy_rain
    WeatherCondition.SNOW -> CoreR.string.weather_snow
    WeatherCondition.SNOW_SHOWER -> CoreR.string.weather_snow_shower
    WeatherCondition.THUNDERSTORM -> CoreR.string.weather_thunderstorm
    WeatherCondition.UNKNOWN -> CoreR.string.weather_unknown
}

/** 날씨 상태 라벨 (Context 기반) */
fun WeatherCondition.toLabel(context: Context): String = context.getString(toLabelRes())

/** 날씨 이모지 */
fun WeatherCondition.toEmoji(): String = when (this) {
    WeatherCondition.CLEAR_DAY -> "☀️"
    WeatherCondition.CLEAR_NIGHT -> "🌙"
    WeatherCondition.PARTLY_CLOUDY_DAY -> "⛅"
    WeatherCondition.PARTLY_CLOUDY_NIGHT -> "⛅"  // TODO: 야간 전용 이모지 추가
    WeatherCondition.CLOUDY -> "☁️"
    WeatherCondition.FOG -> "🌫️"
    WeatherCondition.DRIZZLE -> "🌦️"
    WeatherCondition.RAIN -> "🌧️"
    WeatherCondition.HEAVY_RAIN -> "⛈️"
    WeatherCondition.SNOW -> "❄️"
    WeatherCondition.SNOW_SHOWER -> "🌨️"
    WeatherCondition.THUNDERSTORM -> "⚡"
    WeatherCondition.UNKNOWN -> "❓"
}
