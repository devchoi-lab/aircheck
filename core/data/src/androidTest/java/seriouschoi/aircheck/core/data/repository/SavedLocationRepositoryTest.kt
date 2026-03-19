package seriouschoi.aircheck.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import seriouschoi.aircheck.core.domain.model.SavedLocation

/**
 * SavedLocationRepository Instrumented 테스트
 * 
 * DataStore 기반 위치 저장/조회/삭제 검증
 */
@RunWith(AndroidJUnit4::class)
class SavedLocationRepositoryTest {

    private lateinit var repository: SavedLocationRepositoryImpl

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = SavedLocationRepositoryImpl(context)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 저장 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 위치_저장_후_조회() = runTest {
        // Given
        val location = createLocation("집", "서울 강남구")

        // When
        val id = repository.add(location)

        // Then
        val saved = repository.getAll().first()
        assertTrue("저장된 위치 있음", saved.any { it.id == id })
        
        // Cleanup
        repository.delete(id)
    }

    @Test
    fun 저장_시_ID_자동_생성() = runTest {
        // Given
        val location = createLocation("회사", "서울 중구")

        // When
        val id = repository.add(location)

        // Then
        assertTrue("ID는 0보다 큼", id > 0)
        
        // Cleanup
        repository.delete(id)
    }

    @Test
    fun 여러_위치_저장() = runTest {
        // Given
        val loc1 = createLocation("집", "서울 강남구")
        val loc2 = createLocation("회사", "서울 중구")
        val loc3 = createLocation("카페", "서울 마포구")

        // When
        val id1 = repository.add(loc1)
        val id2 = repository.add(loc2)
        val id3 = repository.add(loc3)

        // Then
        val saved = repository.getAll().first()
        assertTrue("3개 이상 저장됨", saved.size >= 3)
        
        // Cleanup
        repository.delete(id1)
        repository.delete(id2)
        repository.delete(id3)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 삭제 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 위치_삭제() = runTest {
        // Given
        val location = createLocation("삭제할 위치", "서울")
        val id = repository.add(location)
        
        // 저장 확인
        val beforeDelete = repository.getAll().first()
        assertTrue("삭제 전 존재", beforeDelete.any { it.id == id })

        // When
        repository.delete(id)

        // Then
        val afterDelete = repository.getAll().first()
        assertFalse("삭제 후 없음", afterDelete.any { it.id == id })
    }

    @Test
    fun 없는_ID_삭제해도_에러_없음() = runTest {
        // Given
        val nonExistentId = 999999999L

        // When & Then (에러 없이 완료)
        repository.delete(nonExistentId)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 순서 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 저장_순서_유지() = runTest {
        // Given
        val loc1 = createLocation("첫번째", "A")
        val loc2 = createLocation("두번째", "B")
        
        // When
        val id1 = repository.add(loc1)
        val id2 = repository.add(loc2)

        // Then
        val saved = repository.getAll().first()
        val filtered = saved.filter { it.id == id1 || it.id == id2 }
        
        assertEquals("첫번째 위치", id1, filtered[0].id)
        assertEquals("두번째 위치", id2, filtered[1].id)
        
        // Cleanup
        repository.delete(id1)
        repository.delete(id2)
    }

    @Test
    fun 순서_업데이트() = runTest {
        // Given
        val loc1 = createLocation("A", "지역A")
        val loc2 = createLocation("B", "지역B")
        val id1 = repository.add(loc1)
        val id2 = repository.add(loc2)
        
        val saved = repository.getAll().first()
        val toReorder = saved.filter { it.id == id1 || it.id == id2 }

        // When: 순서 뒤집기
        val reordered = toReorder.reversed()
        repository.updateOrder(reordered)

        // Then
        val afterReorder = repository.getAll().first()
        val filtered = afterReorder.filter { it.id == id1 || it.id == id2 }
        
        assertEquals("B가 먼저", id2, filtered[0].id)
        assertEquals("A가 나중", id1, filtered[1].id)
        
        // Cleanup
        repository.delete(id1)
        repository.delete(id2)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 데이터 무결성 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun 저장된_데이터_정확성() = runTest {
        // Given
        val location = SavedLocation(
            id = 0,
            name = "테스트",
            displayName = "서울특별시 강남구 테스트동",
            lat = 37.12345,
            lng = 127.67890,
            order = 0
        )

        // When
        val id = repository.add(location)

        // Then
        val saved = repository.getAll().first().find { it.id == id }
        assertNotNull("저장된 위치 찾음", saved)
        assertEquals("이름 일치", "테스트", saved!!.name)
        assertEquals("표시이름 일치", "서울특별시 강남구 테스트동", saved.displayName)
        assertEquals("위도 일치", 37.12345, saved.lat, 0.00001)
        assertEquals("경도 일치", 127.67890, saved.lng, 0.00001)
        
        // Cleanup
        repository.delete(id)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper
    // ══════════════════════════════════════════════════════════════════════════

    private fun createLocation(name: String, displayName: String) = SavedLocation(
        id = 0,
        name = name,
        displayName = displayName,
        lat = 37.5,
        lng = 127.0,
        order = 0
    )
}
