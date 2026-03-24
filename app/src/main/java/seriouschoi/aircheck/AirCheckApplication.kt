package seriouschoi.aircheck

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import seriouschoi.aircheck.core.firebase.appcheck.AppCheckInitializer
import javax.inject.Inject

@HiltAndroidApp
class AirCheckApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var appCheckInitializer: AppCheckInitializer

    override fun onCreate() {
        super.onCreate()
        appCheckInitializer.initialize(this, BuildConfig.DEBUG)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
