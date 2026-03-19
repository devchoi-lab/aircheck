package seriouschoi.aircheck.core.data.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * 법정동 매칭기
 * 
 * GPS 좌표를 받아서 가장 가까운 법정동의 centroid 좌표를 반환
 * 격자 인덱싱으로 O(1) 버킷 조회 후 haversine 거리 계산
 */
@Singleton
class BjdMatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // 격자 인덱스 (0.1도 단위 ≈ 약 10km)
    private val gridIndex: Map<String, List<BjdDistrict>> by lazy {
        loadAndIndex()
    }
    
    private fun loadAndIndex(): Map<String, MutableList<BjdDistrict>> {
        val index = mutableMapOf<String, MutableList<BjdDistrict>>()
        
        try {
            val jsonString = context.assets.open("korean_regions_2.json")
                .bufferedReader()
                .use { it.readText() }
            
            val data = json.decodeFromString<RegionData>(jsonString)
            
            // isLeaf == true (동/읍/면)만 필터링
            data.regions
                .filter { it.isLeaf }
                .forEach { region ->
                    val district = BjdDistrict(
                        code = region.id.toString(),
                        name = region.fullName,
                        lat = region.lat,
                        lng = region.lng
                    )
                    val key = gridKey(district.lat, district.lng)
                    index.getOrPut(key) { mutableListOf() }.add(district)
                }
        } catch (e: Exception) {
            // 파일 로드 실패 시 빈 인덱스
            e.printStackTrace()
        }
        
        return index
    }
    
    /**
     * GPS 좌표 → 가장 가까운 법정동 찾기
     */
    fun findNearest(lat: Double, lng: Double): BjdResult? {
        // 현재 격자 + 인접 8개 격자 탐색
        val candidates = mutableListOf<BjdDistrict>()
        
        for (dLat in -1..1) {
            for (dLng in -1..1) {
                val key = gridKey(lat + dLat * 0.1, lng + dLng * 0.1)
                gridIndex[key]?.let { candidates.addAll(it) }
            }
        }
        
        if (candidates.isEmpty()) return null
        
        // 가장 가까운 법정동 찾기
        return candidates
            .minByOrNull { haversineDistance(lat, lng, it.lat, it.lng) }
            ?.let { BjdResult(it.code, it.name, it.lat, it.lng) }
    }
    
    private fun gridKey(lat: Double, lng: Double): String {
        val latKey = (lat * 10).toInt()
        val lngKey = (lng * 10).toInt()
        return "$latKey,$lngKey"
    }
    
    /**
     * Haversine 거리 계산 (km)
     */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // 지구 반지름 (km)
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return r * c
    }
}

// ── 모델 ───────────────────────────────────────────────────────────────────

@Serializable
data class RegionData(
    val regions: List<Region> = emptyList()
)

@Serializable
data class Region(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val isLeaf: Boolean = false,
    val parentId: Int? = null,
    val parentName: String? = null
) {
    val fullName: String
        get() = if (parentName != null) "$parentName $name" else name
}

data class BjdDistrict(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

/**
 * 법정동 매칭 결과
 */
data class BjdResult(
    val code: String,      // 법정동 코드
    val name: String,      // 법정동 이름
    val centroidLat: Double,  // centroid 위도
    val centroidLng: Double   // centroid 경도
)
