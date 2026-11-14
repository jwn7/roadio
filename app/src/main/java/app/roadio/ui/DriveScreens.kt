package app.roadio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.roadio.data.PassPoint
import app.roadio.data.Place
import app.roadio.data.Side
import app.roadio.data.ThemeRoute
import app.roadio.drive.DriveState
import app.roadio.drive.formatDistance

/** 창 폭이 이보다 넓으면 지점 목록을 오른쪽 칸으로 뺀다(Pleos 오른쪽 넓은 영역은 약 1680). */
private val WIDE_FROM = 1100.dp

/**
 * 주행 화면. 주행 중 보이는 화면이므로:
 * 글자는 28 이상, 한 화면에 3줄 이하 문장, 움직이는 요소 없음, 누를 수 있는 것은 버튼 하나.
 *
 * 창 폭에 따라 두 가지로 놓인다.
 * - 좁은 칸(세로 한 칸, 840 폭 기준): 방향·이름·거리가 위에 오고 그 아래에 목록이 이어진다.
 * - 넓은 영역: 왼쪽에 방향·이름·거리, 오른쪽 칸에 지점 목록. 아래가 비지 않게 풍경 그림이 채운다.
 */
@Composable
fun DriveScreen(
    route: ThemeRoute, destination: Place, state: DriveState, toast: String?, visitFor: Place?,
    onVisit: (Place) -> Unit, onVisitConfirm: (Boolean) -> Unit, onBackToNavi: () -> Unit,
) {
    val c = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val wide = maxWidth >= WIDE_FROM
        Row(Modifier.fillMaxSize().padding(start = 44.dp, top = 40.dp, end = 44.dp, bottom = 44.dp), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Header(route, state)
                Spacer(Modifier.height(36.dp))
                when (state) {
                    is DriveState.Approaching -> {
                        ApproachHead(state)
                        if (wide) Scenery(route.theme, Modifier.padding(top = 28.dp).fillMaxWidth().weight(1f))
                        else { Spacer(Modifier.height(12.dp)); PointList(route, state.index) ; Spacer(Modifier.weight(1f)) }
                    }
                    is DriveState.Guiding -> {
                        GuideHead(state.point)
                        Scenery(route.theme, Modifier.padding(vertical = 28.dp).fillMaxWidth().weight(1f))
                    }
                    is DriveState.Done -> {
                        Scenery("arrival", Modifier.fillMaxWidth().then(if (wide) Modifier.weight(1f) else Modifier.height(260.dp)))
                        DoneHead(destination)
                        if (!wide) { PassedList(route); Spacer(Modifier.weight(1f)) }
                    }
                }
                when (state) {
                    is DriveState.Done -> PrimaryButton("내비게이션으로 돌아가기", Modifier.padding(top = 28.dp).fillMaxWidth(), height = 120.dp, onClick = onBackToNavi)
                    else -> {
                        val visit = (state as? DriveState.Approaching)?.point?.visit ?: (state as? DriveState.Guiding)?.point?.visit
                        if (visit != null) VisitButton(visit, Modifier.padding(top = if (wide) 28.dp else 0.dp)) { onVisit(visit) }
                    }
                }
            }
            if (wide) {
                Column(Modifier.width(600.dp).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(c.panel).padding(horizontal = 36.dp, vertical = 28.dp)) {
                    if (state is DriveState.Done) PassedList(route) else PointList(route, state.index)
                    Spacer(Modifier.weight(1f))
                    // 이 화면이 넓게 떠 있다는 것은 내비게이션을 가리고 있다는 뜻이다. 나란히 놓는 방법을 알려 준다.
                    // (Pleos 는 앱이 스스로 한 칸으로 줄어드는 방법을 주지 않는다. docs/verify.md 5차·6차 실행)
                    Text(
                        "오른쪽 끝의 막대(⋮)를 왼쪽으로 끌면 내비게이션과 나란히 볼 수 있습니다.",
                        color = c.sub, fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, maxLines = 3,
                    )
                }
            }
        }
        if (toast != null) Toast(toast, Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 188.dp))
        if (visitFor != null) VisitPopup(visitFor, onVisitConfirm)
    }
}

@Composable
private fun Header(route: ThemeRoute, state: DriveState) {
    val c = LocalPalette.current
    Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LineIcon.forTheme(route.theme)?.let { LineIcon(it, c.accText, 32.dp, stroke = 2.5f) }
        Text(route.name, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        // 진행 표시는 본문과 같은 진하기로(흐리면 주행 중에 읽기 어렵다)
        Text("${minOf(state.index + 1, route.passPoints.size)} / ${route.passPoints.size}", color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SideArrow(side: Side, box: Int, icon: Int, inverted: Boolean) {
    val c = LocalPalette.current
    Box(Modifier.size(box.dp).clip(CircleShape).background(if (inverted) c.accOn else c.acc), contentAlignment = Alignment.Center) {
        Icon(
            if (side == Side.R) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null, tint = if (inverted) c.acc else c.accOn, modifier = Modifier.size(icon.dp),
        )
    }
}

// ---------- 5. 접근 중 ----------
@Composable
private fun ApproachHead(s: DriveState.Approaching) {
    val c = LocalPalette.current
    val (num, unit) = formatDistance(s.distanceM)
    Text("다음", color = c.sub, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    Text(s.point.name, color = c.text, fontSize = 104.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    Spacer(Modifier.height(28.dp))
    Rule()
    Row(Modifier.padding(vertical = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        SideArrow(s.point.side, 96, 60, inverted = false)
        Text(if (s.point.side == Side.R) "오른쪽" else "왼쪽", color = c.text, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        Text(num, color = c.text, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
        Text(unit, color = c.sub, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    }
    Rule()
}

/** 지점 목록: 지난 곳은 체크, 지금 향하는 곳은 번호 강조 */
@Composable
private fun PointList(route: ThemeRoute, current: Int) {
    val c = LocalPalette.current
    route.passPoints.forEachIndexed { i, p ->
        Row(Modifier.height(84.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            when {
                i < current -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = "지남", tint = c.sub, modifier = Modifier.size(30.dp))
                }
                i == current -> NumberDot(i + 1, filled = true)
                else -> Box(Modifier.size(40.dp).border(3.dp, c.line, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = c.sub, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(p.name, color = if (i == current) c.text else c.sub, fontSize = 30.sp, fontWeight = if (i == current) FontWeight.ExtraBold else FontWeight.SemiBold)
        }
        if (i < route.passPoints.lastIndex) Rule()
    }
}

// ---------- 6. 안내 중 ----------
@Composable
private fun GuideHead(p: PassPoint) {
    val c = LocalPalette.current
    // 띠 안의 화살표와 글자를 실제 방향 쪽에 붙인다. 글자를 읽기 전에 모양만으로 좌우가 구분된다.
    val right = p.side == Side.R
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.acc).padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp, if (right) Alignment.End else Alignment.Start),
    ) {
        if (!right) SideArrow(p.side, 144, 88, inverted = true)
        Text(if (right) "오른쪽 창밖" else "왼쪽 창밖", color = c.accOn, fontSize = 80.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        if (right) SideArrow(p.side, 144, 88, inverted = true)
    }
    Spacer(Modifier.height(40.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(p.name, color = c.text, fontSize = 104.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.weight(1f))
        // 음성이 나오는 중임을 알리는 표시. 움직이지 않는다.
        LineIcon(LineIcon.SPEAKER, c.accText, 56.dp, stroke = 2.2f)
    }
    Text(p.guideText, color = c.text, fontSize = 36.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, modifier = Modifier.padding(top = 16.dp))
}

/** 들르기 버튼. 주행 중 누르는 유일한 버튼이라 테두리와 강조색 아이콘으로 버튼임을 분명히 한다. */
@Composable
private fun VisitButton(visit: Place, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalPalette.current
    Row(
        modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(c.surf2)
            .border(3.dp, c.text, RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = c.accText, modifier = Modifier.size(44.dp))
        Text("  ${visit.name} 들르기", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------- 8. 모든 곳을 지남 ----------
@Composable
private fun ColumnScope.DoneHead(destination: Place) {
    val c = LocalPalette.current
    Text("길 위의 여행은 여기까지", color = c.text, fontSize = 64.sp, lineHeight = 76.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, modifier = Modifier.padding(top = 32.dp))
    Text("이제 ${destination.name}에서 새로운 여행이 시작됩니다.", color = c.text, fontSize = 36.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun PassedList(route: ThemeRoute) {
    val c = LocalPalette.current
    Text("오늘 지나온 곳", color = c.sub, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
    route.passPoints.forEach { p ->
        Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Filled.Check, contentDescription = "지남", tint = c.sub, modifier = Modifier.size(30.dp))
            Text(p.name, color = c.sub, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * 빈 자리를 채우는 풍경 그림(움직이지 않는다). theme: "sea" = 바다, "arrival" = 바다 위로 뜨는 해, 그 밖 = 산과 고갯길.
 * 실제 사진이 아니라 선과 면으로 그린 그림이다. 좌표는 752 x 376 기준으로 잡고 칸 크기에 맞춰 늘린다.
 */
@Composable
private fun Scenery(theme: String, modifier: Modifier = Modifier) {
    val c = LocalPalette.current
    Canvas(modifier.clip(RoundedCornerShape(24.dp)).background(c.surf2)) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x / 752f * w, y / 376f * h)
        val u = minOf(w / 752f, h / 376f)                      // 선 굵기·원 크기용 배율
        if (theme == "sea" || theme == "arrival") {
            val horizon = 210f
            val sun = p(376f, horizon)
            drawCircle(c.acc, 86f * u, sun)
            drawCircle(c.accText, 86f * u, sun, style = Stroke(3f * u))
            drawRect(c.sea, topLeft = p(0f, horizon), size = Size(w, h - p(0f, horizon).y))
            listOf(40f to 44f, 72f to 26f, 100f to 12f).forEachIndexed { i, (dy, half) ->
                drawLine(c.accText.copy(alpha = 1f - i * 0.2f), p(376f - half, horizon + dy), p(376f + half, horizon + dy), 6f * u, StrokeCap.Round)
            }
            listOf(60f to 262f, 560f to 290f, 150f to 322f, 500f to 340f).forEach { (x, y) ->
                val wave = Path().apply {
                    moveTo(p(x, y).x, p(x, y).y)
                    for (k in 0 until 3) {
                        val a = p(x + 22f + k * 44f, y - 12f); val b = p(x + 44f + k * 44f, y)
                        quadraticTo(a.x, a.y, b.x, b.y)
                    }
                }
                drawPath(wave, c.line, style = Stroke(4f * u, cap = StrokeCap.Round))
            }
        } else {
            drawCircle(c.acc, 34f * u, p(604f, 92f))
            drawCircle(c.accText, 34f * u, p(604f, 92f), style = Stroke(3f * u))
            val far = Path().apply {
                val xs = listOf(0f to 236f, 70f to 190f, 130f to 214f, 226f to 120f, 290f to 176f, 352f to 138f, 452f to 226f, 530f to 170f, 610f to 214f, 690f to 160f, 752f to 196f)
                moveTo(p(0f, 376f).x, p(0f, 376f).y)
                xs.forEach { (x, y) -> lineTo(p(x, y).x, p(x, y).y) }
                lineTo(w, h); close()
            }
            drawPath(far, c.line)
            val near = Path().apply {
                moveTo(0f, h); lineTo(p(0f, 300f).x, p(0f, 300f).y)
                cubicTo(p(90f, 250f).x, p(90f, 250f).y, p(170f, 292f).x, p(170f, 292f).y, p(262f, 268f).x, p(262f, 268f).y)
                cubicTo(p(360f, 242f).x, p(360f, 242f).y, p(430f, 300f).x, p(430f, 300f).y, p(530f, 276f).x, p(530f, 276f).y)
                cubicTo(p(620f, 256f).x, p(620f, 256f).y, p(690f, 286f).x, p(690f, 286f).y, p(752f, 262f).x, p(752f, 262f).y)
                lineTo(w, h); close()
            }
            drawPath(near, c.sub.copy(alpha = 0.45f))
            val road = Path().apply {
                moveTo(p(330f, 376f).x, p(330f, 376f).y)
                cubicTo(p(372f, 340f).x, p(372f, 340f).y, p(300f, 322f).x, p(300f, 322f).y, p(352f, 300f).x, p(352f, 300f).y)
                cubicTo(p(398f, 282f).x, p(398f, 282f).y, p(362f, 268f).x, p(362f, 268f).y, p(392f, 256f).x, p(392f, 256f).y)
            }
            drawPath(road, c.accText, style = Stroke(7f * u, cap = StrokeCap.Round))
            drawCircle(c.text, 9f * u, p(392f, 256f))
        }
    }
}

// ---------- 7. 들러보기 확인 (Pleos 시스템 팝업 구조: 제목 / 설명 / 취소·확인 가로 버튼) ----------
@Composable
private fun VisitPopup(place: Place, onResult: (Boolean) -> Unit) {
    val c = LocalPalette.current
    Box(Modifier.fillMaxSize().background(c.scrim).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 48.dp).width(744.dp).clip(RoundedCornerShape(28.dp)).background(c.panel).padding(start = 40.dp, top = 44.dp, end = 40.dp, bottom = 40.dp)) {
            Text("${place.name}에 들를까요?", color = c.text, fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold)
            Text("내비게이션 경유지로 추가합니다.", color = c.sub, fontSize = 30.sp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SecondaryButton("지나가기", Modifier.weight(1f)) { onResult(false) }
                PrimaryButton("들르기", Modifier.weight(1f), height = 104.dp) { onResult(true) }
            }
        }
    }
}
