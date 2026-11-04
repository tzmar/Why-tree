package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WhyTreeDarkColorScheme = darkColorScheme(
  primary = SoftGold,
  onPrimary = MidnightNavy,
  primaryContainer = GoldContainer,
  onPrimaryContainer = OnGoldContainer,
  secondary = RelHow,
  onSecondary = MidnightNavy,
  tertiary = RelEvidence,
  onTertiary = MidnightNavy,
  background = MidnightNavy,
  onBackground = TextPrimary,
  surface = SurfaceNavy,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceElevatedNavy,
  onSurfaceVariant = TextSecondary,
  outline = CardBorder,
  outlineVariant = CardBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Why Tree defaults to its signature premium dark theme
  dynamicColor: Boolean = false, // Keep distinctive midnight theme
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = WhyTreeDarkColorScheme,
    typography = Typography,
    content = content
  )
}
