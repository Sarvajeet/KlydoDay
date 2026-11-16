package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KlydoDarkColorScheme = darkColorScheme(
    primary = TerracottaAccent,
    onPrimary = CreamDial,
    primaryContainer = WarmSlate,
    onPrimaryContainer = CreamDial,
    secondary = SaffronAmber,
    onSecondary = ObsidianChassis,
    secondaryContainer = PlyCharcoal,
    onSecondaryContainer = WarmIvory,
    tertiary = CelestialTeal,
    onTertiary = ObsidianChassis,
    background = ObsidianChassis,
    onBackground = CreamDial,
    surface = PlyCharcoal,
    onSurface = CreamDial,
    surfaceVariant = WarmSlate,
    onSurfaceVariant = MutedSand,
    outline = HairlineBorder,
    error = CrimsonAlert,
    onError = CreamDial
)

@Composable
fun KlydoDayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KlydoDarkColorScheme,
        typography = KlydoTypography,
        content = content
    )
}
