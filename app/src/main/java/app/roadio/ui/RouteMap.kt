package app.roadio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.roadio.data.Place
import app.roadio.data.ThemeRoute

/**
 * 개략 지도. 실제 지도 타일 없이 "어디서 무엇을 보는지"만 보여 준다(실제 지도는 내비게이션이 그린다).
 *
 * 볼거리는 대개 목적지 근처 마지막 구간에 몰려 있다(서울–양양은 마지막 40km). 축척대로 그리면 점이 한데 뭉쳐서
 * 구분이 안 되므로, 지점들이 있는 구간을 화면 오른쪽 넓은 칸에 확대해 그리고 출발지는 왼쪽 아래에 따로 둔다.
 * 그래서 이 그림의 거리 비율은 실제와 다르다.
 */
@Composable
fun RouteMap(origin: Place, destination: Place, selected: ThemeRoute?, modifier: Modifier = Modifier) {
    val c = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val numberStyle = TextStyle(color = c.accOn, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val o = Offset(w * 0.07f, h * 0.80f)                               // 출발지: 왼쪽 아래 고정

        // 확대해서 그릴 구간: 지나가는 지점들과 목적지. 지점이 없으면 목적지만.
        val detail = (selected?.passPoints?.map { it.lat to it.lng } ?: emptyList()) + (destination.lat to destination.lng)
        val minLat = detail.minOf { it.first }; val maxLat = detail.maxOf { it.first }
        val minLng = detail.minOf { it.second }; val maxLng = detail.maxOf { it.second }
        val left = w * 0.36f; val right = w * 0.86f
        val top = h * 0.16f; val bottom = h * 0.62f
        fun xy(lat: Double, lng: Double): Offset {
            // 범위가 거의 0이면(지점이 한 줄로 서 있으면) 가운데에 둔다.
            val fx = if (maxLng - minLng < 1e-4) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
            val fy = if (maxLat - minLat < 1e-4) 0.5f else ((lat - minLat) / (maxLat - minLat)).toFloat()
            return Offset(left + fx * (right - left), bottom - fy * (bottom - top))
        }
        val d = xy(destination.lat, destination.lng)

        // 동해(화면 오른쪽 끝)
        drawRect(c.sea, topLeft = Offset(w * 0.93f, 0f))

        // 빠른 길: 아래로 완만하게 휘는 선
        val fast = Path().apply {
            moveTo(o.x, o.y)
            cubicTo(w * 0.30f, h * 0.96f, w * 0.70f, h * 0.92f, d.x, d.y)
        }
        drawPath(fast, c.sub.copy(alpha = 0.7f), style = Stroke(6f, cap = StrokeCap.Round))

        // 선택한 테마 경로: 출발지에서 위로 휘어 들어와 지점들을 차례로 지난다. 빠른 길처럼 곡선으로 그려야
        // "직선거리"로 읽히지 않는다.
        val pts = selected?.passPoints?.map { xy(it.lat, it.lng) } ?: emptyList()
        if (pts.isNotEmpty()) {
            val path = Path().apply {
                moveTo(o.x, o.y)
                val first = pts.first()
                cubicTo(w * 0.16f, h * 0.44f, first.x - w * 0.14f, first.y + h * 0.04f, first.x, first.y)
                (pts.drop(1) + d).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, c.map, style = Stroke(22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, c.accText, style = Stroke(11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            // 같은 자리에 겹치는 점은 조금씩 비껴 놓는다(두 경로에 모두 나오는 낙산사는 목적지 바로 옆이다).
            val placed = mutableListOf<Offset>()
            pts.forEachIndexed { i, raw ->
                var at = raw
                while (placed.any { (it - at).getDistance() < 46f } || (d - at).getDistance() < 44f) at += Offset(-34f, -30f)
                placed += at
                // 밝은 배경에서도 보이도록 어두운 테두리를 두른다.
                drawCircle(c.map, 27f, at)
                drawCircle(c.accOn, 23f, at)
                drawCircle(c.acc, 20f, at)
                val label = measurer.measure("${i + 1}", numberStyle)
                drawText(label, topLeft = Offset(at.x - label.size.width / 2f, at.y - label.size.height / 2f))
            }
        }
        dot(o, 14f, c.text, c.map)
        dot(d, 18f, c.text, c.map)
    }
}

private fun DrawScope.dot(at: Offset, r: Float, fill: Color, ring: Color) {
    drawCircle(ring, r + 5f, at)
    drawCircle(fill, r, at)
}
