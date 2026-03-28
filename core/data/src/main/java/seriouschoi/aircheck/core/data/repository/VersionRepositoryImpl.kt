package seriouschoi.aircheck.core.data.repository

import seriouschoi.aircheck.core.domain.model.VersionCheck
import seriouschoi.aircheck.core.domain.repository.VersionRepository
import seriouschoi.aircheck.core.network.api.AircheckApi
import javax.inject.Inject

class VersionRepositoryImpl @Inject constructor(
    private val api: AircheckApi
) : VersionRepository {
    
    override suspend fun checkVersion(versionCode: Int): Result<VersionCheck> {
        return try {
            val response = api.checkVersion(versionCode = versionCode)
            Result.success(
                VersionCheck(
                    needUpdate = response.needUpdate,
                    updateUrl = response.updateUrl
                )
            )
        } catch (e: Exception) {
            // 버전 체크 실패 시 업데이트 불필요로 처리 (앱 사용 가능)
            Result.success(VersionCheck(needUpdate = false, updateUrl = null))
        }
    }
}
