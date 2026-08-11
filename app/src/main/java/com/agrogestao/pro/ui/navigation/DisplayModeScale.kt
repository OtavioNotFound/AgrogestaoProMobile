package com.agrogestao.pro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

internal const val SIMPLE_MODE_COMPONENT_SCALE = 1.16f
internal const val SIMPLE_MODE_ADDITIONAL_FONT_SCALE = 1.06f

internal fun displayModeComponentScale(simpleMode: Boolean): Float =
    if (simpleMode) SIMPLE_MODE_COMPONENT_SCALE else 1f

internal fun displayModeAdditionalFontScale(simpleMode: Boolean): Float =
    if (simpleMode) SIMPLE_MODE_ADDITIONAL_FONT_SCALE else 1f

/**
 * Makes hard-coded dp/sp values visibly larger without discarding the font scale
 * selected by the user in Android accessibility settings.
 */
@Composable
internal fun DisplayModeScale(
    simpleMode: Boolean,
    content: @Composable () -> Unit
) {
    val systemDensity = LocalDensity.current
    val componentScale = displayModeComponentScale(simpleMode)
    val additionalFontScale = displayModeAdditionalFontScale(simpleMode)
    val displayDensity = remember(systemDensity, componentScale, additionalFontScale) {
        Density(
            density = systemDensity.density * componentScale,
            fontScale = systemDensity.fontScale * additionalFontScale
        )
    }

    CompositionLocalProvider(LocalDensity provides displayDensity, content = content)
}
