package app.roadio.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.roadio.R

/** 디자인 시안의 색. Pleos 공식 색상값을 받으면 여기만 바꾼다. */
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

/**
 * 화면 밝기 설정. 기본은 밝은 화면이고, 사용자가 바꾸면 그 값을 기억한다.
 * 기기의 어두운 화면 설정은 따르지 않는다(두 Activity 가 같은 값을 본다).
 */
object ThemeSetting {
    private const val PREFS = "roadio"
    private const val KEY_DARK = "dark"

    var dark by mutableStateOf(false)
        private set

    /** Activity 가 화면을 그리기 전에 한 번 부른다. */
    fun load(context: Context) {
        dark = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)
    }

    fun toggle(context: Context) {
        dark = !dark
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
    }
}

val LocalPalette = staticCompositionLocalOf { Light }

/**
 * 시안의 글꼴 Asta Sans (SIL Open Font License, docs/licenses/AstaSans-OFL.txt).
 * res/font/asta_sans.ttf 는 굵기를 축으로 가진 가변 글꼴 하나다. 화면에서 쓰는 굵기 네 가지를 같은 파일에서 뽑는다.
 */
@OptIn(ExperimentalTextApi::class)
val AstaSans = FontFamily(
    listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map { w ->
        Font(R.font.asta_sans, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    }
)

@Composable
fun RoadioTheme(content: @Composable () -> Unit) {
    // 글꼴을 따로 지정하지 않은 모든 Text 가 Asta Sans 로 그려진다.
    CompositionLocalProvider(
        LocalPalette provides if (ThemeSetting.dark) Dark else Light,
        LocalTextStyle provides TextStyle(fontFamily = AstaSans),
        content = content,
    )
}
