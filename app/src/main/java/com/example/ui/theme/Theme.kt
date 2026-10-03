package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EyeTealPrimaryDark,
    onPrimary = EyeTealOnPrimaryDark,
    primaryContainer = EyeTealContainerDark,
    onPrimaryContainer = EyeTealOnContainerDark,
    secondary = EyeSageSecondaryDark,
    onSecondary = EyeSageOnSecondaryDark,
    secondaryContainer = EyeSageContainerDark,
    onSecondaryContainer = EyeSageOnContainerDark,
    tertiary = EyeBlueTertiaryDark,
    background = EyeSurfaceDark,
    surface = EyeSurfaceDark,
    onBackground = EyeOnSurfaceDark,
    onSurface = EyeOnSurfaceDark,
    surfaceVariant = EyeSurfaceVariantDark,
    onSurfaceVariant = EyeOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = EyeTealPrimaryLight,
    onPrimary = EyeTealOnPrimaryLight,
    primaryContainer = EyeTealContainerLight,
    onPrimaryContainer = EyeTealOnContainerLight,
    secondary = EyeSageSecondaryLight,
    secondaryContainer = EyeSageContainerLight,
    onSecondaryContainer = EyeSageOnContainerLight,
    tertiary = EyeBlueTertiaryLight,
    background = EyeSurfaceLight,
    surface = EyeSurfaceLight,
    onBackground = EyeOnSurfaceLight,
    onSurface = EyeOnSurfaceLight,
    surfaceVariant = EyeSurfaceVariantLight,
    onSurfaceVariant = EyeOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our soothing custom eye-care palette by default
    content: @Composable () -> Unit,
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
