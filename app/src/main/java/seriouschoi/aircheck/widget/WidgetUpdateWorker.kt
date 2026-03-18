package seriouschoi.aircheck.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 위젯 백그라운드 갱신 Worker
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val widgetDataManager: WidgetDataManager
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            widgetDataManager.refreshWidgetData()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        private const val WORK_NAME = "widget_update_work"
        
        /**
         * 주기적 위젯 갱신 스케줄링 (30분마다)
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
        
        /**
         * 즉시 갱신 (1회성)
         */
        fun refreshNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
        
        /**
         * 스케줄 취소
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
