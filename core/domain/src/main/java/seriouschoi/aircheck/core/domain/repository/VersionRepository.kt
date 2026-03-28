package seriouschoi.aircheck.core.domain.repository

import seriouschoi.aircheck.core.domain.model.VersionCheck

interface VersionRepository {
    suspend fun checkVersion(versionCode: Int): Result<VersionCheck>
}
