package seriouschoi.aircheck.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventLocationMappingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AircheckDatabase : RoomDatabase() {
    abstract fun eventLocationMappingDao(): EventLocationMappingDao

    companion object {
        const val DB_NAME = "aircheck.db"
    }
}
