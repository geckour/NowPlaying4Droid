package com.geckour.nowplaying4gpm.app.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = DeepRed,
    primaryVariant = DarkRed,
    secondary = CleanBlue,
    secondaryVariant = CleanBlue,
    surface = MilkWhite,
    background = TarBlack,
    onPrimary = Color.White,
    onSurface = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
)

private val LightColorPalette = lightColors(
    primary = LightRed,
    primaryVariant = DarkRed,
    secondary = DeepBlue,
    secondaryVariant = CleanBlue,
    surface = MilkWhite,
    onPrimary = Color.White,
    onSurface = InkBlack,
    onSecondary = Color.White,
    onBackground = InkBlack,
)

@Composable
fun SettingsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}