package seriouschoi.aircheck.core.domain.model

/**
 * 지역 모델 (행정구역 계층 구조)
 *
 * 플랫 JSON(korean_regions_2.json)에서 파싱 후 트리로 조립된다.
 * 모든 노드에 lat/lng가 존재한다 (parent는 하위 leaf의 centroid).
 */
data class Region(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    /** 최하위 지역인지 (동/읍/면/리) — JSON에서 직접 읽음 */
    val isLeaf: Boolean,
    val children: List<Region> = emptyList()
) {
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
    val bjdCode: String,        // 법정동 코드
    val lat: Double,
    val lng: Double,
    val order: Int = 0
)
