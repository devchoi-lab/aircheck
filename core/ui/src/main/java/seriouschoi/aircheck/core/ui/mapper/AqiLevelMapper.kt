package seriouschoi.aircheck.core.ui.mapper

import android.content.Context
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.resource.R as CoreR

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

/** 공기질 등급 색상 (ARGB Long) */
fun AqiLevel.toColorHex(): Long = when (this) {
    AqiLevel.GOOD -> 0xFF00C853
    AqiLevel.MODERATE -> 0xFFFFD600
    AqiLevel.UNHEALTHY_SENSITIVE -> 0xFFFF9100
    AqiLevel.UNHEALTHY -> 0xFFFF5722
    AqiLevel.VERY_UNHEALTHY -> 0xFF9C27B0
    AqiLevel.HAZARDOUS -> 0xFF7E0023
    AqiLevel.UNKNOWN -> 0xFF9E9E9E
}
