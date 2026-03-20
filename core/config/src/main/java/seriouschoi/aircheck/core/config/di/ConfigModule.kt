package seriouschoi.aircheck.core.config.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import seriouschoi.aircheck.core.config.ApiConfig
import seriouschoi.aircheck.core.config.ProdApiConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {
    
    @Binds
    @Singleton
    abstract fun bindApiConfig(impl: ProdApiConfig): ApiConfig
}
