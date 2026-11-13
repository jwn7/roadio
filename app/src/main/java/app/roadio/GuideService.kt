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
import app.roadio.data.PassPoint
import app.roadio.data.RouteRepository
import app.roadio.data.Side
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
    private var points: List<PassPoint> = emptyList()
    private var destinationName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_VISIT) { visit(intent.getStringExtra(EXTRA_POINT)); return START_NOT_STICKY }
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
        points = route.passPoints
        destinationName = RouteRepository(this).load().destination.name
        post(id, "${route.name} 안내를 시작합니다", "첫 지점은 ${route.passPoints.first().name}입니다. 가까워지면 알려 드립니다.")
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
                // 안내 구간에 처음 들어올 때 한 번만 음성을 틀고, 내비게이션 위로 안내 카드를 띄운다.
                if (s is DriveState.Guiding && spokenId != s.point.id) {
                    spokenId = s.point.id
                    voice.play(s.point)
                    showCard(id, s.point)
                }
                // 마지막 지점을 지나면 마지막 음성이 끝날 시간을 두고 서비스를 내린다.
                if (s is DriveState.Done && !finishing) {
                    finishing = true
                    post(id, "길 위의 여행은 여기까지", "이제 ${destinationName}에서 새로운 여행이 시작됩니다.")
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
        getSystemService(NotificationManager::class.java).cancel(CARD_ID)
        super.onDestroy()
    }

    /**
     * 지점 안내 카드: "오른쪽 창밖 · 한계령" + 짧은 설명 + (들를 곳이 있으면) 들르기 버튼.
     */
    private fun showCard(routeId: String, point: PassPoint) {
        val side = if (point.side == Side.R) "오른쪽" else "왼쪽"
        post(routeId, "$side 창밖 · ${point.name}", point.guideText, point)
    }

    /**
     * 내비게이션 위에 카드를 띄운다(헤드업 알림). 안내 시작 뒤에는 내비게이션이 화면을 차지하므로,
     * 달리는 동안 이 앱이 보이는 곳은 이 카드다. Pleos 가 화면 맨 위 층에 그려서 어떤 앱이 앞에 있어도 보인다.
     *
     * - 주행 화면이 보이는 중이면 띄우지 않는다. 같은 내용이 화면에 이미 있다.
     * - 소리는 Narrator 가 내므로 알림은 소리를 내지 않는다.
     * - 카드는 내비게이션의 회전 안내 자리를 덮으므로 [CARD_MS] 뒤에 스스로 내려간다.
     * - 카드를 누르면 주행 화면이 열린다.
     */
    private fun post(routeId: String, title: String, text: String, point: PassPoint? = null) {
        if (DriveActivity.visible) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CARD_CHANNEL, "지점 안내", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            }
        )
        val card = Notification.Builder(this, CARD_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setContentIntent(openDrive(routeId))
            .setAutoCancel(true)
            .setTimeoutAfter(CARD_MS)
        point?.visit?.let { place ->
            val go = PendingIntent.getService(
                this, 1,
                Intent(this, GuideService::class.java).setAction(ACTION_VISIT).putExtra(EXTRA_POINT, point.id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            card.addAction(Notification.Action.Builder(null, "${place.name} 들르기", go).build())
        }
        nm.notify(CARD_ID, card.build())
    }

    /** 카드의 "들르기": 남겨 둔 두 번째 경유지 칸에 그 지점의 들를 곳을 넣는다. */
    private fun visit(pointId: String?) {
        val place = points.firstOrNull { it.id == pointId }?.visit ?: return
        Log.d(TAG, "visit ${place.name}")
        Session.navi(this, null).addWaypoint(2, place)
        getSystemService(NotificationManager::class.java).cancel(CARD_ID)
    }

    private fun openDrive(routeId: String): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, DriveActivity::class.java).putExtra(EXTRA_ROUTE, routeId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun startInForeground(routeId: String, withLocation: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "창밖 안내", NotificationManager.IMPORTANCE_LOW))
        val open = openDrive(routeId)
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
        private const val CARD_CHANNEL = "point"
        private const val CARD_ID = 2
        /** 카드가 떠 있는 시간. 내비게이션의 회전 안내를 오래 가리지 않게 짧게 둔다 */
        private const val CARD_MS = 8_000L
        private const val ACTION_VISIT = "app.roadio.VISIT"
        private const val EXTRA_POINT = "point"
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

        /** 알림을 띄울 수 있는가. 못 띄우면 내비게이션 위 카드가 안 보이므로 주행 화면을 앞에 둬야 한다 */
        fun canShowCards(context: Context): Boolean =
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

        fun stop(context: Context) {
            context.stopService(Intent(context, GuideService::class.java))
        }
    }
}
