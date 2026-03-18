package seriouschoi.aircheck.core.domain.usecase

import kotlinx.coroutines.flow.first
import seriouschoi.aircheck.core.domain.model.SavedLocation
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import javax.inject.Inject

/**
 * 저장 위치 추가 UseCase
 *
 * 비즈니스 규칙: 최대 [MAX_SAVED_LOCATIONS]개까지만 저장 가능
 */
class AddSavedLocationUseCase @Inject constructor(
    private val repository: SavedLocationRepository
) {
    suspend operator fun invoke(location: SavedLocation): Result<Long> {
        val current = repository.getAll().first()

        // 중복 체크 (같은 좌표의 위치가 이미 저장돼 있으면 거부)
        val isDuplicate = current.any { it.lat == location.lat && it.lng == location.lng }
        if (isDuplicate) {
            return Result.failure(
                DuplicateLocationException("이미 저장된 위치입니다: ${location.displayName}")
            )
        }

        if (current.size >= MAX_SAVED_LOCATIONS) {
            return Result.failure(
                LimitExceededException("저장 가능한 위치는 최대 ${MAX_SAVED_LOCATIONS}개입니다.")
            )
        }
        val id = repository.add(location)
        return Result.success(id)
    }

    companion object {
        const val MAX_SAVED_LOCATIONS = 5
    }
}

/** 저장 위치 갯수 초과 예외 */
class LimitExceededException(message: String) : Exception(message)

/** 중복 위치 저장 시도 예외 */
class DuplicateLocationException(message: String) : Exception(message)
