package seriouschoi.aircheck.core.resource

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * ViewModel에서 UI 텍스트를 전달하기 위한 sealed class
 * - DynamicString: 동적으로 생성된 문자열 (API 응답 등)
 * - StringResource: strings.xml 리소스 ID
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(resId, *args)
    }
}
