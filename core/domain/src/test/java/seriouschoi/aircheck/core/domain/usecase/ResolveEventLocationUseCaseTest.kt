package seriouschoi.aircheck.core.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.RegionSearchResult
import seriouschoi.aircheck.core.domain.model.ResolvedEventLocation
import seriouschoi.aircheck.core.domain.repository.EventLocationMapping
import seriouschoi.aircheck.core.domain.repository.EventLocationMappingRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.RegionRepository
import seriouschoi.aircheck.core.domain.repository.SmartLocation

class ResolveEventLocationUseCaseTest {

    private lateinit var mappingRepo: EventLocationMappingRepository
    private lateinit var regionRepo: RegionRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var useCase: ResolveEventLocationUseCase

    @Before
    fun setUp() {
        mappingRepo = mockk()
        regionRepo = mockk()
        locationProvider = mockk()
        useCase = ResolveEventLocationUseCase(mappingRepo, regionRepo, locationProvider)
    }

    // ── 1. MAPPING ─────────────────────────────────────────────────────────

    @Test
    fun `매핑이 있고 텍스트가 일치하면 MAPPING 반환`() = runTest {
        val mapping = EventLocationMapping(
            eventId = 1L,
            bjdCode = "1168010100",
            bjdName = "서울특별시 강남구 역삼동",
            locationTextSnapshot = "강남역 스타벅스",
            eventEndUtcMillis = 0L,
            updatedAtUtcMillis = 0L,
        )
        coEvery { mappingRepo.findByEventId(1L) } returns mapping

        val result = useCase(eventId = 1L, locationText = "강남역 스타벅스")

        assertEquals(ResolvedEventLocation.Source.MAPPING, result.source)
        assertEquals("1168010100", result.bjdCode)
        assertEquals("서울특별시 강남구 역삼동", result.bjdName)
    }

    @Test
    fun `매핑은 있지만 텍스트가 다르면 매핑을 무시한다`() = runTest {
        val mapping = EventLocationMapping(
            eventId = 1L,
            bjdCode = "1168010100",
            bjdName = "서울특별시 강남구 역삼동",
            locationTextSnapshot = "강남역 스타벅스",
            eventEndUtcMillis = 0L,
            updatedAtUtcMillis = 0L,
        )
        coEvery { mappingRepo.findByEventId(1L) } returns mapping
        // 텍스트가 바뀌었으므로 INFERRED 단계로 진입
        coEvery { regionRepo.deepSearchRegions(any()) } returns emptyList()
        coEvery { locationProvider.getCurrentSmartLocation() } returns
            SmartLocation("4113500000", "성남시 분당구", 37.4, 127.1)

        val result = useCase(eventId = 1L, locationText = "판교 카페")

        // INFERRED가 실패했으므로 CURRENT_LOCATION fallback
        assertEquals(ResolvedEventLocation.Source.CURRENT_LOCATION, result.source)
        assertEquals("4113500000", result.bjdCode)
    }

    // ── 2. INFERRED ─────────────────────────────────────────────────────────

    @Test
    fun `매핑이 없고 텍스트로 leaf 법정동을 찾으면 INFERRED 반환`() = runTest {
        coEvery { mappingRepo.findByEventId(any()) } returns null

        val leafRegion = Region(
            id = 1168010100,
            name = "역삼동",
            lat = 37.5,
            lng = 127.0,
            isLeaf = true,
        )
        coEvery { regionRepo.deepSearchRegions("강남역") } returns emptyList()
        coEvery { regionRepo.deepSearchRegions("역삼동") } returns listOf(
            RegionSearchResult(
                region = leafRegion,
                path = listOf("서울특별시", "강남구"),
            )
        )

        val result = useCase(eventId = 1L, locationText = "강남역 역삼동")

        assertEquals(ResolvedEventLocation.Source.INFERRED, result.source)
        assertEquals("1168010100", result.bjdCode)
        assertEquals("서울특별시 강남구 역삼동", result.bjdName)
    }

    @Test
    fun `leaf 매치가 비-leaf 매치보다 우선한다`() = runTest {
        coEvery { mappingRepo.findByEventId(any()) } returns null

        // 첫 토큰: 비-leaf만 매치
        val nonLeaf = Region(id = 1100000000, name = "강남구", lat = 0.0, lng = 0.0, isLeaf = false)
        // 두 번째 토큰: leaf 매치
        val leaf = Region(id = 1168010100, name = "역삼동", lat = 0.0, lng = 0.0, isLeaf = true)

        coEvery { regionRepo.deepSearchRegions("강남구") } returns listOf(
            RegionSearchResult(region = nonLeaf, path = listOf("서울특별시"))
        )
        coEvery { regionRepo.deepSearchRegions("역삼동") } returns listOf(
            RegionSearchResult(region = leaf, path = listOf("서울특별시", "강남구"))
        )

        val result = useCase(eventId = 1L, locationText = "강남구 역삼동")

        assertEquals(ResolvedEventLocation.Source.INFERRED, result.source)
        assertEquals("1168010100", result.bjdCode) // leaf 우선
    }

    @Test
    fun `leaf 매치가 없으면 첫 비-leaf 매치를 사용한다`() = runTest {
        coEvery { mappingRepo.findByEventId(any()) } returns null

        val nonLeaf = Region(id = 1100000000, name = "강남구", lat = 0.0, lng = 0.0, isLeaf = false)
        coEvery { regionRepo.deepSearchRegions("강남구") } returns listOf(
            RegionSearchResult(region = nonLeaf, path = listOf("서울특별시"))
        )

        val result = useCase(eventId = 1L, locationText = "강남구")

        assertEquals(ResolvedEventLocation.Source.INFERRED, result.source)
        assertEquals("1100000000", result.bjdCode)
        assertEquals("서울특별시 강남구", result.bjdName)
    }

    // ── 3. CURRENT_LOCATION ─────────────────────────────────────────────────

    @Test
    fun `매핑도 없고 텍스트도 비어있으면 현재위치 fallback`() = runTest {
        coEvery { mappingRepo.findByEventId(any()) } returns null
        coEvery { locationProvider.getCurrentSmartLocation() } returns
            SmartLocation("1168010100", "서울특별시 강남구 역삼동", 37.5, 127.0)

        val result = useCase(eventId = 1L, locationText = null)

        assertEquals(ResolvedEventLocation.Source.CURRENT_LOCATION, result.source)
        assertEquals("1168010100", result.bjdCode)
    }

    @Test
    fun `매핑도 없고 텍스트 유추 실패하면 현재위치 fallback`() = runTest {
        coEvery { mappingRepo.findByEventId(any()) } returns null
        coEvery { regionRepo.deepSearchRegions(any()) } returns emptyList()
        coEvery { locationProvider.getCurrentSmartLocation() } returns
            SmartLocation("1168010100", "서울특별시 강남구 역삼동", 37.5, 127.0)

        val result = useCase(eventId = 1L, locationText = "사무실 1층 카페")

        assertEquals(ResolvedEventLocation.Source.CURRENT_LOCATION, result.source)
        assertEquals("1168010100", result.bjdCode)
    }

    @Test
    fun `현재위치도 못 얻으면 빈 코드와 placeholder 라벨을 반환`() = runTest {
        coEvery { mappingRepo.findByEventId(any()) } returns null
        coEvery { locationProvider.getCurrentSmartLocation() } returns null

        val result = useCase(eventId = 1L, locationText = null)

        assertEquals(ResolvedEventLocation.Source.CURRENT_LOCATION, result.source)
        assertEquals("", result.bjdCode)
        assertEquals(
            ResolveEventLocationUseCase.CURRENT_LOCATION_FALLBACK_NAME,
            result.bjdName,
        )
    }
}
