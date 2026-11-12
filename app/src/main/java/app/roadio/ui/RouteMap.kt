package app.roadio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.roadio.data.Place
import app.roadio.data.ThemeRoute

/**
 * 개략 지도. 실제 지도 타일 없이 위경도를 화면에 비례 배치만 한다(1차 MVP 용).
 * 실제 지도는 내비게이션 앱이 그리므로, 여기서는 "어느 쪽으로 도는 길인지"만 보여 주면 된다.
 */
@Composable
fun RouteMap(origin: Place, destination: Place, selected: ThemeRoute?, modifier: Modifier = Modifier) {
    val c = LocalPalette.current
    Canvas(modifier) {
        val pts = listOf(origin.lat to origin.lng, destination.lat to destination.lng) +
            (selected?.passPoints?.map { it.lat to it.lng } ?: emptyList())
        val minLat = pts.minOf { it.first } - 0.15
        val maxLat = pts.maxOf { it.first } + 0.15
        val minLng = pts.minOf { it.second } - 0.1
        val maxLng = pts.maxOf { it.second } + 0.25
        fun xy(lat: Double, lng: Double) = Offset(
            ((lng - minLng) / (maxLng - minLng)).toFloat() * size.width,
            (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()) * size.height,
        )
        val o = xy(origin.lat, origin.lng)
        val d = xy(destination.lat, destination.lng)

        // 동해(목적지 오른쪽)
        drawRect(c.sea, topLeft = Offset(d.x + size.width * 0.03f, 0f))

        // 빠른 길: 출발지-목적지를 잇는 완만한 곡선
        val fast = Path().apply {
            moveTo(o.x, o.y)
            quadraticBezierTo((o.x + d.x) / 2, maxOf(o.y, d.y) + size.height * 0.18f, d.x, d.y)
        }
        drawPath(fast, c.sub.copy(alpha = 0.7f), style = Stroke(6f, cap = StrokeCap.Round))

        // 선택한 테마 경로: 지나가는 지점을 차례로 잇는다
        if (selected != null && selected.passPoints.isNotEmpty()) {
            val path = Path().apply {
                moveTo(o.x, o.y)
                selected.passPoints.forEach { p -> xy(p.lat, p.lng).let { lineTo(it.x, it.y) } }
                lineTo(d.x, d.y)
            }
            drawPath(path, c.map, style = Stroke(22f, cap = StrokeCap.Round))
            drawPath(path, c.accText, style = Stroke(11f, cap = StrokeCap.Round))
            selected.passPoints.forEach { p -> dot(xy(p.lat, p.lng), 20f, c.acc, c.map) }
        }
        dot(o, 14f, c.text, c.map)
        dot(d, 18f, c.text, c.map)
    }
}

private fun DrawScope.dot(at: Offset, r: Float, fill: androidx.compose.ui.graphics.Color, ring: androidx.compose.ui.graphics.Color) {
    drawCircle(ring, r + 5f, at)
    drawCircle(fill, r, at)
}
