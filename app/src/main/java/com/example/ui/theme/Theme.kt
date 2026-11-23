package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.ThemeMode

private fun buildLightScheme(palette: AccentPalette): ColorScheme {
    val (primary, secondary) = when (palette) {
        AccentPalette.OCEAN_TEAL -> Sim1OceanBlue to Sim2DeepTeal
        AccentPalette.COBALT_MINT -> CobaltPrimary to MintSecondary
        AccentPalette.ROYAL_AMETHYST -> RoyalAmethystPrimary to RoyalAmethystSecondary
        AccentPalette.EMERALD_GOLD -> EmeraldPrimary to GoldSecondary
        AccentPalette.SUNSET_CRIMSON -> SunsetCrimsonPrimary to SunsetCoralSecondary
        AccentPalette.CYBER_NEON -> CyberCyanPrimary to CyberLimeSecondary
        AccentPalette.ROSE_QUARTZ -> RoseQuartzPrimary to RoseVioletSecondary
        AccentPalette.GRAPHITE_AMBER -> GraphitePrimary to AmberSecondary
    }
    val lightBg = when (palette) {
        AccentPalette.ROSE_QUARTZ -> Color(0xFFFFF7FA)
        else -> LightBackground
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.14f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.14f),
        onSecondaryContainer = secondary,
        tertiary = CallFavoriteAmber,
        onTertiary = Color.Black,
        background = lightBg,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        error = CallDeclineCrimson,
        onError = Color.White
    )
}

private fun buildDarkScheme(palette: AccentPalette, oled: Boolean): ColorScheme {
    val (primary, secondary) = when (palette) {
        AccentPalette.OCEAN_TEAL -> Sim1OceanBlueDark to Sim2DeepTealDark
        AccentPalette.COBALT_MINT -> Color(0xFF60A5FA) to Color(0xFF34D399)
        AccentPalette.ROYAL_AMETHYST -> Color(0xFFA78BFA) to Color(0xFFF472B6)
        AccentPalette.EMERALD_GOLD -> Color(0xFF34D399) to Color(0xFFFBBF24)
        AccentPalette.SUNSET_CRIMSON -> Color(0xFFFB7185) to Color(0xFFFB923C)
        AccentPalette.CYBER_NEON -> Color(0xFF22D3EE) to Color(0xFFA3E635)
        AccentPalette.ROSE_QUARTZ -> Color(0xFFF472B6) to Color(0xFFA78BFA)
        AccentPalette.GRAPHITE_AMBER -> Color(0xFF94A3B8) to Color(0xFFFBBF24)
    }
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF06162E),
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color(0xFF042622),
        secondaryContainer = secondary.copy(alpha = 0.22f),
        onSecondaryContainer = secondary,
        tertiary = CallFavoriteAmber,
        onTertiary = Color.Black,
        background = if (oled) OledBackground else DarkBackground,
        onBackground = DarkOnSurface,
        surface = if (oled) OledSurface else DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = if (oled) OledSurfaceVariant else DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = if (oled) OledOutline else DarkOutline,
        error = CallMissedCrimson,
        onError = Color.White
    )
}

@Composable
fun PulsePhoneTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = false,
    accentPalette: AccentPalette = AccentPalette.OCEAN_TEAL,
    forceOledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = forceOledBlack || when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED_BLACK -> true
    }
    val effectiveOled = forceOledBlack || themeMode == ThemeMode.OLED_BLACK

    val context = LocalContext.current
    val colorScheme = when {
        forceOledBlack -> buildDarkScheme(accentPalette, oled = true)
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (effectiveDark) {
                val base = dynamicDarkColorScheme(context)
                if (effectiveOled) {
                    base.copy(
                        background = OledBackground,
                        surface = OledSurface,
                        surfaceVariant = OledSurfaceVariant
                    )
                } else {
                    base
                }
            } else {
                dynamicLightColorScheme(context)
            }
        }
        effectiveDark -> buildDarkScheme(accentPalette, oled = effectiveOled)
        else -> buildLightScheme(accentPalette)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !effectiveDark
                controller.isAppearanceLightNavigationBars = !effectiveDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulsePhoneTypography,
        content = content
    )
}
