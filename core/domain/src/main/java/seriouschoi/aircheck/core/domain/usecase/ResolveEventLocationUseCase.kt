package seriouschoi.aircheck.core.domain.usecase

import seriouschoi.aircheck.core.domain.model.ResolvedEventLocation
import seriouschoi.aircheck.core.domain.repository.EventLocationMappingRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import javax.inject.Inject

/**
 * 캘린더 일정 한 건에 대해 어떤 법정동의 날씨를 보여줄지 결정한다.
 *
 * 우선순위:
 *  1. **MAPPING** — DB에 매핑이 있고, 매핑 시점의 EVENT_LOCATION 스냅샷이
 *     현재 [locationText]와 동일하면 그 매핑을 사용한다. 다른 캘린더 앱에서
 *     장소가 수정된 경우에는 스냅샷이 어긋나므로 매핑을 무시하고 다음 단계로 간다.
 *  2. **INFERRED** — 일정의 장소 텍스트를 공백 단위로 토큰화해 [RegionRepository]의
 *     `deepSearchRegions`로 검색한다. 가장 구체적인(leaf, 동/읍/면) 매치를 우선한다.
 *  3. **CURRENT_LOCATION** — 위 둘 다 실패하면 [LocationProvider]의 현재 위치로 대체.
 *     현재 위치도 못 얻으면 빈 코드 + "현재 위치" 라벨을 반환한다.
 */
class ResolveEventLocationUseCase @Inject constructor(
    private val eventLocationMappingRepository: EventLocationMappingRepository,
    private val regionRepository: RegionRepository,
    private val locationProvider: LocationProvider,
) {
    suspend operator fun invoke(
        eventId: Long,
        locationText: String?,
    ): ResolvedEventLocation {
        // 1. MAPPING
        val mapping = eventLocationMappingRepository.findByEventId(eventId)
        if (mapping != null && mapping.locationTextSnapshot == locationText) {
            return ResolvedEventLocation(
                bjdCode = mapping.bjdCode,
                bjdName = mapping.bjdName,
                source = ResolvedEventLocation.Source.MAPPING,
            )
        }

        // 2. INFERRED
        if (!locationText.isNullOrBlank()) {
            val inferred = inferFromText(locationText)
            if (inferred != null) return inferred
        }

        // 3. CURRENT_LOCATION fallback
        val current = locationProvider.getCurrentSmartLocation()
        return if (current != null) {
            ResolvedEventLocation(
                bjdCode = current.bjdCode,
                bjdName = current.bjdName,
                source = ResolvedEventLocation.Source.CURRENT_LOCATION,
            )
        } else {
            ResolvedEventLocation(
                bjdCode = "",
                bjdName = CURRENT_LOCATION_FALLBACK_NAME,
                source = ResolvedEventLocation.Source.CURRENT_LOCATION,
            )
        }
    }

    /**
     * 장소 텍스트를 공백 단위 토큰으로 쪼개 법정동을 검색한다.
     * - 토큰별로 [RegionRepository.deepSearchRegions]를 호출해 후보를 모은다.
     * - leaf(동/읍/면) 매치를 비-leaf보다 우선한다.
     * - 같은 leaf 후보가 여러 토큰에서 나오면 첫 매치 순서를 유지한다.
     */
    private suspend fun inferFromText(locationText: String): ResolvedEventLocation? {
        val tokens = locationText.split(WHITESPACE).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        var firstNonLeaf: ResolvedEventLocation? = null

        for (token in tokens) {
            val results = regionRepository.deepSearchRegions(token)
            for (result in results) {
                val resolved = ResolvedEventLocation(
                    bjdCode = result.region.id.toString(),
                    bjdName = result.fullName,
                    source = ResolvedEventLocation.Source.INFERRED,
                )
                if (result.region.isLeaf) return resolved
                if (firstNonLeaf == null) firstNonLeaf = resolved
            }
        }

        return firstNonLeaf
    }

    companion object {
        private val WHITESPACE = "\\s+".toRegex()
        const val CURRENT_LOCATION_FALLBACK_NAME = "현재 위치"
    }
}
