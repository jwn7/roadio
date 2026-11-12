package app.roadio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 디자인 시안(v4)의 색. Pleos 공식 색상값을 받으면 여기만 바꾼다. */
data class Palette(
    val bg: Color, val map: Color, val panel: Color, val surf2: Color, val line: Color, val road: Color,
    val text: Color, val sub: Color, val acc: Color, val accOn: Color, val accText: Color, val warn: Color,
    val sea: Color, val scrim: Color, val toast: Color, val toastOn: Color,
)

val Dark = Palette(
    bg = Color(0xFF0D1116), map = Color(0xFF10151B), panel = Color(0xFF181E26), surf2 = Color(0xFF232B35),
    line = Color(0xFF2E3844), road = Color(0xFF222B35), text = Color(0xFFF3F5F7), sub = Color(0xFFA9B2BD),
    acc = Color(0xFFF9F88D), accOn = Color(0xFF16150A), accText = Color(0xFFF9F88D), warn = Color(0xFFFFB95C),
    sea = Color(0xFF0F1F2D), scrim = Color(0xB8040609), toast = Color(0xFFF3F5F7), toastOn = Color(0xFF12160F),
)
val Light = Palette(
    bg = Color(0xFFF4F5F2), map = Color(0xFFE8EBE4), panel = Color(0xFFFFFFFF), surf2 = Color(0xFFEEF0EB),
    line = Color(0xFFD3D8CF), road = Color(0xFFFFFFFF), text = Color(0xFF12160F), sub = Color(0xFF4B5548),
    acc = Color(0xFFF9F88D), accOn = Color(0xFF16150A), accText = Color(0xFF6B6400), warn = Color(0xFF8A5200),
    sea = Color(0xFFD3E3EB), scrim = Color(0x8C12160F), toast = Color(0xFF12160F), toastOn = Color(0xFFF3F5F7),
)

val LocalPalette = staticCompositionLocalOf { Dark }

@Composable
fun RoadioTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides if (isSystemInDarkTheme()) Dark else Light, content = content)
}
