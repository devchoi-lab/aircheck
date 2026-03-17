package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.Region

/**
 * 행정구역 데이터 조회
 */
interface RegionRepository {
    /** 최상위 지역 목록 (시/도) */
    suspend fun getTopLevelRegions(): List<Region>
    
    /** 경로로 특정 지역 조회 (예: ["서울특별시", "강남구"]) */
    suspend fun getRegionByPath(path: List<String>): Region?
    
    /** 검색어로 지역 필터링 */
    suspend fun searchRegions(query: String, parentPath: List<String> = emptyList()): List<Region>
}
