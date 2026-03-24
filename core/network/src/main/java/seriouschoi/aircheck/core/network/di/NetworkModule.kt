package seriouschoi.aircheck.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import seriouschoi.aircheck.core.config.ApiConfig
import seriouschoi.aircheck.core.firebase.appcheck.AppCheckInterceptor
import seriouschoi.aircheck.core.network.api.AircheckApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiConfig: ApiConfig,
        appCheckInterceptor: AppCheckInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(appCheckInterceptor)
            .connectTimeout(apiConfig.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(apiConfig.timeoutMs, TimeUnit.MILLISECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json, apiConfig: ApiConfig): Retrofit =
        Retrofit.Builder()
            .baseUrl(apiConfig.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAircheckApi(retrofit: Retrofit): AircheckApi =
        retrofit.create(AircheckApi::class.java)
}
