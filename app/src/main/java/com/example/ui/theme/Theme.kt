package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF004785),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFE5C16C),
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFF8CD6AF),
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF005133),
    onTertiaryContainer = Color(0xFFA8F2CA),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF132338),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFFC89D3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBF4E4),
    onSecondaryContainer = Color(0xFF261900),
    tertiary = Color(0xFF1E6B4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA8F2CA),
    onTertiaryContainer = Color(0xFF002113),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFDFDFD),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerLow = Color(0xFFF7F9FD),
    surfaceContainerHigh = Color(0xFFE8ECF2),
    surfaceContainerHighest = Color(0xFFDFE3E9),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Dynamic color enabled for wallpaper and phone theme matching!
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


