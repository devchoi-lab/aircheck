package seriouschoi.aircheck.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RegionJson(
    val name: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val children: List<RegionJson> = emptyList()
)

@Serializable
private data class RegionsRoot(
    val regions: List<RegionJson>
)

@Singleton
class RegionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RegionRepository {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private var cachedRegions: List<Region>? = null
    
    override suspend fun getTopLevelRegions(): List<Region> {
        return getCachedRegions()
    }
    
    override suspend fun getRegionByPath(path: List<String>): Region? {
        if (path.isEmpty()) return null
        
        var current: Region? = getCachedRegions().find { it.name == path[0] }
        
        for (i in 1 until path.size) {
            current = current?.children?.find { it.name == path[i] }
            if (current == null) break
        }
        
        return current
    }
    
    override suspend fun searchRegions(query: String, parentPath: List<String>): List<Region> {
        val parent = if (parentPath.isEmpty()) {
            null
        } else {
            getRegionByPath(parentPath)
        }
        
        val searchList = parent?.children ?: getCachedRegions()
        
        return if (query.isBlank()) {
            searchList
        } else {
            searchList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
    
    private suspend fun getCachedRegions(): List<Region> {
        cachedRegions?.let { return it }
        
        return withContext(Dispatchers.IO) {
            val jsonString = context.assets.open("korean_regions.json")
                .bufferedReader()
                .use { it.readText() }
            
            val root = json.decodeFromString<RegionsRoot>(jsonString)
            root.regions.map { it.toDomain() }.also {
                cachedRegions = it
            }
        }
    }
    
    private fun RegionJson.toDomain(): Region = Region(
        name = name,
        lat = lat,
        lng = lng,
        children = children.map { it.toDomain() }
    )
}
