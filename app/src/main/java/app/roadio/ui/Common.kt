package app.roadio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * 치수는 시안의 px 값을 그대로 dp/sp 로 옮겼다(앱 창 1700x1184 기준).
 * 에뮬레이터에서 크기가 다르게 보이면 이 파일과 각 화면의 숫자를 같은 비율로 조정한다.
 * 글꼴: 시안은 Asta Sans. res/font 에 파일을 넣고 FontFamily 로 지정하면 된다(지금은 시스템 글꼴).
 */

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, height: Dp = 112.dp, onClick: () -> Unit) {
    val c = LocalPalette.current
    Box(
        modifier.height(height).clip(RoundedCornerShape(20.dp)).background(c.acc).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.accOn, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun SecondaryButton(label: String, modifier: Modifier = Modifier, height: Dp = 104.dp, onClick: () -> Unit) {
    val c = LocalPalette.current
    Box(
        modifier.height(height).clip(RoundedCornerShape(20.dp)).background(c.surf2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, fontSize = 32.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun Rule() {
    Box(Modifier.fillMaxWidth().height(2.dp).background(LocalPalette.current.line))
}

@Composable
fun NumberDot(n: Int, filled: Boolean, size: Dp = 40.dp) {
    val c = LocalPalette.current
    Box(
        Modifier.size(size).clip(CircleShape).background(if (filled) c.acc else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) { Text("$n", color = if (filled) c.accOn else c.sub, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
}

/** Pleos 토스트 규격: 패딩 24, 최대 폭 792 */
@Composable
fun Toast(message: String, modifier: Modifier = Modifier) {
    val c = LocalPalette.current
    Box(
        modifier.widthIn(max = 792.dp).clip(RoundedCornerShape(20.dp)).background(c.toast).padding(horizontal = 40.dp, vertical = 24.dp)
    ) { Text(message, color = c.toastOn, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
}
