package seriouschoi.aircheck.core.data.location

import io.mockk.every
import io.mockk.mockk
import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * BjdMatcher 단위 테스트
 * 
 * GPS 좌표 → 법정동 매칭 로직 검증
 */
class BjdMatcherTest {

    private lateinit var bjdMatcher: BjdMatcher

    @Before
    fun setup() {
        // JSON 파일을 test resources에서 로드
        val jsonStream = javaClass.classLoader!!.getResourceAsStream("korean_regions_2.json")
            ?: throw IllegalStateException("korean_regions_2.json not found in test resources")
        val jsonString = jsonStream.bufferedReader().use { it.readText() }
        
        // Context mock
        val mockAssetManager = mockk<AssetManager>()
        every { mockAssetManager.open("korean_regions_2.json") } returns ByteArrayInputStream(jsonString.toByteArray())
        
        val mockContext = mockk<Context>()
        every { mockContext.assets } returns mockAssetManager
        
        bjdMatcher = BjdMatcher(mockContext)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 정상 케이스
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `서울 강남역 좌표 - 역삼동 반환`() {
        // Given: 강남역 좌표
        val lat = 37.498095
        val lng = 127.027610

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("결과가 있어야 함", result)
        assertTrue("서울 강남구 포함", result!!.name.contains("강남구"))
        println("결과: ${result.name} (${result.code})")
    }

    @Test
    fun `부산 해운대 좌표 - 해운대구 반환`() {
        // Given: 해운대 해수욕장 좌표
        val lat = 35.158698
        val lng = 129.160384

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("결과가 있어야 함", result)
        assertTrue("부산 해운대구 포함", result!!.name.contains("해운대"))
        println("결과: ${result.name} (${result.code})")
    }

    @Test
    fun `제주 서귀포 좌표 - 서귀포시 반환`() {
        // Given: 서귀포시 좌표
        val lat = 33.253077
        val lng = 126.559757

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("결과가 있어야 함", result)
        assertTrue("제주 서귀포 포함", result!!.name.contains("서귀포") || result.name.contains("제주"))
        println("결과: ${result.name} (${result.code})")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 경계 케이스
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `대한민국 최북단 좌표 - 결과 반환`() {
        // Given: 강원도 고성 통일전망대 근처
        val lat = 38.45
        val lng = 128.40

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("최북단도 결과 있어야 함", result)
        println("최북단 결과: ${result!!.name}")
    }

    @Test
    fun `대한민국 최남단 좌표 - 결과 반환`() {
        // Given: 제주 마라도 근처
        val lat = 33.11
        val lng = 126.27

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("최남단도 결과 있어야 함", result)
        println("최남단 결과: ${result!!.name}")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 해외 좌표 (범위 밖)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `일본 도쿄 좌표 - null 반환`() {
        // Given: 도쿄 좌표
        val lat = 35.6762
        val lng = 139.6503

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNull("해외 좌표는 null", result)
    }

    @Test
    fun `중국 베이징 좌표 - null 반환`() {
        // Given: 베이징 좌표
        val lat = 39.9042
        val lng = 116.4074

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNull("해외 좌표는 null", result)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // centroid 좌표 검증
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `반환된 centroid는 대한민국 범위 내`() {
        // Given: 서울 좌표
        val lat = 37.5665
        val lng = 126.9780

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull(result)
        // 대한민국 위도 범위: 33 ~ 39
        assertTrue("위도 범위 내", result!!.centroidLat in 33.0..39.0)
        // 대한민국 경도 범위: 124 ~ 132
        assertTrue("경도 범위 내", result.centroidLng in 124.0..132.0)
    }

    @Test
    fun `법정동 코드는 숫자 문자열`() {
        // Given
        val lat = 37.5665
        val lng = 126.9780

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull(result)
        assertTrue("코드는 숫자로만 구성", result!!.code.all { it.isDigit() })
        assertTrue("코드가 비어있지 않음", result.code.isNotEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 성능 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `1000회 조회 성능`() {
        // Given: 서울 좌표
        val lat = 37.5665
        val lng = 126.9780

        // When
        val startTime = System.currentTimeMillis()
        repeat(1000) {
            bjdMatcher.findNearest(lat, lng)
        }
        val elapsed = System.currentTimeMillis() - startTime

        // Then
        println("1000회 조회: ${elapsed}ms (평균 ${elapsed / 1000.0}ms)")
        assertTrue("1000회 1초 이내", elapsed < 1000)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 다양한 지역 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `전국 주요 도시 매칭`() {
        val testCases = listOf(
            Triple(37.5665, 126.9780, "서울"),    // 서울시청
            Triple(35.1796, 129.0756, "부산"),    // 부산시청
            Triple(35.8714, 128.6014, "대구"),    // 대구시청
            Triple(37.4563, 126.7052, "인천"),    // 인천시청
            Triple(35.1595, 126.8526, "광주"),    // 광주시청
            Triple(36.3504, 127.3845, "대전"),    // 대전시청
            Triple(35.5384, 129.3114, "울산"),    // 울산시청
        )

        testCases.forEach { (lat, lng, expectedCity) ->
            val result = bjdMatcher.findNearest(lat, lng)
            assertNotNull("$expectedCity 결과 있어야 함", result)
            println("$expectedCity: ${result!!.name}")
        }
    }
}
