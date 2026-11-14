package app.roadio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

/**
 * 시안의 선 아이콘. 시안(HTML)의 SVG path 문자열을 그대로 옮겼다. 모두 24 x 24 칸 기준이고 선으로만 그린다.
 * 그림을 바꾸려면 시안의 path 를 여기에 붙여 넣으면 된다.
 */
enum class LineIcon(vararg val paths: String) {
    MOUNTAIN("m3 20 6-11 4 7 2-3 6 7H3z"),
    SEA("M2 8c2.5 0 2.5 2 5 2s2.5-2 5-2 2.5 2 5 2 2.5-2 5-2", "M2 14c2.5 0 2.5 2 5 2s2.5-2 5-2 2.5 2 5 2 2.5-2 5-2"),
    HISTORY("M3 10 12 4l9 6", "M5 10v9", "M19 10v9", "M9 19v-6h6v6", "M3 19h18"),
    FESTIVAL("M5 21V4", "M5 4c4-2 6 2 10 0s4 0 4 0v8s-1-2-4 0-6-2-10 0"),
    DESTINATION("m4 11 16-7-7 16-2-7-7-2z"),
    SPEAKER("M4 10v4h4l5 4V6L8 10H4z", "M16.5 9a4 4 0 0 1 0 6", "M19 6.5a7.5 7.5 0 0 1 0 11");

    companion object {
        /** 테마 id → 아이콘. 빠른 길처럼 테마가 없는 경로는 null */
        fun forTheme(theme: String): LineIcon? = when (theme) {
            "mountain" -> MOUNTAIN
            "sea" -> SEA
            "history" -> HISTORY
            "festival" -> FESTIVAL
            else -> null
        }
    }
}

/** [stroke] 는 24 칸 기준 선 굵기(시안의 stroke-width 와 같은 값). */
@Composable
fun LineIcon(icon: LineIcon, color: Color, size: Dp, modifier: Modifier = Modifier, stroke: Float = 2.4f) {
    val paths = remember(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            paths.forEach { drawPath(it, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        }
    }
}
