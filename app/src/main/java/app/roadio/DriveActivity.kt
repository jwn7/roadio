package app.roadio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.roadio.audio.Narrator
import app.roadio.data.Place
import app.roadio.data.RouteRepository
import app.roadio.drive.DriveEngine
import app.roadio.drive.DriveState
import app.roadio.navi.LocationSource
import app.roadio.ui.DriveScreen
import app.roadio.ui.RoadioTheme
import kotlinx.coroutines.delay

/** 5 주행 화면 / 6 안내 중 / 7 들러보기 확인. 내비게이션 옆 세로 한 칸에 놓인다. */
class DriveActivity : ComponentActivity() {
    private lateinit var narrator: Narrator
    private var locationGranted by mutableStateOf(false)

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { locationGranted = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val set = RouteRepository(this).load()
        val route = set.routes.first { it.id == intent.getStringExtra(EXTRA_ROUTE) }
        val added = intent.getIntExtra(EXTRA_WAYPOINTS, 0)
        val navi = Session.navi(this, set.destination)
        val engine = DriveEngine(route.passPoints)
        narrator = Narrator(this)

        locationGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!locationGranted && !Session.SIMULATE_DRIVE) askLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            RoadioTheme {
                var state by remember { mutableStateOf<DriveState>(DriveState.Approaching(0, route.passPoints.first(), 0)) }
                var toast by remember { mutableStateOf(if (added > 0) "경유지 ${added}곳을 추가했습니다" else null) }
                var visitFor by remember { mutableStateOf<Place?>(null) }
                var spokenId by remember { mutableStateOf<String?>(null) }

                // 위치 → 판정 → 화면. 안내 구간에 처음 들어올 때 한 번만 음성을 튼다.
                LaunchedEffect(locationGranted) {
                    val fixes = when {
                        Session.SIMULATE_DRIVE -> LocationSource.simulated(route.passPoints)
                        locationGranted -> LocationSource.gps(this@DriveActivity)
                        else -> return@LaunchedEffect
                    }
                    fixes.collect { f ->
                        val s = engine.update(f.lat, f.lng)
                        state = s
                        if (s is DriveState.Guiding && spokenId != s.point.id) {
                            spokenId = s.point.id
                            narrator.play(s.point)
                        }
                    }
                }
                LaunchedEffect(toast) { if (toast != null) { delay(3500); toast = null } }

                DriveScreen(
                    route, state, toast, visitFor,
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

    override fun onDestroy() {
        narrator.release()
        if (isFinishing) Session.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ROUTE = "route"
        const val EXTRA_WAYPOINTS = "waypoints"
    }
}
