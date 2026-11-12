package app.roadio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.roadio.data.RouteRepository
import app.roadio.navi.NaviGateway
import app.roadio.ui.FindScreen
import app.roadio.ui.RoadioTheme
import app.roadio.ui.RouteScreen
import app.roadio.ui.StartScreen
import kotlinx.coroutines.delay

/** 1 목적지 확인 → 2 경로 찾는 중 → 3 경로 결과. "안내 시작"을 누르면 경유지를 등록하고 DriveActivity 로 넘어간다. */
class PlanActivity : ComponentActivity() {
    private enum class Step { START, FIND, ROUTE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val set = RouteRepository(this).load()
        val navi = Session.navi(this, set.destination)

        setContent {
            RoadioTheme {
                var step by remember { mutableStateOf(Step.START) }
                var dest by remember { mutableStateOf(set.destination) }      // 내비 목적지를 받기 전에는 데모 목적지
                var picked by remember { mutableStateOf(setOf("mountain", "sea")) }
                var findStep by remember { mutableIntStateOf(0) }
                var selectedId by remember { mutableStateOf(set.routes.first().id) }

                // 내비에 설정된 목적지를 가져온다. 실패하면 데모 목적지를 그대로 쓴다.
                LaunchedEffect(Unit) {
                    navi.start { ok -> if (ok) navi.requestDestination { p -> if (p != null && p.name.isNotBlank()) dest = p } }
                }

                when (step) {
                    Step.START -> StartScreen(
                        set, dest, picked,
                        onToggle = { id -> picked = if (id in picked) picked - id else picked + id },
                        onFind = { findStep = 0; step = Step.FIND },
                    )
                    Step.FIND -> {
                        // 1차 MVP: 결과는 이미 JSON 에 있으므로 단계만 차례로 보여 준다.
                        // 2차: 여기서 서버에 (출발지, 목적지, 테마)를 보내고 응답을 기다린다.
                        LaunchedEffect(Unit) {
                            repeat(3) { findStep = it; delay(900) }
                            step = Step.ROUTE
                        }
                        FindScreen(set, dest, picked, findStep, onCancel = { step = Step.START })
                    }
                    Step.ROUTE -> RouteScreen(
                        set, dest, selectedId,
                        onSelect = { selectedId = it },
                        onBack = { step = Step.START },
                        onStart = { startGuidance(navi, selectedId) },
                    )
                }
            }
        }
    }

    private fun startGuidance(navi: NaviGateway, routeId: String) {
        val route = RouteRepository(this).load().routes.first { it.id == routeId }
        // 경유지는 최대 2개. 길을 끌어오는 지점(steerWaypoint)만 등록하고, 슬롯 하나는 "들르기"용으로 남긴다.
        val steer = route.passPoints.filter { it.steerWaypoint }.take(NaviGateway.MAX_WAYPOINTS - 1)
        navi.clearWaypoints()
        steer.forEachIndexed { i, p -> navi.addWaypoint(i + 1, app.roadio.data.Place(p.name, p.lat, p.lng)) }

        if (route.passPoints.isEmpty()) { finish(); return }                  // 빠른 길: 내비 그대로
        startActivity(
            Intent(this, DriveActivity::class.java)
                .putExtra(DriveActivity.EXTRA_ROUTE, routeId)
                .putExtra(DriveActivity.EXTRA_WAYPOINTS, steer.size)
        )
        finish()
    }
}
