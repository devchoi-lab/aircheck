package seriouschoi.aircheck.core.domain.usecase

import kotlinx.coroutines.flow.first
import seriouschoi.aircheck.core.domain.model.DataError
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

        // 중복 체크 (같은 법정동이 이미 저장돼 있으면 거부)
        val isDuplicate = current.any { it.bjdCode == location.bjdCode }
        if (isDuplicate) {
            return Result.failure(DataError.DuplicateLocation)
        }

        if (current.size >= MAX_SAVED_LOCATIONS) {
            return Result.failure(DataError.SavedLocationLimitExceeded(MAX_SAVED_LOCATIONS))
        }
        val id = repository.add(location)
        return Result.success(id)
    }

    companion object {
        const val MAX_SAVED_LOCATIONS = 5
    }
}
