package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.RegionSearchResult

/**
 * 행정구역 데이터 조회
 */
interface RegionRepository {
    /** 최상위 지역 목록 (시/도) */
    suspend fun getTopLevelRegions(): List<Region>

    /** 경로로 특정 지역 조회 (예: ["서울특별시", "강남구"]) */
    suspend fun getRegionByPath(path: List<String>): Region?

    /** 검색어로 지역 필터링 (현재 depth만) */
    suspend fun searchRegions(query: String, parentPath: List<String> = emptyList()): List<Region>

    /** 전체 계층 재귀 검색 (도/시/군/구/읍/면/동 모두) */
    suspend fun deepSearchRegions(query: String): List<RegionSearchResult>
}
