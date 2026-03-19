package seriouschoi.aircheck.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위젯 데이터 관리자
 * 
 * Repository에서 데이터 fetch → SharedPreferences 저장 → 위젯 갱신
 */
@Singleton
class WidgetDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val combinedWeatherRepository: CombinedWeatherRepository,
    private val locationProvider: LocationProvider
) {
    companion object {
        private const val PREFS_NAME = "widget_data"
        private const val KEY_LOCATION = "location"
        private const val KEY_PM_LEVEL = "pm_level"
        private const val KEY_PM_COLOR = "pm_color"
        private const val KEY_RAIN_PROB = "rain_prob"
        private const val KEY_CLOTHING = "clothing"
        private const val KEY_CLOTHING_ICON = "clothing_icon"
        private const val KEY_UPDATE_TIME = "update_time"
    }
    
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 위젯 데이터 갱신
     */
    suspend fun refreshWidgetData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 현재 위치 가져오기
            val smartLocation = locationProvider.getCurrentSmartLocation()
                ?: return@withContext Result.failure(Exception(context.getString(CoreR.string.widget_error_location)))
            
            // 2. 날씨 데이터 가져오기
            val result = combinedWeatherRepository.fetch(smartLocation.lat, smartLocation.lng)
            
            val weather = result.weather.getOrNull()
            val airQuality = result.airQuality.getOrNull()
            
            if (weather == null && airQuality == null) {
                return@withContext Result.failure(Exception(context.getString(CoreR.string.widget_error_data)))
            }
            
            // 3. 위젯 데이터 저장
            val locationName = smartLocation.bjdName.split(" ").lastOrNull() ?: context.getString(CoreR.string.widget_default_location)
            val pmLevel = airQuality?.level ?: AqiLevel.UNKNOWN
            val rainProb = weather?.precipitationProbability ?: 0
            val feelsLike = weather?.feelsLike ?: 20.0
            
            saveData(locationName, pmLevel, rainProb, feelsLike)
            
            // 4. 위젯 갱신 요청
            notifyWidgetUpdate()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun saveData(
        location: String,
        pmLevel: AqiLevel,
        rainProb: Int,
        feelsLike: Double
    ) {
        val clothing = RecommendedClothing.fromFeelsLike(feelsLike)
        val pmColor = getPmColor(pmLevel)
        
        prefs.edit()
            .putString(KEY_LOCATION, location)
            .putString(KEY_PM_LEVEL, pmLevel.label)
            .putInt(KEY_PM_COLOR, pmColor)
            .putString(KEY_RAIN_PROB, "${rainProb}%")
            .putString(KEY_CLOTHING, clothing.toLabel(context))
            .putInt(KEY_CLOTHING_ICON, clothing.toIconRes())
            .putString(KEY_UPDATE_TIME, context.getString(CoreR.string.widget_update_time_now))
            .apply()
    }
    
    private fun notifyWidgetUpdate() {
        val intent = Intent(context, AircheckWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = widgetManager.getAppWidgetIds(
                ComponentName(context, AircheckWidgetProvider::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        context.sendBroadcast(intent)
    }
    
    private fun getPmColor(level: AqiLevel): Int = when (level) {
        AqiLevel.GOOD -> 0xFF4CAF50.toInt()
        AqiLevel.MODERATE -> 0xFFFFC107.toInt()
        AqiLevel.UNHEALTHY_SENSITIVE -> 0xFFFF9800.toInt()
        AqiLevel.UNHEALTHY -> 0xFFF44336.toInt()
        AqiLevel.VERY_UNHEALTHY -> 0xFF9C27B0.toInt()
        AqiLevel.HAZARDOUS -> 0xFF880E4F.toInt()
        AqiLevel.UNKNOWN -> 0xFF888888.toInt()
    }
}
