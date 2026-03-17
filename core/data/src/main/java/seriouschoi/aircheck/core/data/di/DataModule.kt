package seriouschoi.aircheck.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import seriouschoi.aircheck.core.data.repository.AirQualityRepositoryImpl
import seriouschoi.aircheck.core.data.repository.RegionRepositoryImpl
import seriouschoi.aircheck.core.data.repository.SavedLocationRepositoryImpl
import seriouschoi.aircheck.core.data.repository.WeatherRepositoryImpl
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    @Singleton
    abstract fun bindAirQualityRepository(
        impl: AirQualityRepositoryImpl
    ): AirQualityRepository
    
    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        impl: WeatherRepositoryImpl
    ): WeatherRepository
    
    @Binds
    @Singleton
    abstract fun bindRegionRepository(
        impl: RegionRepositoryImpl
    ): RegionRepository
    
    @Binds
    @Singleton
    abstract fun bindSavedLocationRepository(
        impl: SavedLocationRepositoryImpl
    ): SavedLocationRepository
}
