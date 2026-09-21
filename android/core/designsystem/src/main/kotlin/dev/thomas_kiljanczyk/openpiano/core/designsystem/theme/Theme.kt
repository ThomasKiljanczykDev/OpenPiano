package dev.thomas_kiljanczyk.openpiano.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Amber,
    onPrimary = Ebony,
    background = SlateLight,
    surface = Ivory,
)

private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Ebony,
    background = Slate,
    surface = Ebony,
)

@Composable
fun OpenPianoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && dynamicAvailable && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && dynamicAvailable -> dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    CompositionLocalProvider(
        LocalKeyColors provides if (darkTheme) DarkKeyColors else LightKeyColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OpenPianoTypography,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                content = content,
            )
        }
    }
}
