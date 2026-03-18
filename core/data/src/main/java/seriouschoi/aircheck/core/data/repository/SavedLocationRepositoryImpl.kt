@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import seriouschoi.aircheck.core.domain.model.SavedLocation
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "saved_locations")

@Serializable
private data class SavedLocationJson(
    val id: Long,
    val name: String,
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val order: Int
)

@Singleton
class SavedLocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SavedLocationRepository {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private val locationsKey = stringPreferencesKey("locations")
    private var nextId = 1L
    
    override fun getAll(): Flow<List<SavedLocation>> {
        return context.dataStore.data.map { prefs ->
            val jsonString = prefs[locationsKey] ?: "[]"
            try {
                json.decodeFromString<List<SavedLocationJson>>(jsonString)
                    .map { it.toDomain() }
                    .sortedBy { it.order }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    override suspend fun add(location: SavedLocation): Long {
        val id = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = getCurrentList(prefs)
            val newLocation = location.copy(id = id, order = current.size)
            val updated = current + newLocation.toJson()
            prefs[locationsKey] = json.encodeToString(updated)
        }
        return id
    }
    
    override suspend fun delete(id: Long) {
        context.dataStore.edit { prefs ->
            val current = getCurrentList(prefs)
            val updated = current.filter { it.id != id }
            prefs[locationsKey] = json.encodeToString(updated)
        }
    }
    
    override suspend fun updateOrder(locations: List<SavedLocation>) {
        context.dataStore.edit { prefs ->
            val updated = locations.mapIndexed { index, loc ->
                loc.copy(order = index).toJson()
            }
            prefs[locationsKey] = json.encodeToString(updated)
        }
    }
    
    private fun getCurrentList(prefs: Preferences): List<SavedLocationJson> {
        val jsonString = prefs[locationsKey] ?: "[]"
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun SavedLocationJson.toDomain() = SavedLocation(
        id = id,
        name = name,
        displayName = displayName,
        lat = lat,
        lng = lng,
        order = order
    )
    
    private fun SavedLocation.toJson() = SavedLocationJson(
        id = id,
        name = name,
        displayName = displayName,
        lat = lat,
        lng = lng,
        order = order
    )
}
