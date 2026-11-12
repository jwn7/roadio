package app.roadio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.roadio.data.PassPoint
import app.roadio.data.Place
import app.roadio.data.Side
import app.roadio.data.ThemeRoute
import app.roadio.drive.DriveState
import app.roadio.drive.formatDistance

/**
 * 주행 화면(세로 한 칸, 840 폭). 주행 중 보이는 화면이므로:
 * 글자는 28 이상, 한 화면에 3줄 이하 문장, 움직이는 요소 없음, 누를 수 있는 것은 버튼 하나.
 */
@Composable
fun DriveScreen(route: ThemeRoute, state: DriveState, toast: String?, visitFor: Place?, onVisit: (Place) -> Unit, onVisitConfirm: (Boolean) -> Unit) {
    val c = LocalPalette.current
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 44.dp, top = 40.dp, end = 44.dp, bottom = 44.dp)) {
            Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(route.name, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${minOf(state.index + 1, route.passPoints.size)} / ${route.passPoints.size}", color = c.sub, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(36.dp))
            when (state) {
                is DriveState.Approaching -> Approaching(route, state)
                is DriveState.Guiding -> Guiding(state.point)
                is DriveState.Done -> Text("모든 곳을 지났습니다", color = c.text, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            val visit = when (state) {
                is DriveState.Approaching -> state.point.visit
                is DriveState.Guiding -> state.point.visit
                is DriveState.Done -> null
            }
            if (visit != null) {
                Row(
                    Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(c.surf2).clickable { onVisit(visit) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = c.text, modifier = Modifier.size(40.dp))
                    Text("  ${visit.name} 들르기", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (toast != null) Toast(toast, Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 188.dp))
        if (visitFor != null) VisitPopup(visitFor, onVisitConfirm)
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
private fun Approaching(route: ThemeRoute, s: DriveState.Approaching) {
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
    Spacer(Modifier.height(12.dp))
    route.passPoints.forEachIndexed { i, p ->
        Row(Modifier.height(84.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            when {
                i < s.index -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = "지남", tint = c.sub, modifier = Modifier.size(30.dp))
                }
                i == s.index -> NumberDot(i + 1, filled = true)
                else -> Box(Modifier.size(40.dp).border(3.dp, c.line, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = c.sub, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(p.name, color = if (i == s.index) c.text else c.sub, fontSize = 30.sp, fontWeight = if (i == s.index) FontWeight.ExtraBold else FontWeight.SemiBold)
        }
        if (i < route.passPoints.lastIndex) Rule()
    }
}

// ---------- 6. 안내 중 ----------
@Composable
private fun Guiding(p: PassPoint) {
    val c = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.acc).padding(32.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        SideArrow(p.side, 144, 88, inverted = true)
        Text(if (p.side == Side.R) "오른쪽\n창밖" else "왼쪽\n창밖", color = c.accOn, fontSize = 72.sp, lineHeight = 84.sp, fontWeight = FontWeight.ExtraBold)
    }
    Spacer(Modifier.height(40.dp))
    Text(p.name, color = c.text, fontSize = 104.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    Text(p.guideText, color = c.text, fontSize = 36.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, modifier = Modifier.padding(top = 16.dp))
}

// ---------- 7. 들러보기 확인 (Pleos 시스템 팝업 구조: 제목 / 설명 / 취소·확인 가로 버튼) ----------
@Composable
private fun VisitPopup(place: Place, onResult: (Boolean) -> Unit) {
    val c = LocalPalette.current
    Box(Modifier.fillMaxSize().background(c.scrim).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 48.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.panel).padding(start = 40.dp, top = 44.dp, end = 40.dp, bottom = 40.dp)) {
            Text("${place.name}에 들를까요?", color = c.text, fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold)
            Text("내비게이션 경유지로 추가합니다.", color = c.sub, fontSize = 30.sp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SecondaryButton("지나가기", Modifier.weight(1f)) { onResult(false) }
                PrimaryButton("들르기", Modifier.weight(1f), height = 104.dp) { onResult(true) }
            }
        }
    }
}
