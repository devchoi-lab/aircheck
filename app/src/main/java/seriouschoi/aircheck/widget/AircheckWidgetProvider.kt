package seriouschoi.aircheck.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import seriouschoi.aircheck.R
import seriouschoi.aircheck.MainActivity
import seriouschoi.aircheck.core.resource.R as CoreR

class AircheckWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        
        // 위젯 추가/업데이트 시 데이터 갱신 요청
        WidgetUpdateWorker.refreshNow(context)
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 첫 번째 위젯 추가 시 주기적 갱신 스케줄링
        WidgetUpdateWorker.schedule(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 마지막 위젯 제거 시 스케줄 취소
        WidgetUpdateWorker.cancel(context)
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
        
        // SharedPreferences에서 저장된 데이터 로드
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        
        val location = prefs.getString("location", "위치 로딩중...") ?: "위치 로딩중..."
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
    }
}
