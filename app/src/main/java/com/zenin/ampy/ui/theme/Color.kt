package com.zenin.ampy.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy Material colors (kept for compatibility with any remaining references).
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Ampy brand palette — a calm teal/green with a blue accent.
val AmpyLightPrimary = Color(0xFF00695C)
val AmpyLightSecondary = Color(0xFF2E7D32)
val AmpyLightTertiary = Color(0xFF1565C0)

val AmpyDarkPrimary = Color(0xFF5EEAD4)
val AmpyDarkSecondary = Color(0xFFA5D6A7)
val AmpyDarkTertiary = Color(0xFF90CAF9)

// Deprecated aliases kept so older references still compile.
@Deprecated("Use AmpyLightPrimary", ReplaceWith("AmpyLightPrimary"))
val RedLightPrimary get() = AmpyLightPrimary
@Deprecated("Use AmpyLightSecondary", ReplaceWith("AmpyLightSecondary"))
val RedLightSecondary get() = AmpyLightSecondary
@Deprecated("Use AmpyLightTertiary", ReplaceWith("AmpyLightTertiary"))
val RedLightTertiary get() = AmpyLightTertiary
@Deprecated("Use AmpyDarkPrimary", ReplaceWith("AmpyDarkPrimary"))
val RedDarkPrimary get() = AmpyDarkPrimary
@Deprecated("Use AmpyDarkSecondary", ReplaceWith("AmpyDarkSecondary"))
val RedDarkSecondary get() = AmpyDarkSecondary
@Deprecated("Use AmpyDarkTertiary", ReplaceWith("AmpyDarkTertiary"))
val RedDarkTertiary get() = AmpyDarkTertiary

// Light theme
val AmpyLightBackground = Color(0xFFF6F8F7)
val AmpyLightSurface = Color(0xFFFFFFFF)
val AmpyLightSurfaceContainer = Color(0xFFEDF3F0)
val AmpyLightSurfaceVariant = Color(0xFFDDE8E4)
val AmpyLightOnSurface = Color(0xFF111C19)
val AmpyLightOnSurfaceVariant = Color(0xFF40514C)
val AmpyLightOutline = Color(0xFF6F817B)
val AmpyLightPrimaryContainer = Color(0xFFB2DFDB)
val AmpyLightOnPrimaryContainer = Color(0xFF003D33)
val AmpyLightSecondaryContainer = Color(0xFFC8E6C9)
val AmpyLightOnSecondaryContainer = Color(0xFF1B5E20)
val AmpyLightTertiaryContainer = Color(0xFFD1E4FF)
val AmpyLightOnTertiaryContainer = Color(0xFF0D47A1)

// Dark theme
val AmpyDarkBackground = Color(0xFF0B1412)
val AmpyDarkSurface = Color(0xFF121E1B)
val AmpyDarkSurfaceContainer = Color(0xFF182621)
val AmpyDarkSurfaceVariant = Color(0xFF23332E)
val AmpyDarkOnSurface = Color(0xFFE4ECE9)
val AmpyDarkOnSurfaceVariant = Color(0xFFB8C9C3)
val AmpyDarkOutline = Color(0xFF84968F)
val AmpyDarkPrimaryContainer = Color(0xFF00695C)
val AmpyDarkOnPrimaryContainer = Color(0xFFB2DFDB)
val AmpyDarkSecondaryContainer = Color(0xFF2E7D32)
val AmpyDarkOnSecondaryContainer = Color(0xFFC8E6C9)
val AmpyDarkTertiaryContainer = Color(0xFF1565C0)
val AmpyDarkOnTertiaryContainer = Color(0xFFD1E4FF)
