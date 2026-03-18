package seriouschoi.aircheck.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import seriouschoi.aircheck.R
import seriouschoi.aircheck.MainActivity
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.resource.R as CoreR

class AircheckWidgetProvider : AppWidgetProvider() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_aircheck)
        
        // 앱 실행 인텐트
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // 데이터 로드 및 표시
        scope.launch {
            try {
                val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
                
                val location = prefs.getString("location", "현재 위치") ?: "현재 위치"
                val pmLevel = prefs.getString("pm_level", "---") ?: "---"
                val pmColor = prefs.getInt("pm_color", 0xFF888888.toInt())
                val rainProb = prefs.getString("rain_prob", "--%") ?: "--%"
                val clothing = prefs.getString("clothing", "---") ?: "---"
                val clothingIcon = prefs.getInt("clothing_icon", CoreR.drawable.ic_recommend_tshirt)
                val updateTime = prefs.getString("update_time", "") ?: ""
                
                views.setTextViewText(R.id.tv_location, location)
                views.setTextViewText(R.id.tv_pm_value, pmLevel)
                views.setTextColor(R.id.tv_pm_value, pmColor)
                views.setTextViewText(R.id.tv_rain_prob, rainProb)
                views.setTextViewText(R.id.tv_clothing, clothing)
                views.setImageViewResource(R.id.iv_clothing, clothingIcon)
                views.setTextViewText(R.id.tv_update_time, updateTime)
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                // 에러 시 기본값 유지
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
    
    companion object {
        /**
         * 위젯 데이터 저장 (앱에서 호출)
         */
        fun saveWidgetData(
            context: Context,
            location: String,
            pmLevel: AqiLevel,
            rainProb: Int,
            feelsLike: Double
        ) {
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            
            val (clothing, clothingIcon) = getClothingRecommendation(feelsLike)
            val pmColor = when (pmLevel) {
                AqiLevel.GOOD -> 0xFF4CAF50.toInt()
                AqiLevel.MODERATE -> 0xFFFFC107.toInt()
                AqiLevel.UNHEALTHY_SENSITIVE -> 0xFFFF9800.toInt()
                AqiLevel.UNHEALTHY -> 0xFFF44336.toInt()
                AqiLevel.VERY_UNHEALTHY -> 0xFF9C27B0.toInt()
                AqiLevel.HAZARDOUS -> 0xFF880E4F.toInt()
                AqiLevel.UNKNOWN -> 0xFF888888.toInt()
            }
            
            prefs.edit()
                .putString("location", location)
                .putString("pm_level", pmLevel.label)
                .putInt("pm_color", pmColor)
                .putString("rain_prob", "${rainProb}%")
                .putString("clothing", clothing)
                .putInt("clothing_icon", clothingIcon)
                .putString("update_time", "방금")
                .apply()
            
            // 위젯 갱신 요청
            val intent = Intent(context, AircheckWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
        
        private fun getClothingRecommendation(feelsLike: Double): Pair<String, Int> = when {
            feelsLike >= 28 -> "민소매" to CoreR.drawable.ic_recommend_tank_top
            feelsLike >= 23 -> "반팔" to CoreR.drawable.ic_recommend_tshirt
            feelsLike >= 17 -> "긴팔" to CoreR.drawable.ic_recommend_long_sleeve
            feelsLike >= 12 -> "자켓" to CoreR.drawable.ic_recommend_jacket
            feelsLike >= 6 -> "코트" to CoreR.drawable.ic_recommend_coat
            feelsLike >= 0 -> "패딩" to CoreR.drawable.ic_recommend_padding
            else -> "롱패딩" to CoreR.drawable.ic_recommend_heavy_padding
        }
    }
}
