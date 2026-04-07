package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.data.db.EventLocationMappingDao
import seriouschoi.aircheck.core.data.db.EventLocationMappingEntity
import seriouschoi.aircheck.core.domain.repository.EventLocationMapping
import seriouschoi.aircheck.core.domain.repository.EventLocationMappingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventLocationMappingRepositoryImpl @Inject constructor(
    private val dao: EventLocationMappingDao,
) : EventLocationMappingRepository {

    override suspend fun findByEventId(eventId: Long): EventLocationMapping? =
        dao.findByEventId(eventId)?.toDomain()

    override suspend fun upsert(mapping: EventLocationMapping) {
        dao.upsert(mapping.toEntity())
    }

    override suspend fun deleteByEventId(eventId: Long) {
        dao.deleteByEventId(eventId)
    }

    override suspend fun deleteOlderThan(thresholdUtcMillis: Long): Int =
        dao.deleteOlderThan(thresholdUtcMillis)

    private fun EventLocationMappingEntity.toDomain() = EventLocationMapping(
        eventId = eventId,
        bjdCode = bjdCode,
        bjdName = bjdName,
        locationTextSnapshot = locationTextSnapshot,
        eventEndUtcMillis = eventEndUtcMillis,
        updatedAtUtcMillis = updatedAtUtcMillis,
    )

    private fun EventLocationMapping.toEntity() = EventLocationMappingEntity(
        eventId = eventId,
        bjdCode = bjdCode,
        bjdName = bjdName,
        locationTextSnapshot = locationTextSnapshot,
        eventEndUtcMillis = eventEndUtcMillis,
        updatedAtUtcMillis = updatedAtUtcMillis,
    )
}
