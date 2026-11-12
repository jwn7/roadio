package app.roadio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import app.roadio.audio.Narrator
import app.roadio.data.RouteRepository
import app.roadio.drive.DriveEngine
import app.roadio.drive.DriveState
import app.roadio.navi.LocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 창밖 안내를 화면과 따로 돌린다: 위치 → 판정(DriveEngine) → 음성(Narrator).
 *
 * "안내 시작"을 누르면 내비게이션이 앞으로 나오고 우리 화면은 가려진다. 화면(Activity) 안에서
 * 이 반복을 돌리면 몇 초 뒤 시스템이 프로세스를 얼려서 안내와 음성이 멈춘다(docs/verify.md).
 * 포그라운드 서비스로 돌리면 화면이 가려져도 계속된다. 화면은 [state] 를 보고 그리기만 한다.
 */
class GuideService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var narrator: Narrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ROUTE) ?: activeRoute
        if (id == null) { stopSelf(); return START_NOT_STICKY }

        val located = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val useGps = !Session.SIMULATE_DRIVE
        startInForeground(id, withLocation = useGps && located)

        if (id == activeRoute && job?.isActive == true) return START_NOT_STICKY      // 이미 이 경로로 안내 중
        job?.cancel()
        activeRoute = id

        val route = RouteRepository(this).load().routes.firstOrNull { it.id == id }
        if (route == null || route.passPoints.isEmpty()) { stopSelf(); return START_NOT_STICKY }
        _state.value = DriveState.Approaching(0, route.passPoints.first(), 0)
        if (useGps && !located) return START_NOT_STICKY                              // 권한을 받으면 화면이 다시 start 를 부른다

        val voice = narrator ?: Narrator(this).also { narrator = it }
        val engine = DriveEngine(route.passPoints)
        val fixes = if (useGps) LocationSource.gps(this) else LocationSource.simulated(route.passPoints)
        job = scope.launch {
            var spokenId: String? = null
            var finishing = false
            fixes.collect { f ->
                val s = engine.update(f.lat, f.lng)
                _state.value = s
                // 안내 구간에 처음 들어올 때 한 번만 음성을 튼다.
                if (s is DriveState.Guiding && spokenId != s.point.id) {
                    spokenId = s.point.id
                    voice.play(s.point)
                }
                // 마지막 지점을 지나면 마지막 음성이 끝날 시간을 두고 서비스를 내린다.
                if (s is DriveState.Done && !finishing) {
                    finishing = true
                    scope.launch { delay(LAST_AUDIO_MS); stopSelf() }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "stop $activeRoute")
        scope.cancel()
        narrator?.release()
        narrator = null
        activeRoute = null          // state 값은 남겨 둔다(화면이 "모두 지남"을 계속 보여 줄 수 있게)
        super.onDestroy()
    }

    private fun startInForeground(routeId: String, withLocation: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "창밖 안내", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, DriveActivity::class.java).putExtra(EXTRA_ROUTE, routeId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("창밖 안내 중")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            // 위치 유형은 위치 권한이 있을 때만 붙일 수 있다(없이 붙이면 API 34 에서 예외).
            if (withLocation) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "GuideService"
        private const val CHANNEL = "guide"
        private const val NOTIFICATION_ID = 1
        private const val LAST_AUDIO_MS = 30_000L
        const val EXTRA_ROUTE = "route"

        private val _state = MutableStateFlow<DriveState?>(null)
        /** 지금 안내 상태. 한 번도 안내한 적이 없으면 null */
        val state: StateFlow<DriveState?> = _state

        /** 안내 중인 경로 id. 안내 중이 아니면 null */
        var activeRoute: String? = null
            private set

        /** 이 경로로 안내를 시작한다. 같은 경로로 이미 안내 중이면 아무 일도 없다. */
        fun start(context: Context, routeId: String) {
            if (routeId != activeRoute) _state.value = null          // 이전 경로의 상태가 잠깐 보이지 않게
            context.startForegroundService(Intent(context, GuideService::class.java).putExtra(EXTRA_ROUTE, routeId))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuideService::class.java))
        }
    }
}
