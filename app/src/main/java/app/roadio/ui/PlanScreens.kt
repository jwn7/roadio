package app.roadio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.roadio.data.PassPoint
import app.roadio.data.Place
import app.roadio.data.RouteSet
import app.roadio.data.Side
import app.roadio.data.ThemeRoute
import app.roadio.data.formatMinutes

val THEMES = listOf("mountain" to "산·자연", "sea" to "바다", "history" to "역사·문화", "festival" to "축제·행사")

/** 전체 지도 위에 왼쪽 패널(640)을 얹는 가로 화면 공통 틀 */
@Composable
private fun PlanFrame(set: RouteSet, selected: ThemeRoute?, candidates: List<PassPoint> = emptyList(), panel: @Composable ColumnScope.() -> Unit) {
    val c = LocalPalette.current
    Box(Modifier.fillMaxSize().background(c.map)) {
        RouteMap(set.origin, set.destination, selected, Modifier.fillMaxSize().padding(start = 700.dp, top = 80.dp, end = 40.dp, bottom = 80.dp), candidates)
        Column(
            Modifier.padding(32.dp).width(640.dp).fillMaxHeight()
                .clip(RoundedCornerShape(24.dp)).background(c.panel).padding(40.dp),
            content = panel,
        )
    }
}

@Composable
private fun DestinationHeader(dest: Place, onBack: (() -> Unit)? = null, onToggleTheme: (() -> Unit)? = null) {
    val c = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        if (onBack != null) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전", tint = c.text, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineIcon(LineIcon.DESTINATION, c.sub, 28.dp)
                Text("목적지", color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(dest.name, color = c.text, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (onToggleTheme != null) {
            val toDark = !ThemeSetting.dark
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(20.dp))
                    .clickable(onClickLabel = if (toDark) "어두운 화면으로 바꾸기" else "밝은 화면으로 바꾸기", onClick = onToggleTheme),
                contentAlignment = Alignment.Center,
            ) { ThemeIcon(moon = toDark, color = c.sub, cutout = c.panel) }
        }
    }
}

/** 화면 전환 버튼 그림. 밝은 화면에서는 달(어두운 화면으로), 어두운 화면에서는 해(밝은 화면으로). */
@Composable
private fun ThemeIcon(moon: Boolean, color: Color, cutout: Color) {
    Canvas(Modifier.size(44.dp)) {
        val r = size.minDimension / 2
        if (moon) {
            drawCircle(color, radius = r * 0.72f)
            drawCircle(cutout, radius = r * 0.62f, center = center + Offset(r * 0.36f, -r * 0.30f))
        } else {
            drawCircle(color, radius = r * 0.36f)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                drawLine(color, center + dir * (r * 0.62f), center + dir * (r * 0.92f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            }
        }
    }
}

// ---------- 1. 목적지 확인 ----------
@Composable
fun StartScreen(set: RouteSet, dest: Place, picked: Set<String>, onToggle: (String) -> Unit, onToggleTheme: () -> Unit, onFind: () -> Unit) {
    val c = LocalPalette.current
    PlanFrame(set, null) {
        DestinationHeader(dest, onToggleTheme = onToggleTheme)
        Text(
            listOf(dest.region, formatMinutes(set.fastMinutes)).filter { it.isNotBlank() }.joinToString(" · "),
            color = c.sub, fontSize = 28.sp, modifier = Modifier.padding(top = 4.dp),
        )
        Text("가는 길에 볼 것", color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 36.dp))
        // 경로 데이터가 있는 테마만 고를 수 있다. 없는 테마는 "준비 중"으로 보이고 눌리지 않는다.
        val ready = set.routes.map { it.theme }.toSet()
        THEMES.forEachIndexed { i, (id, name) ->
            val available = id in ready
            val on = available && id in picked
            Row(
                Modifier.fillMaxWidth().height(120.dp).then(if (available) Modifier.clickable { onToggle(id) } else Modifier),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LineIcon.forTheme(id)?.let { LineIcon(it, if (available) c.text else c.sub.copy(alpha = 0.6f), 48.dp, stroke = 2.2f) }
                Text(
                    name, color = if (available) c.text else c.sub.copy(alpha = 0.6f), fontSize = 36.sp,
                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                if (available) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .then(if (on) Modifier.background(c.acc) else Modifier.border(3.dp, c.line, RoundedCornerShape(12.dp))),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = c.accOn, modifier = Modifier.size(32.dp)) }
                } else {
                    Text(
                        "준비 중", color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.surf2).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (i < THEMES.lastIndex) Rule()
        }
        Spacer(Modifier.weight(1f))
        // 테마를 하나도 안 고르면 버튼을 숨기지 않고 그대로 두되 동작만 막는다(위치가 바뀌지 않게)
        PrimaryButton("경로 찾기", Modifier.fillMaxWidth()) { if (picked.isNotEmpty()) onFind() }
    }
}

// ---------- 2. 경로 찾는 중 ----------
@Composable
fun FindScreen(set: RouteSet, dest: Place, picked: Set<String>, step: Int, onCancel: () -> Unit) {
    val c = LocalPalette.current
    val steps = listOf("주변 명소 찾기", "창밖으로 보이는지 확인", "경로 만들기")
    // 첫 단계("주변 명소 찾기")가 끝나면 고른 테마의 지점들을 지도에 후보로 찍는다.
    val found = if (step >= 1) set.routes.filter { it.theme in picked }.flatMap { it.passPoints }.distinctBy { it.id } else emptyList()
    PlanFrame(set, null, found) {
        DestinationHeader(dest, onBack = onCancel)
        Text(THEMES.filter { it.first in picked }.joinToString(", ") { it.second }, color = c.sub, fontSize = 28.sp, modifier = Modifier.padding(top = 4.dp))
        // 진행 막대(정지 화면에서만 보이는 화면이라 단순 표시)
        Box(Modifier.padding(top = 44.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(c.line)) {
            Box(Modifier.fillMaxWidth(((step + 0.6f) / steps.size).coerceAtMost(1f)).fillMaxHeight().background(c.accText))
        }
        Spacer(Modifier.height(28.dp))
        steps.forEachIndexed { i, title ->
            Row(Modifier.height(88.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    when {
                        i < step -> Icon(Icons.Filled.Check, contentDescription = "완료", tint = c.accText, modifier = Modifier.size(40.dp))
                        i == step -> CircularProgressIndicator(color = c.accText, trackColor = c.line, strokeWidth = 6.dp, modifier = Modifier.size(40.dp))
                        else -> Box(Modifier.size(14.dp).clip(CircleShape).background(c.line))
                    }
                }
                Text(title, color = if (i == step) c.text else c.sub, fontSize = 34.sp, fontWeight = if (i == step) FontWeight.ExtraBold else FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton("취소", Modifier.fillMaxWidth(), onClick = onCancel)
    }
}

// ---------- 3. 경로 결과 ----------
@Composable
fun RouteScreen(set: RouteSet, dest: Place, selectedId: String, onSelect: (String) -> Unit, onBack: () -> Unit, onStart: () -> Unit) {
    val c = LocalPalette.current
    val selected = set.routes.first { it.id == selectedId }
    PlanFrame(set, selected) {
        DestinationHeader(dest, onBack = onBack)
        Spacer(Modifier.height(20.dp))
        set.routes.forEachIndexed { i, r ->
            val on = r.id == selectedId
            Column(Modifier.fillMaxWidth().clickable { onSelect(r.id) }) {
                Row(Modifier.height(104.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .then(if (on) Modifier.background(c.acc) else Modifier.border(3.dp, c.line, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = c.accOn, modifier = Modifier.size(28.dp)) }
                    Text(r.name, color = c.text, fontSize = 36.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    // 사람들이 실제로 비교하는 것은 빠른 길보다 얼마나 더 걸리는가다. 그래서 차이를 크게, 전체 시간을 작게.
                    val extra = r.minutes - set.fastMinutes
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (extra > 0) "+${formatMinutes(extra)}" else "가장 빠름",
                            color = if (on) c.text else c.sub, fontSize = 36.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                        )
                        Text(formatMinutes(r.minutes), color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (on && r.passPoints.isNotEmpty()) {
                    Column(Modifier.padding(start = 64.dp, bottom = 20.dp)) {
                        r.passPoints.forEachIndexed { n, p -> PassRow(n + 1, p) }
                        if (r.curvy) {
                            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CurvyIcon(c.warn)
                                Text("굽은 길 많음", color = c.warn, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (i < set.routes.lastIndex) Rule()
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("안내 시작", Modifier.fillMaxWidth(), onClick = onStart)
    }
}

/** 굽은 길 표시: S 자로 휘는 길 */
@Composable
private fun CurvyIcon(color: Color) {
    Canvas(Modifier.size(32.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.33f, h * 0.90f)
            cubicTo(w * 0.33f, h * 0.60f, w * 0.72f, h * 0.66f, w * 0.72f, h * 0.44f)
            cubicTo(w * 0.72f, h * 0.24f, w * 0.33f, h * 0.30f, w * 0.33f, h * 0.08f)
        }
        drawPath(path, color, style = Stroke(w * 0.12f, cap = StrokeCap.Round))
    }
}

@Composable
private fun PassRow(n: Int, p: PassPoint) {
    val c = LocalPalette.current
    Row(Modifier.height(60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        NumberDot(n, filled = true)
        Text(p.name, color = c.text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(
            if (p.side == Side.R) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null, tint = c.sub, modifier = Modifier.size(26.dp),
        )
        Text(if (p.side == Side.R) "오른쪽" else "왼쪽", color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    }
}
