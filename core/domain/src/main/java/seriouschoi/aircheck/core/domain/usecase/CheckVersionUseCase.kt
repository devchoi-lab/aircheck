package seriouschoi.aircheck.core.domain.usecase

import seriouschoi.aircheck.core.domain.model.VersionCheck
import seriouschoi.aircheck.core.domain.repository.VersionRepository
import javax.inject.Inject

class CheckVersionUseCase @Inject constructor(
    private val versionRepository: VersionRepository
) {
    suspend operator fun invoke(versionCode: Int): Result<VersionCheck> {
        return versionRepository.checkVersion(versionCode)
    }
}
