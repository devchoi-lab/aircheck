package seriouschoi.aircheck.core.domain.repository

import kotlinx.coroutines.flow.Flow
import seriouschoi.aircheck.core.domain.model.SavedLocation

/**
 * 저장된 위치 관리
 */
interface SavedLocationRepository {
    /** 모든 저장된 위치 (순서대로) */
    fun getAll(): Flow<List<SavedLocation>>
    
    /** 위치 추가 */
    suspend fun add(location: SavedLocation): Long
    
    /** 위치 삭제 */
    suspend fun delete(id: Long)
    
    /** 순서 변경 */
    suspend fun updateOrder(locations: List<SavedLocation>)
}
