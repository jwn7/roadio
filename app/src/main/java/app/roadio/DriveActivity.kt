package app.roadio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.roadio.data.Place
import app.roadio.data.RouteRepository
import app.roadio.drive.DriveState
import app.roadio.ui.DriveScreen
import app.roadio.ui.RoadioTheme
import app.roadio.ui.ThemeSetting
import kotlinx.coroutines.delay

/**
 * 5 주행 화면 / 6 안내 중 / 7 들러보기 확인.
 * 안내 자체(위치 판정과 음성)는 [GuideService] 가 돌린다. 이 화면은 그 상태를 보여 주고 "들르기"만 받는다.
 * 그래서 내비게이션에 가려져 있어도 안내가 이어지고, 돌아오면 지금 상태가 바로 보인다.
 */
class DriveActivity : ComponentActivity() {
    private lateinit var routeId: String

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) GuideService.start(this, routeId)        // 권한을 받았으니 GPS 로 다시 시작
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSetting.load(this)
        val set = RouteRepository(this).load()
        val route = set.routes.firstOrNull { it.id == (intent.getStringExtra(EXTRA_ROUTE) ?: GuideService.activeRoute) }
        if (route == null || route.passPoints.isEmpty()) { finish(); return }
        routeId = route.id
        openRoute = routeId
        val added = intent.getIntExtra(EXTRA_WAYPOINTS, 0)
        val navi = Session.navi(this, set.destination)

        GuideService.start(this, routeId)
        val located = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!located && !Session.SIMULATE_DRIVE) askLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            RoadioTheme {
                val live by GuideService.state.collectAsState()
                val state = live ?: DriveState.Approaching(0, route.passPoints.first(), 0)
                var toast by remember { mutableStateOf(if (added > 0) "경유지 ${added}곳을 추가했습니다" else null) }
                var visitFor by remember { mutableStateOf<Place?>(null) }

                LaunchedEffect(toast) { if (toast != null) { delay(3500); toast = null } }

                DriveScreen(
                    route, set.destination, state, toast, visitFor,
                    onBackToNavi = { backToNavi() },
                    onVisit = { visitFor = it },
                    onVisitConfirm = { yes ->
                        val place = visitFor
                        visitFor = null
                        if (yes && place != null) {
                            navi.addWaypoint(2, place)                          // 남겨 둔 두 번째 슬롯
                            toast = "${place.name} 경유지로 추가했습니다"
                        }
                    },
                )
            }
        }
    }

    /** 안내를 마치고 내비게이션 화면을 앞으로 부른다. 내비 앱을 찾지 못하면 화면만 닫는다. */
    private fun backToNavi() {
        packageManager.getLaunchIntentForPackage(NAVI_PACKAGE)?.let { runCatching { startActivity(it) } }
        finish()
    }

    override fun onStart() { super.onStart(); visible = true }
    override fun onStop() { visible = false; super.onStop() }

    override fun onDestroy() {
        // 사용자가 화면을 닫을 때만 안내를 끝낸다. 시스템이 화면을 정리한 경우에는 안내가 이어진다.
        if (isFinishing) {
            GuideService.stop(this)
            Session.release()
        }
        openRoute = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ROUTE = GuideService.EXTRA_ROUTE
        const val EXTRA_WAYPOINTS = "waypoints"

        /** 주행 화면이 열려 있는 경로 id. 안내가 끝난 뒤에도 화면을 닫기 전까지는 값이 남는다 */
        var openRoute: String? = null
            private set
        /** 주행 화면이 지금 보이는가. 보이는 동안에는 서비스가 내비게이션 위 카드를 띄우지 않는다 */
        var visible = false
            private set
        /** Pleos Connect v2.1.0 에뮬레이터의 내비게이션 앱 (adb 로 확인) */
        private const val NAVI_PACKAGE = "ai.umos.maps.android.navigation.app"
    }
}
