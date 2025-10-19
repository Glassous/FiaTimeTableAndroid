package com.glassous.fiatimetable.ui.theme

import android.os.Build
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import com.glassous.fiatimetable.R

@Composable
private fun resourceColorScheme(darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    val baseContext = LocalContext.current
    val variantContext = run {
        val config = Configuration(baseContext.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        baseContext.createConfigurationContext(config)
    }
    fun colorOf(resId: Int) = Color(ContextCompat.getColor(variantContext, resId))

    val primary = colorOf(R.color.md_theme_primary)
    val onPrimary = colorOf(R.color.md_theme_onPrimary)
    val primaryContainer = colorOf(R.color.md_theme_primaryContainer)
    val onPrimaryContainer = colorOf(R.color.md_theme_onPrimaryContainer)
    val secondary = colorOf(R.color.md_theme_secondary)
    val onSecondary = colorOf(R.color.md_theme_onSecondary)
    val secondaryContainer = colorOf(R.color.md_theme_secondaryContainer)
    val onSecondaryContainer = colorOf(R.color.md_theme_onSecondaryContainer)
    val tertiary = colorOf(R.color.md_theme_tertiary)
    val onTertiary = colorOf(R.color.md_theme_onTertiary)
    val tertiaryContainer = colorOf(R.color.md_theme_tertiaryContainer)
    val onTertiaryContainer = colorOf(R.color.md_theme_onTertiaryContainer)
    val error = colorOf(R.color.md_theme_error)
    val onError = colorOf(R.color.md_theme_onError)
    val errorContainer = colorOf(R.color.md_theme_errorContainer)
    val onErrorContainer = colorOf(R.color.md_theme_onErrorContainer)
    val background = colorOf(R.color.md_theme_background)
    val onBackground = colorOf(R.color.md_theme_onBackground)
    val surface = colorOf(R.color.md_theme_surface)
    val onSurface = colorOf(R.color.md_theme_onSurface)
    val surfaceVariant = colorOf(R.color.md_theme_surfaceVariant)
    val onSurfaceVariant = colorOf(R.color.md_theme_onSurfaceVariant)
    val outline = colorOf(R.color.md_theme_outline)
    val outlineVariant = colorOf(R.color.md_theme_outlineVariant)
    val inverseSurface = colorOf(R.color.md_theme_inverseSurface)
    val inverseOnSurface = colorOf(R.color.md_theme_inverseOnSurface)
    val inversePrimary = colorOf(R.color.md_theme_inversePrimary)
    val surfaceDim = colorOf(R.color.md_theme_surfaceDim)
    val surfaceBright = colorOf(R.color.md_theme_surfaceBright)
    val surfaceContainerLowest = colorOf(R.color.md_theme_surfaceContainerLowest)
    val surfaceContainerLow = colorOf(R.color.md_theme_surfaceContainerLow)
    val surfaceContainer = colorOf(R.color.md_theme_surfaceContainer)
    val surfaceContainerHigh = colorOf(R.color.md_theme_surfaceContainerHigh)
    val surfaceContainerHighest = colorOf(R.color.md_theme_surfaceContainerHighest)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary,
            surfaceDim = surfaceDim,
            surfaceBright = surfaceBright,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary,
            surfaceDim = surfaceDim,
            surfaceBright = surfaceBright,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    }
}

@Composable
fun FiaTimeTableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> resourceColorScheme(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}