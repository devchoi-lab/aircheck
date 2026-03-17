package seriouschoi.aircheck.feature.weather.navigation

import kotlinx.serialization.Serializable

/** 날씨 피처 네비게이션 라우트 */
sealed interface WeatherRoute {
    @Serializable
    data object Main : WeatherRoute
    
    @Serializable
    data object LocationPicker : WeatherRoute
}
