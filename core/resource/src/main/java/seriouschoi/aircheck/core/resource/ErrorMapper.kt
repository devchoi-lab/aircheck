package seriouschoi.aircheck.core.resource

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * 기술적 예외를 사용자 친화적 메시지로 변환
 * - 서버 URL, IP 주소 등 민감한 정보 숨김
 */
object ErrorMapper {
    
    fun toUiText(throwable: Throwable?): UiText? {
        if (throwable == null) return null
        
        return when (throwable) {
            is UnknownHostException -> UiText.StringResource(R.string.error_network_dns)
            is ConnectException -> UiText.StringResource(R.string.error_network_connection)
            is SocketTimeoutException -> UiText.StringResource(R.string.error_network_timeout)
            is SSLException -> UiText.StringResource(R.string.error_network_ssl)
            else -> {
                // 기타 예외는 일반 네트워크 오류로 처리
                // 원본 메시지에 URL/IP가 포함되어 있을 수 있으므로 숨김
                UiText.StringResource(R.string.error_network_general)
            }
        }
    }
}
