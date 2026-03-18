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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import seriouschoi.aircheck.core.data.location.SmartLocationRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SmartLocation
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context,
        smartLocationRepository: SmartLocationRepository
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
        
        override fun smartLocationFlow(): Flow<SmartLocation> {
            return smartLocationRepository.smartLocationFlow().map { result ->
                SmartLocation(
                    bjdCode = result.bjdCode,
                    bjdName = result.bjdName,
                    lat = result.centroidLat,
                    lng = result.centroidLng
                )
            }
        }
        
        override suspend fun getCurrentSmartLocation(): SmartLocation? {
            return smartLocationRepository.getCurrentSmartLocation()?.let { result ->
                SmartLocation(
                    bjdCode = result.bjdCode,
                    bjdName = result.bjdName,
                    lat = result.centroidLat,
                    lng = result.centroidLng
                )
            }
        }
        
        override fun forceRefresh() {
            smartLocationRepository.forceRefresh()
        }
    }
}
