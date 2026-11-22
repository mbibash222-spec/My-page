package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = EspressoSecondary,
    onSecondary = Color.White,
    secondaryContainer = EspressoContainer,
    onSecondaryContainer = OnEspressoContainer,
    tertiary = SageTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = OnSageContainer,
    background = AlabasterBackground,
    onBackground = InkOnLight,
    surface = SandSurface,
    onSurface = InkOnLight,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = MutedInkOnLight,
    surfaceContainerLowest = SandSurfaceElevated,
    surfaceContainerLow = AlabasterBackground,
    surfaceContainer = SandSurface,
    surfaceContainerHigh = SandSurfaceVariant,
    outline = WarmOutline,
    outlineVariant = Color(0xFFE4D8C8)
)

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaBright,
    onPrimary = Color(0xFF4F1206),
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFD8C2B8),
    onSecondary = Color(0xFF392923),
    secondaryContainer = DarkEspressoElevated,
    onSecondaryContainer = CreamOnDark,
    tertiary = SageBright,
    onTertiary = Color(0xFF063820),
    tertiaryContainer = DarkSageContainer,
    onTertiaryContainer = SageContainer,
    background = DarkCharcoalBackground,
    onBackground = CreamOnDark,
    surface = DarkEspressoSurface,
    onSurface = CreamOnDark,
    surfaceVariant = DarkEspressoElevated,
    onSurfaceVariant = MutedCreamOnDark,
    surfaceContainerLowest = DarkCharcoalBackground,
    surfaceContainerLow = DarkEspressoSurface,
    surfaceContainer = DarkEspressoElevated,
    surfaceContainerHigh = Color(0xFF392E28),
    outline = DarkWarmOutline,
    outlineVariant = Color(0xFF332924)
)

val SanuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SanuShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SanuTypography,
        shapes = SanuShapes,
        content = content
    )
}
