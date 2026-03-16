package seriouschoi.aircheck.di

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider = object : LocationProvider {
        
        private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        
        @SuppressLint("MissingPermission")
        override suspend fun getCurrentLocation(): Location? {
            return try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            } catch (e: Exception) {
                null
            }
        }
    }
}
