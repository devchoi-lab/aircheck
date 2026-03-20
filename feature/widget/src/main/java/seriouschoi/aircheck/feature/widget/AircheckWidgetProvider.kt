package seriouschoi.aircheck.feature.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import seriouschoi.aircheck.core.resource.R as CoreR

class AircheckWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "AircheckWidget"
        const val EXTRA_REQUEST_BG_LOCATION = "request_bg_location"
        const val ACTION_LAUNCH_APP = "seriouschoi.aircheck.ACTION_LAUNCH_APP"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called, widgetIds: ${appWidgetIds.toList()}")
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

        // 백그라운드 위치 권한이 없으면 앱을 열어 권한 요청 안내
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                it.putExtra(EXTRA_REQUEST_BG_LOCATION, true)
                context.startActivity(it)
            }
        }
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
        Log.d(TAG, "updateWidget called for widgetId: $appWidgetId")

        val views = RemoteViews(context.packageName, R.layout.widget_aircheck)

        // 앱 실행 인텐트 (패키지 매니저를 통해 런처 액티비티 가져오기)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // SharedPreferences에서 저장된 데이터 로드
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)

        val defaultLocation = context.getString(CoreR.string.widget_location_loading)
        val location = prefs.getString("location", defaultLocation) ?: defaultLocation
        val pmLevel = prefs.getString("pm_level", "---") ?: "---"
        val pmColor = prefs.getInt("pm_color", 0xFF888888.toInt())
        val rainProb = prefs.getString("rain_prob", "--%") ?: "--%"
        val clothing = prefs.getString("clothing", "---") ?: "---"
        val clothingIcon = prefs.getInt("clothing_icon", CoreR.drawable.ic_recommend_tshirt)
        val updateTime = prefs.getString("update_time", "") ?: ""

        Log.d(TAG, "Widget data - location: $location, pm: $pmLevel, rain: $rainProb, clothing: $clothing, time: $updateTime")

        views.setTextViewText(R.id.tv_location, location)
        views.setTextViewText(R.id.tv_pm_value, pmLevel)
        views.setTextColor(R.id.tv_pm_value, pmColor)
        views.setTextViewText(R.id.tv_rain_prob, rainProb)
        views.setTextViewText(R.id.tv_clothing, clothing)
        views.setImageViewResource(R.id.iv_clothing, clothingIcon)
        views.setTextViewText(R.id.tv_update_time, updateTime)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.d(TAG, "Widget updated successfully")
    }
}
