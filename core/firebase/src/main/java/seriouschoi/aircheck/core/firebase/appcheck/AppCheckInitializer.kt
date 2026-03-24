package seriouschoi.aircheck.core.firebase.appcheck

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCheckInitializer @Inject constructor() {
    
    fun initialize(context: Context, isDebug: Boolean) {
        FirebaseApp.initializeApp(context)
        
        val factory = if (isDebug) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}
