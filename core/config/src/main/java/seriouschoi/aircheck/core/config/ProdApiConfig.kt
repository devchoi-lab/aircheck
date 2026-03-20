package seriouschoi.aircheck.core.config

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로덕션 API 설정
 */
@Singleton
class ProdApiConfig @Inject constructor() : ApiConfig {
    override val baseUrl: String = "https://api.todaygonggi.com/"
    override val timeoutMs: Long = 30_000L
    override val connectTimeoutMs: Long = 10_000L
}
