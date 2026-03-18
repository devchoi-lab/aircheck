package seriouschoi.aircheck.core.domain.model

/**
 * 지역 모델 (행정구역 계층 구조)
 */
data class Region(
    val name: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val children: List<Region> = emptyList()
) {
    /** 최하위 지역인지 (동/읍/면/리) */
    val isLeaf: Boolean get() = children.isEmpty() && lat != null && lng != null
    
    /** 하위 지역이 있는지 */
    val hasChildren: Boolean get() = children.isNotEmpty()
}

/**
 * 지역 검색 결과 (지역 + 상위 경로)
 */
data class RegionSearchResult(
    val region: Region,
    val path: List<String>        // 상위 경로 (예: ["서울특별시", "강남구"])
) {
    /** 전체 경로 표시용 (예: "서울특별시 > 강남구") */
    val pathDisplay: String get() = path.joinToString(" > ")

    /** 저장용 전체 이름 (예: "서울특별시 강남구 역삼동") */
    val fullName: String get() = (path + region.name).joinToString(" ")
}

/**
 * 저장된 위치 (사용자가 추가한 관심 지역)
 */
data class SavedLocation(
    val id: Long = 0,
    val name: String,           // "청담동"
    val displayName: String,    // "서울특별시 강남구 청담동"
    val lat: Double,
    val lng: Double,
    val order: Int = 0
)
