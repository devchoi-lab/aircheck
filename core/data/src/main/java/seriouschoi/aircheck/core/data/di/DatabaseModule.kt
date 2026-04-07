package seriouschoi.aircheck.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import seriouschoi.aircheck.core.data.db.AircheckDatabase
import seriouschoi.aircheck.core.data.db.EventLocationMappingDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAircheckDatabase(
        @ApplicationContext context: Context,
    ): AircheckDatabase = Room.databaseBuilder(
        context,
        AircheckDatabase::class.java,
        AircheckDatabase.DB_NAME,
    ).build()

    @Provides
    fun provideEventLocationMappingDao(
        db: AircheckDatabase,
    ): EventLocationMappingDao = db.eventLocationMappingDao()
}
