package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * 체감온도 + 미세먼지 기반 그라데이션 배경
 * 
 * X축: 체감온도 (파랑 ← → 빨강)
 * Y축: 미세먼지 (맑음 ← → 탁함/어두움)
 */
object GradientBackground {
    
    /**
     * 체감온도와 PM2.5 농도를 기반으로 배경색 계산
     * 
     * @param feelsLike 체감온도 (°C)
     * @param pm25 초미세먼지 농도 (μg/m³), null이면 미세먼지 영향 없음
     * @return 상단/하단 그라데이션 색상 쌍
     */
    fun calculateColors(feelsLike: Double?, pm25: Double?): Pair<Color, Color> {
        // 기본값 (데이터 없으면 중립 그레이)
        if (feelsLike == null) {
            return Color(0xFFE0E0E0) to Color(0xFFF5F5F5)
        }
        
        // 1. 체감온도 → 색상 Hue (240=파랑 ~ 0=빨강)
        val tempNormalized = ((feelsLike + 10) / 45).coerceIn(0.0, 1.0) // -10°C ~ 35°C
        val hue = (240 * (1 - tempNormalized)).toFloat() // 파랑(240) → 빨강(0)
        
        // 2. 미세먼지 → 채도/명도 조절
        val (saturation, brightness) = if (pm25 != null) {
            val airNormalized = (pm25 / 75).coerceIn(0.0, 1.0) // 0 ~ 75 μg/m³
            val sat = (0.3 - airNormalized * 0.2).toFloat() // 좋음: 0.3, 나쁨: 0.1
            val bright = (0.95 - airNormalized * 0.2).toFloat() // 좋음: 0.95, 나쁨: 0.75
            sat to bright
        } else {
            0.25f to 0.92f
        }
        
        // HSV → RGB 변환
        val topColor = hsvToColor(hue, saturation, brightness)
        val bottomColor = hsvToColor(hue, saturation * 0.5f, min(brightness + 0.05f, 1f))
        
        return topColor to bottomColor
    }
    
    /**
     * HSV를 Color로 변환
     */
    private fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
        val m = v - c
        
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        
        return Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f)
        )
    }
}

/**
 * 그라데이션 배경 Modifier
 */
@Composable
fun Modifier.weatherGradientBackground(
    feelsLike: Double?,
    pm25: Double?
): Modifier {
    val (topColor, bottomColor) = GradientBackground.calculateColors(feelsLike, pm25)
    
    return this.background(
        Brush.verticalGradient(
            colors = listOf(topColor, bottomColor)
        )
    )
}
