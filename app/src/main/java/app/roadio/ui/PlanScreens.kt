package app.roadio.ui

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
private fun PlanFrame(set: RouteSet, selected: ThemeRoute?, panel: @Composable ColumnScope.() -> Unit) {
    val c = LocalPalette.current
    Box(Modifier.fillMaxSize().background(c.map)) {
        RouteMap(set.origin, set.destination, selected, Modifier.fillMaxSize().padding(start = 700.dp, top = 80.dp, end = 40.dp, bottom = 80.dp))
        Column(
            Modifier.padding(32.dp).width(640.dp).fillMaxHeight()
                .clip(RoundedCornerShape(24.dp)).background(c.panel).padding(40.dp),
            content = panel,
        )
    }
}

@Composable
private fun DestinationHeader(dest: Place, onBack: (() -> Unit)? = null) {
    val c = LocalPalette.current
    Row(verticalAlignment = Alignment.Top) {
        if (onBack != null) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전", tint = c.text, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        Column {
            Text("목적지", color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(dest.name, color = c.text, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ---------- 1. 목적지 확인 ----------
@Composable
fun StartScreen(set: RouteSet, dest: Place, picked: Set<String>, onToggle: (String) -> Unit, onFind: () -> Unit) {
    val c = LocalPalette.current
    PlanFrame(set, null) {
        DestinationHeader(dest)
        Text(
            listOf(dest.region, formatMinutes(set.fastMinutes)).filter { it.isNotBlank() }.joinToString(" · "),
            color = c.sub, fontSize = 28.sp, modifier = Modifier.padding(top = 4.dp),
        )
        Text("가는 길에 볼 것", color = c.sub, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 36.dp))
        THEMES.forEachIndexed { i, (id, name) ->
            val on = id in picked
            Row(
                Modifier.fillMaxWidth().height(120.dp).clickable { onToggle(id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, color = c.text, fontSize = 36.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .then(if (on) Modifier.background(c.acc) else Modifier.border(3.dp, c.line, RoundedCornerShape(12.dp))),
                    contentAlignment = Alignment.Center,
                ) { if (on) Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = c.accOn, modifier = Modifier.size(32.dp)) }
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
    PlanFrame(set, null) {
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
                    Text(formatMinutes(r.minutes), color = if (on) c.text else c.sub, fontSize = 36.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold)
                }
                if (on && r.passPoints.isNotEmpty()) {
                    Column(Modifier.padding(start = 64.dp, bottom = 20.dp)) {
                        r.passPoints.forEachIndexed { n, p -> PassRow(n + 1, p) }
                        if (r.curvy) Text("굽은 길 많음", color = c.warn, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            if (i < set.routes.lastIndex) Rule()
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("안내 시작", Modifier.fillMaxWidth(), onClick = onStart)
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
