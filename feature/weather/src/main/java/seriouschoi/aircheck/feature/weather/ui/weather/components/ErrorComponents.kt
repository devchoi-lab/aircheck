package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun ErrorSection(title: String, message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(CoreR.string.error_section_failed, title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        TextButton(onClick = onRetry) { Text(stringResource(CoreR.string.button_retry)) }
    }
}

@Composable
internal fun PermissionRequiredContent(
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text("📍", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(CoreR.string.permission_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(CoreR.string.permission_message),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(CoreR.string.permission_go_settings))
        }
    }
}

@Composable
internal fun ErrorContent(
    error: WeatherContract.ErrorState,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("😵", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(error.message.asString(), textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onIntent(error.action) }) {
            Text(stringResource(CoreR.string.button_retry_again))
        }
    }
}
