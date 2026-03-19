package seriouschoi.aircheck.core.ui.mapper

import android.content.Context
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.resource.R as CoreR

/** 도메인 의상 enum -> Android drawable 리소스 */
fun RecommendedClothing.toIconRes(): Int = when (this) {
    RecommendedClothing.TANK_TOP -> CoreR.drawable.ic_recommend_tank_top
    RecommendedClothing.T_SHIRT -> CoreR.drawable.ic_recommend_tshirt
    RecommendedClothing.LONG_SLEEVE -> CoreR.drawable.ic_recommend_long_sleeve
    RecommendedClothing.JACKET -> CoreR.drawable.ic_recommend_jacket
    RecommendedClothing.COAT -> CoreR.drawable.ic_recommend_coat
    RecommendedClothing.PADDING -> CoreR.drawable.ic_recommend_padding
    RecommendedClothing.HEAVY_PADDING -> CoreR.drawable.ic_recommend_heavy_padding
}

/** 간단 라벨 string resource ID (시간별 예보, 위젯 등) */
fun RecommendedClothing.toLabelRes(): Int = when (this) {
    RecommendedClothing.TANK_TOP -> CoreR.string.outfit_sleeveless
    RecommendedClothing.T_SHIRT -> CoreR.string.outfit_short_sleeve
    RecommendedClothing.LONG_SLEEVE -> CoreR.string.outfit_long_sleeve
    RecommendedClothing.JACKET -> CoreR.string.outfit_jacket
    RecommendedClothing.COAT -> CoreR.string.outfit_coat
    RecommendedClothing.PADDING -> CoreR.string.outfit_padding
    RecommendedClothing.HEAVY_PADDING -> CoreR.string.outfit_long_padding
}

/** 간단 라벨 문자열 (Context 기반) */
fun RecommendedClothing.toLabel(context: Context): String = context.getString(toLabelRes())

/** 추천 섹션용 상세 라벨 string resource ID */
fun RecommendedClothing.toDetailedLabelRes(): Int = when (this) {
    RecommendedClothing.TANK_TOP -> CoreR.string.outfit_sleeveless
    RecommendedClothing.T_SHIRT -> CoreR.string.outfit_short_sleeve
    RecommendedClothing.LONG_SLEEVE -> CoreR.string.outfit_thin_long
    RecommendedClothing.JACKET -> CoreR.string.outfit_jacket_cardigan
    RecommendedClothing.COAT -> CoreR.string.outfit_coat_knit
    RecommendedClothing.PADDING -> CoreR.string.outfit_padding_scarf
    RecommendedClothing.HEAVY_PADDING -> CoreR.string.outfit_heavy_padding_full
}

/** 추천 섹션용 상세 라벨 문자열 (Context 기반) */
fun RecommendedClothing.toDetailedLabel(context: Context): String = context.getString(toDetailedLabelRes())

/** 체감온도 범위 문자열 (Context 기반) */
fun RecommendedClothing.toTemperatureRange(context: Context): String = when (this) {
    RecommendedClothing.TANK_TOP -> context.getString(CoreR.string.temp_range_above, 28)
    RecommendedClothing.T_SHIRT -> context.getString(CoreR.string.temp_range_between, 23, 27)
    RecommendedClothing.LONG_SLEEVE -> context.getString(CoreR.string.temp_range_between, 17, 22)
    RecommendedClothing.JACKET -> context.getString(CoreR.string.temp_range_between, 12, 16)
    RecommendedClothing.COAT -> context.getString(CoreR.string.temp_range_between, 6, 11)
    RecommendedClothing.PADDING -> context.getString(CoreR.string.temp_range_between, 0, 5)
    RecommendedClothing.HEAVY_PADDING -> context.getString(CoreR.string.temp_range_below, 0)
}
