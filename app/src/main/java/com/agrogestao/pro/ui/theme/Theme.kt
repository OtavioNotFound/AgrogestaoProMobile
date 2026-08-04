package com.agrogestao.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAgroGreen,
    onPrimary = SurfaceCard,
    secondary = SecondaryAgroGreen,
    onSecondary = SurfaceCard,
    tertiary = AccentEarthOrange,
    background = BackgroundLight,
    surface = SurfaceCard,
    surfaceVariant = SurfaceSoft,
    onBackground = TextDark,
    onSurface = TextDark,
    outline = CardBorder,
    error = StatusOrange
)

private val AgroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun AgroGestaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AgroTypography,
        shapes = AgroShapes,
        content = content
    )
}
