@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package seriouschoi.aircheck.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.RegionSearchResult
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 플랫 JSON 노드 */
@Serializable
private data class FlatRegionJson(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val isLeaf: Boolean,
    val parentId: Int? = null,
    val parentName: String? = null
)

@Serializable
private data class FlatRegionsRoot(
    val regions: List<FlatRegionJson>
)

@Singleton
class RegionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RegionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** 트리로 조립된 최상위 지역 캐시 */
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
        val parent = if (parentPath.isEmpty()) null else getRegionByPath(parentPath)
        val searchList = parent?.children ?: getCachedRegions()

        return if (query.isBlank()) {
            searchList
        } else {
            searchList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    override suspend fun deepSearchRegions(query: String): List<RegionSearchResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<RegionSearchResult>()

        fun searchRecursive(regions: List<Region>, path: List<String>) {
            for (region in regions) {
                if (region.name.contains(query, ignoreCase = true)) {
                    results.add(RegionSearchResult(region = region, path = path))
                }
                if (region.hasChildren) {
                    searchRecursive(region.children, path + region.name)
                }
            }
        }

        searchRecursive(getCachedRegions(), emptyList())
        return results
    }

    // ── 플랫 JSON → 트리 빌드 ────────────────────────────────────────────────

    private suspend fun getCachedRegions(): List<Region> {
        cachedRegions?.let { return it }

        return withContext(Dispatchers.IO) {
            val jsonString = context.assets.open("korean_regions_2.json")
                .bufferedReader()
                .use { it.readText() }

            val root = json.decodeFromString<FlatRegionsRoot>(jsonString)
            buildTree(root.regions).also {
                cachedRegions = it
            }
        }
    }

    /**
     * 플랫 리스트를 트리로 조립한다.
     * parentId == null 인 노드가 최상위.
     */
    private fun buildTree(flatList: List<FlatRegionJson>): List<Region> {
        // parentId별로 자식 그룹핑
        val childrenByParentId: Map<Int, List<FlatRegionJson>> =
            flatList.filter { it.parentId != null }.groupBy { it.parentId!! }

        fun buildNode(node: FlatRegionJson): Region {
            val children = childrenByParentId[node.id]
                ?.map { buildNode(it) }
                ?: emptyList()

            return Region(
                id = node.id,
                name = node.name,
                lat = node.lat,
                lng = node.lng,
                isLeaf = node.isLeaf,
                children = children
            )
        }

        // 최상위 노드 (parentId == null)
        return flatList
            .filter { it.parentId == null }
            .map { buildNode(it) }
    }
}
