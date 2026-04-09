package seriouschoi.aircheck.core.systemcalendar.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import seriouschoi.aircheck.core.domain.repository.CalendarEventRepository
import seriouschoi.aircheck.core.systemcalendar.CalendarEventRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemCalendarModule {

    @Binds
    @Singleton
    abstract fun bindCalendarEventRepository(
        impl: CalendarEventRepositoryImpl,
    ): CalendarEventRepository
}
