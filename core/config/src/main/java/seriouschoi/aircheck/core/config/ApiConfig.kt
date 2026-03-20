package seriouschoi.aircheck.core.config

/**
 * API 설정 인터페이스
 * 
 * 환경별로 다른 구현체를 제공할 수 있음 (Prod, Dev, Mock)
 */
interface ApiConfig {
    /** API 베이스 URL */
    val baseUrl: String
    
    /** HTTP 타임아웃 (밀리초) */
    val timeoutMs: Long
    
    /** 연결 타임아웃 (밀리초) */
    val connectTimeoutMs: Long
}
