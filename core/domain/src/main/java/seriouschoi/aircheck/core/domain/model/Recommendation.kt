package seriouschoi.aircheck.core.domain.model

sealed class Recommendation {
    data class Clothing(val clothing: RecommendedClothing) : Recommendation()
    data class Umbrella(val maxPrecipitationProbability: Int) : Recommendation()
    data class Snow(val totalSnowfall: Double) : Recommendation()
    data class Mask(val aqiLevel: AqiLevel) : Recommendation()
}

fun getRecommendations(
    weather: WeatherData?,
    airQuality: AirQuality?
): List<Recommendation> = buildList {
    // 1. 체감온도 기반 옷 추천
    weather?.feelsLike?.let { temp ->
        add(Recommendation.Clothing(RecommendedClothing.fromFeelsLike(temp)))
    }

    // 2. 날씨 상태 기반 우산 (6시간 내 비 상태 존재 시)
    weather?.hourlyForecast?.take(6)
        ?.filter { it.condition.isRainy() }
        ?.maxOfOrNull { it.precipitationProbability }
        ?.let { add(Recommendation.Umbrella(it)) }

    // 3. 날씨 상태 기반 눈 (6시간 내 눈 상태 존재 시)
    weather?.hourlyForecast?.take(6)
        ?.filter { it.condition.isSnowy() }
        ?.sumOf { it.snowfall }
        ?.takeIf { it > 0 }
        ?.let { add(Recommendation.Snow(it)) }

    // 4. 대기질 민감군 주의 이상이면 마스크
    airQuality?.worstLevel
        ?.takeIf { it.ordinal >= AqiLevel.UNHEALTHY_SENSITIVE.ordinal && it != AqiLevel.UNKNOWN }
        ?.let { add(Recommendation.Mask(it)) }
}
