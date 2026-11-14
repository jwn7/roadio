package app.roadio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.roadio.data.PassPoint
import app.roadio.data.Place
import app.roadio.data.ThemeRoute

/**
 * 개략 지도. 실제 지도 타일 없이 "어디서 무엇을 보는지"만 보여 준다(실제 지도는 내비게이션이 그린다).
 *
 * 볼거리는 대개 목적지 근처 마지막 구간에 몰려 있다(서울–양양은 마지막 40km). 축척대로 그리면 점이 한데 뭉쳐서
 * 구분이 안 되므로, 지점들이 있는 구간을 화면 오른쪽 넓은 칸에 확대해 그리고 출발지는 왼쪽 아래에 따로 둔다.
 * 그래서 이 그림의 거리 비율은 실제와 다르다.
 *
 * 등고선 모양과 해안선은 분위기를 위한 그림이다. 실제 지형이 아니며 어떤 목적지에서도 같은 모양으로 그려진다.
 * 글자는 모두 데이터에서 온다(출발지·목적지·지점 이름). 시안에 있는 도시 이름과 도로 번호는 데이터에 없어 그리지 않는다.
 *
 * [candidates] 는 "경로 찾는 중" 화면에서 찾은 후보 지점이다. 작은 점으로만 찍는다.
 */
@Composable
fun RouteMap(
    origin: Place, destination: Place, selected: ThemeRoute?, modifier: Modifier = Modifier,
    candidates: List<PassPoint> = emptyList(),
) {
    val c = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val numberStyle = TextStyle(color = c.accOn, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = AstaSans)
    val pointStyle = TextStyle(color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AstaSans)
    val placeStyle = TextStyle(color = c.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = AstaSans)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val o = Offset(w * 0.07f, h * 0.80f)                               // 출발지: 왼쪽 아래 고정

        // 확대해서 그릴 구간: 지나가는 지점들(또는 후보 지점들)과 목적지. 지점이 없으면 목적지만.
        val shown = selected?.passPoints ?: candidates
        val detail = shown.map { it.lat to it.lng } + (destination.lat to destination.lng)
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

        // 바다: 화면 오른쪽 끝. 직선이 아니라 굽은 해안선으로.
        val coast = Path().apply {
            moveTo(w * 0.93f, 0f)
            cubicTo(w * 0.905f, h * 0.18f, w * 0.95f, h * 0.36f, w * 0.93f, h * 0.55f)
            cubicTo(w * 0.915f, h * 0.72f, w * 0.96f, h * 0.88f, w * 0.94f, h)
            lineTo(w, h); lineTo(w, 0f); close()
        }
        drawPath(coast, c.sea)

        // 등고선 모양(장식): 확대 구간 가운데에 큰 것 하나, 왼쪽 위와 아래에 작은 것 둘.
        val contour = c.line.copy(alpha = 0.7f)
        contours(Offset((left + right) / 2f, (top + bottom) / 2f), w * 0.26f, h * 0.36f, -14f, 5, contour)
        contours(Offset(w * 0.24f, h * 0.30f), w * 0.13f, h * 0.13f, 20f, 2, contour)
        contours(Offset(w * 0.34f, h * 0.94f), w * 0.16f, h * 0.11f, -8f, 2, contour)

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
            pts.forEach { raw ->
                var at = raw
                while (placed.any { (it - at).getDistance() < 46f } || (d - at).getDistance() < 44f) at += Offset(-34f, -30f)
                placed += at
            }
            placed.forEachIndexed { i, at ->
                // 밝은 배경에서도 보이도록 어두운 테두리를 두른다.
                drawCircle(c.map, 27f, at)
                drawCircle(c.accOn, 23f, at)
                drawCircle(c.acc, 20f, at)
                val label = measurer.measure("${i + 1}", numberStyle)
                drawText(label, topLeft = Offset(at.x - label.size.width / 2f, at.y - label.size.height / 2f))
            }
            // 지점 이름("설악산 장수대" → "장수대"). 점이나 다른 이름과 겹치지 않는 자리를 위·아래·오른쪽·왼쪽 순으로 찾는다.
            val taken = (placed + d + o).map { Rect(it.x - 30f, it.y - 30f, it.x + 30f, it.y + 30f) }.toMutableList()
            placed.forEachIndexed { i, at ->
                val text = measurer.measure(selected!!.passPoints[i].name.substringAfterLast(' '), pointStyle)
                val tw = text.size.width.toFloat(); val th = text.size.height.toFloat()
                val spots = listOf(
                    Offset(at.x - tw / 2f, at.y - 32f - th), Offset(at.x - tw / 2f, at.y + 32f),
                    Offset(at.x + 36f, at.y - th / 2f), Offset(at.x - 36f - tw, at.y - th / 2f),
                )
                val free = spots.firstOrNull { sp -> taken.none { it.overlaps(Rect(sp, Size(tw, th))) } } ?: spots.first()
                taken += Rect(free, Size(tw, th))
                haloText(text, pointStyle.color, c.map, free)
            }
        } else {
            // 경로를 고르기 전: 찾은 후보 지점을 작은 점으로만 보여 준다.
            candidates.forEach { p ->
                val at = xy(p.lat, p.lng)
                drawCircle(c.map, 19f, at)
                drawCircle(c.acc, 14f, at)
                drawCircle(c.accText, 14f, at, style = Stroke(2.5f))
            }
        }
        dot(o, 14f, c.text, c.map)
        dot(d, 18f, c.text, c.map)
        drawCircle(c.map, 6f, d)
        label(measurer, origin.name, placeStyle, c.map, o, above = false, gap = 24f)
        label(measurer, destination.name, placeStyle, c.map, d, above = false, gap = 30f, alignEnd = true)
    }
}

/** 한 점을 가운데에 둔 겹겹의 타원(등고선 모양). */
private fun DrawScope.contours(center: Offset, rx: Float, ry: Float, degrees: Float, rings: Int, color: Color) {
    rotate(degrees, center) {
        for (k in 0 until rings) {
            val f = 1f - k.toFloat() / (rings + 0.4f)
            drawOval(color, topLeft = Offset(center.x - rx * f, center.y - ry * f), size = Size(rx * 2 * f, ry * 2 * f), style = Stroke(3f))
        }
    }
}

/** 글자 뒤에 지도 바탕색 테두리를 둘러 선 위에서도 읽히게 한다. [alignEnd] 면 글자의 오른쪽 끝을 점에 맞춘다. */
private fun DrawScope.label(
    measurer: TextMeasurer, text: String, style: TextStyle, halo: Color, at: Offset, above: Boolean, gap: Float, alignEnd: Boolean = false,
) {
    val fill = measurer.measure(text, style)
    val x = if (alignEnd) at.x - fill.size.width + 8f else at.x - fill.size.width / 2f
    val y = if (above) at.y - gap - fill.size.height else at.y + gap
    val topLeft = Offset(x.coerceIn(4f, size.width - fill.size.width - 4f), y)
    haloText(fill, style.color, halo, topLeft)
}

/**
 * 글자 뒤에 [halo] 색 테두리를 두른다. 같은 측정 결과를 두 번 그린다: 먼저 굵은 선(테두리)으로, 그 위에 본래 색으로 채워서.
 * 두 번째에 Fill 을 명시해야 한다. 생략하면 앞에서 쓴 Stroke 가 그대로 남아 본문 글자까지 굵은 선으로 뭉개진다.
 */
private fun DrawScope.haloText(text: TextLayoutResult, color: Color, halo: Color, topLeft: Offset) {
    drawText(text, color = halo, topLeft = topLeft, drawStyle = Stroke(8f, join = StrokeJoin.Round))
    drawText(text, color = color, topLeft = topLeft, drawStyle = Fill)
}

private fun DrawScope.dot(at: Offset, r: Float, fill: Color, ring: Color) {
    drawCircle(ring, r + 5f, at)
    drawCircle(fill, r, at)
}
