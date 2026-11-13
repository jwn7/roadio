package app.roadio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.roadio.data.Place
import app.roadio.data.RouteRepository
import app.roadio.navi.NaviGateway
import app.roadio.ui.FindScreen
import app.roadio.ui.RoadioTheme
import app.roadio.ui.RouteScreen
import app.roadio.ui.StartScreen
import app.roadio.ui.ThemeSetting
import kotlinx.coroutines.delay

/** 1 목적지 확인 → 2 경로 찾는 중 → 3 경로 결과. "안내 시작"을 누르면 경유지를 등록하고 DriveActivity 로 넘어간다. */
class PlanActivity : ComponentActivity() {
    private enum class Step { START, FIND, ROUTE }

    // 내비게이션 위 안내 카드는 알림이다. 정차 중인 첫 화면에서 권한을 받아 둔다. 거절해도 앱은 동작한다.
    private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSetting.load(this)
        // 안내 중에 앱을 다시 열면(하단 바 등) 계획 화면이 아니라 주행 화면으로 보낸다.
        // 안내가 끝나 서비스가 내려간 뒤에도 주행 화면(마무리 화면)이 열려 있으면 그쪽으로 보낸다.
        (GuideService.activeRoute ?: DriveActivity.openRoute)?.let { active ->
            startActivity(
                Intent(this, DriveActivity::class.java).putExtra(DriveActivity.EXTRA_ROUTE, active)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
            return
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val set = RouteRepository(this).load()
        val navi = Session.navi(this, set.destination)

        setContent {
            RoadioTheme {
                var step by remember { mutableStateOf(Step.START) }
                var dest by remember { mutableStateOf(set.destination) }      // 내비 목적지를 받기 전에는 데모 목적지
                var picked by remember { mutableStateOf(setOf("mountain", "sea")) }
                var findStep by remember { mutableIntStateOf(0) }
                var selectedId by remember { mutableStateOf(set.routes.first().id) }
                // 고른 테마의 경로만 결과에 보인다. 빠른 길은 비교용으로 항상 보인다.
                val shown = set.copy(routes = set.routes.filter { it.theme in picked || it.theme == FAST })

                // 내비에 설정된 목적지를 가져온다. 실패하면 데모 목적지를 그대로 쓴다.
                LaunchedEffect(Unit) {
                    navi.start { ok -> if (ok) navi.requestDestination { p -> if (p != null && p.name.isNotBlank()) dest = p } }
                }

                when (step) {
                    Step.START -> StartScreen(
                        set, dest, picked,
                        onToggle = { id -> picked = if (id in picked) picked - id else picked + id },
                        onToggleTheme = { ThemeSetting.toggle(this@PlanActivity) },
                        onFind = { findStep = 0; step = Step.FIND },
                    )
                    Step.FIND -> {
                        // 1차 MVP: 결과는 이미 JSON 에 있으므로 단계만 차례로 보여 준다.
                        // 2차: 여기서 서버에 (출발지, 목적지, 테마)를 보내고 응답을 기다린다.
                        LaunchedEffect(Unit) {
                            repeat(3) { findStep = it; delay(900) }
                            selectedId = shown.routes.first().id
                            step = Step.ROUTE
                        }
                        FindScreen(set, dest, picked, findStep, onCancel = { step = Step.START })
                    }
                    Step.ROUTE -> RouteScreen(
                        shown, dest, selectedId,
                        onSelect = { selectedId = it },
                        onBack = { step = Step.START },
                        onStart = { startGuidance(navi, selectedId, dest) },
                    )
                }
            }
        }
    }

    private companion object {
        const val FAST = "fast"
    }

    private fun startGuidance(navi: NaviGateway, routeId: String, dest: Place) {
        val route = RouteRepository(this).load().routes.first { it.id == routeId }
        // 경유지는 최대 2개. 길을 끌어오는 지점(steerWaypoint)만 등록하고, 슬롯 하나는 "들르기"용으로 남긴다.
        val wanted = route.passPoints.filter { it.steerWaypoint }
        val steer = wanted.take(NaviGateway.MAX_WAYPOINTS - 1)
        if (wanted.size > steer.size) {
            android.util.Log.w("PlanActivity", "경유지로 등록하지 않는 지점: ${wanted.drop(steer.size).map { it.name }} (들르기용 슬롯을 남김)")
        }
        // 목적지와 경유지를 한 번에 넘긴다. 내비에 목적지가 없어도 경로가 만들어진다.
        navi.startRoute(dest, steer.map { Place(it.name, it.lat, it.lng) })

        if (route.passPoints.isEmpty()) { finish(); return }                  // 빠른 길: 내비 그대로
        // 안내 시작 뒤에는 내비게이션이 앞에 남고, 이 앱은 그 위에 카드(헤드업 알림)로 나타난다.
        // 주행 화면은 카드를 누르거나 앱을 다시 열었을 때 보인다.
        if (GuideService.canShowCards(this)) {
            GuideService.start(this, routeId)
            finish()
            return
        }
        // 알림을 못 띄우면 카드가 안 보이므로 예전처럼 주행 화면을 앞에 띄운다. 자기 작업(task)으로 띄운다.
        startActivity(
            Intent(this, DriveActivity::class.java)
                .putExtra(DriveActivity.EXTRA_ROUTE, routeId)
                .putExtra(DriveActivity.EXTRA_WAYPOINTS, steer.size)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
