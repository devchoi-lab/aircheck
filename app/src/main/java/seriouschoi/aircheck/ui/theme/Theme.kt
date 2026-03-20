package seriouschoi.aircheck.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Navy40,
    onPrimary = Color.White,
    primaryContainer = Navy90,
    onPrimaryContainer = Navy10,
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Sky40,
    onTertiary = Color.White,
    tertiaryContainer = Sky90,
    onTertiaryContainer = Sky10,
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVar90,
    onSurfaceVariant = NeutralVar30,
    surfaceContainer = Neutral90,
    outline = NeutralVar50,
    outlineVariant = NeutralVar80,
)

private val DarkColorScheme = darkColorScheme(
    primary = Navy80,
    onPrimary = Navy20,
    primaryContainer = Navy30,
    onPrimaryContainer = Navy90,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Sky80,
    onTertiary = Sky20,
    tertiaryContainer = Sky30,
    onTertiaryContainer = Sky90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = DarkNavyBg,
    onBackground = Neutral90,
    surface = DarkNavyBg,
    onSurface = Neutral90,
    surfaceVariant = NeutralVar30,
    onSurfaceVariant = NeutralVar80,
    surfaceContainer = Neutral17,
    outline = NeutralVar60,
    outlineVariant = NeutralVar30,
)

@Composable
fun AirCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
