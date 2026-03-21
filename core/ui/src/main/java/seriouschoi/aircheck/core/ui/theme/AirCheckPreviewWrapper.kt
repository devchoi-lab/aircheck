package seriouschoi.aircheck.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
fun AirCheckPreviewWrapper(content: @Composable () -> Unit) {
    AirCheckTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
