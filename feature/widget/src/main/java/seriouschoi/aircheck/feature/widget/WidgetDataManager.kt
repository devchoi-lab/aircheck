package seriouschoi.aircheck.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.ui.mapper.toColorArgb
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.ui.mapper.toLabel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val locationProvider: LocationProvider
) {
    companion object {
        private const val TAG = "WidgetDataManager"
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

    suspend fun refreshWidgetData(): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "refreshWidgetData started")
        try {
            val smartLocation = locationProvider.getCurrentSmartLocation()
            if (smartLocation == null) {
                Log.e(TAG, "Failed to get location")
                return@withContext Result.failure(Exception(context.getString(CoreR.string.widget_error_location)))
            }
            Log.d(TAG, "Location: ${smartLocation.bjdName} (${smartLocation.bjdCode})")

            val output = fetchWeatherUseCase(smartLocation.bjdCode)
            val weather = output.result.weather.getOrNull()
            val airQuality = output.result.airQuality.getOrNull()

            Log.d(TAG, "Weather: ${weather != null}, AirQuality: ${airQuality != null}")

            if (weather == null && airQuality == null) {
                Log.e(TAG, "Both weather and airQuality are null")
                return@withContext Result.failure(Exception(context.getString(CoreR.string.widget_error_data)))
            }

            val locationName = smartLocation.bjdName.split(" ").lastOrNull() ?: context.getString(CoreR.string.widget_default_location)
            val pmLevel = airQuality?.level ?: AqiLevel.UNKNOWN
            val rainProb = weather?.precipitationProbability ?: 0
            val feelsLike = weather?.feelsLike ?: 20.0

            Log.d(TAG, "Saving - location: $locationName, pm: $pmLevel, rain: $rainProb, feelsLike: $feelsLike")
            saveData(locationName, pmLevel, rainProb, feelsLike)

            notifyWidgetUpdate()

            Log.d(TAG, "refreshWidgetData completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "refreshWidgetData failed", e)
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
        val pmColor = pmLevel.toColorArgb()

        prefs.edit()
            .putString(KEY_LOCATION, location)
            .putString(KEY_PM_LEVEL, pmLevel.toLabel(context))
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
}
