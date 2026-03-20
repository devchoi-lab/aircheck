package seriouschoi.aircheck.core.ui.mapper

import android.content.Context
import androidx.compose.ui.graphics.Color
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.ui.theme.AqiColors

/** 공기질 등급 string resource ID */
fun AqiLevel.toLabelRes(): Int = when (this) {
    AqiLevel.GOOD -> CoreR.string.aqi_good
    AqiLevel.MODERATE -> CoreR.string.aqi_moderate
    AqiLevel.UNHEALTHY_SENSITIVE -> CoreR.string.aqi_unhealthy_sensitive
    AqiLevel.UNHEALTHY -> CoreR.string.aqi_bad
    AqiLevel.VERY_UNHEALTHY -> CoreR.string.aqi_very_bad
    AqiLevel.HAZARDOUS -> CoreR.string.aqi_hazardous
    AqiLevel.UNKNOWN -> CoreR.string.aqi_unknown
}

/** 공기질 등급 라벨 (Context 기반) */
fun AqiLevel.toLabel(context: Context): String = context.getString(toLabelRes())

/** 공기질 이모지 */
fun AqiLevel.toEmoji(): String = when (this) {
    AqiLevel.GOOD -> "😊"
    AqiLevel.MODERATE -> "🙂"
    AqiLevel.UNHEALTHY_SENSITIVE -> "😐"
    AqiLevel.UNHEALTHY -> "😷"
    AqiLevel.VERY_UNHEALTHY -> "🤢"
    AqiLevel.HAZARDOUS -> "☠️"
    AqiLevel.UNKNOWN -> "❓"
}

/** 공기질 등급 색상 */
fun AqiLevel.toColor(): Color = when (this) {
    AqiLevel.GOOD -> AqiColors.Good
    AqiLevel.MODERATE -> AqiColors.Moderate
    AqiLevel.UNHEALTHY_SENSITIVE -> AqiColors.Sensitive
    AqiLevel.UNHEALTHY -> AqiColors.Unhealthy
    AqiLevel.VERY_UNHEALTHY -> AqiColors.VeryUnhealthy
    AqiLevel.HAZARDOUS -> AqiColors.Hazardous
    AqiLevel.UNKNOWN -> AqiColors.Unknown
}

/** PM2.5 농도 범위 문자열 (도메인 기준값 기반) */
fun AqiLevel.toPm25Range(): String = formatRange(pm25Min, pm25Max)

/** PM10 농도 범위 문자열 (도메인 기준값 기반) */
fun AqiLevel.toPm10Range(): String = formatRange(pm10Min, pm10Max)

private fun AqiLevel.formatRange(min: Double, max: Double): String = when (this) {
    AqiLevel.UNKNOWN -> "-"
    else -> if (max == Double.MAX_VALUE) "${min.formatValue()}+"
            else "${min.formatValue()} ~ ${max.formatValue()}"
}

private fun Double.formatValue(): String =
    if (this == this.toLong().toDouble()) toLong().toString() else toString()

/** 공기질 등급 색상 (ARGB Int - 위젯 RemoteViews용) */
fun AqiLevel.toColorArgb(): Int = android.graphics.Color.argb(
    (toColor().alpha * 255).toInt(),
    (toColor().red * 255).toInt(),
    (toColor().green * 255).toInt(),
    (toColor().blue * 255).toInt()
)
