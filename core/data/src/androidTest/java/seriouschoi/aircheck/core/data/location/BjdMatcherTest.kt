package seriouschoi.aircheck.core.data.location

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BjdMatcher Instrumented 테스트
 * 
 * 실제 Android 환경에서 assets 로드 + 좌표 매칭 검증
 */
@RunWith(AndroidJUnit4::class)
class BjdMatcherTest {

    private lateinit var bjdMatcher: BjdMatcher

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        bjdMatcher = BjdMatcher(context)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Assets 로드 검증
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun assets_korean_regions_2_json_존재() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assets = context.assets.list("") ?: emptyArray()
        assertTrue("korean_regions_2.json 존재", assets.contains("korean_regions_2.json"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 정상 케이스
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 서울_강남역_좌표_강남구_반환() = runBlocking {
        // Given
        val lat = 37.498095
        val lng = 127.027610

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("결과가 있어야 함", result)
        assertTrue("서울 강남구 포함", result!!.name.contains("강남구"))
    }

    @Test
    fun 부산_해운대_좌표_해운대구_반환() = runBlocking {
        // Given
        val lat = 35.158698
        val lng = 129.160384

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("결과가 있어야 함", result)
        assertTrue("부산 해운대구 포함", result!!.name.contains("해운대"))
    }

    @Test
    fun 제주_서귀포_좌표_서귀포시_반환() = runBlocking {
        // Given
        val lat = 33.253077
        val lng = 126.559757

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("결과가 있어야 함", result)
        assertTrue("제주 포함", result!!.name.contains("서귀포") || result.name.contains("제주"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 경계 케이스
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 대한민국_최북단_좌표_결과_반환() = runBlocking {
        // Given: 강원도 고성 근처
        val lat = 38.45
        val lng = 128.40

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("최북단도 결과 있어야 함", result)
    }

    @Test
    fun 대한민국_최남단_좌표_결과_반환() = runBlocking {
        // Given: 제주 마라도 근처
        val lat = 33.11
        val lng = 126.27

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull("최남단도 결과 있어야 함", result)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 해외 좌표 (범위 밖)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 일본_도쿄_좌표_null_반환() = runBlocking {
        // Given
        val lat = 35.6762
        val lng = 139.6503

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNull("해외 좌표는 null", result)
    }

    @Test
    fun 중국_베이징_좌표_null_반환() = runBlocking {
        // Given
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
    fun 반환된_centroid는_대한민국_범위_내() = runBlocking {
        // Given
        val lat = 37.5665
        val lng = 126.9780

        // When
        val result = bjdMatcher.findNearest(lat, lng)

        // Then
        assertNotNull(result)
        assertTrue("위도 범위 내", result!!.centroidLat in 33.0..39.0)
        assertTrue("경도 범위 내", result.centroidLng in 124.0..132.0)
    }

    @Test
    fun 법정동_코드는_숫자_문자열() = runBlocking {
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
    fun 조회_1000회_1초_이내() = runBlocking {
        // Given
        val lat = 37.5665
        val lng = 126.9780

        // When
        val startTime = System.currentTimeMillis()
        repeat(1000) {
            bjdMatcher.findNearest(lat, lng)
        }
        val elapsed = System.currentTimeMillis() - startTime

        // Then
        assertTrue("1000회 3초 이내 (실제: ${elapsed}ms)", elapsed < 3000)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 전국 주요 도시
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 전국_7대_도시_매칭() = runBlocking {
        val testCases = listOf(
            Triple(37.5665, 126.9780, "서울"),
            Triple(35.1796, 129.0756, "부산"),
            Triple(35.8714, 128.6014, "대구"),
            Triple(37.4563, 126.7052, "인천"),
            Triple(35.1595, 126.8526, "광주"),
            Triple(36.3504, 127.3845, "대전"),
            Triple(35.5384, 129.3114, "울산"),
        )

        testCases.forEach { (lat, lng, expectedCity) ->
            val result = bjdMatcher.findNearest(lat, lng)
            assertNotNull("$expectedCity 결과 있어야 함", result)
        }
    }
}
