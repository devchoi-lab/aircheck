package seriouschoi.aircheck.core.domain.model

enum class RecommendedClothing {
    TANK_TOP,
    T_SHIRT,
    LONG_SLEEVE,
    JACKET,
    COAT,
    PADDING,
    HEAVY_PADDING;

    companion object {
        fun fromFeelsLike(feelsLike: Double): RecommendedClothing = when {
            feelsLike >= 28 -> TANK_TOP
            feelsLike >= 23 -> T_SHIRT
            feelsLike >= 17 -> LONG_SLEEVE
            feelsLike >= 12 -> JACKET
            feelsLike >= 6 -> COAT
            feelsLike >= 0 -> PADDING
            else -> HEAVY_PADDING
        }
    }
}
