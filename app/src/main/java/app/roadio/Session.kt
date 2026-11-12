package app.roadio

import android.content.Context
import app.roadio.data.Place
import app.roadio.navi.FakeNavi
import app.roadio.navi.NaviGateway
import app.roadio.navi.PleosNavi

/**
 * 두 화면(Activity)이 같이 쓰는 것들.
 * USE_FAKE_NAVI = true 로 두면 NaviHelper 없이도 화면 흐름을 볼 수 있다(일반 안드로이드 에뮬레이터 등).
 */
object Session {
    const val USE_FAKE_NAVI = false
    /** 실제 GPS 대신 가짜 주행으로 5→6→5 화면을 넘긴다(시연·개발용) */
    const val SIMULATE_DRIVE = true

    private var navi: NaviGateway? = null

    fun navi(context: Context, fallbackDestination: Place?): NaviGateway =
        navi ?: (if (USE_FAKE_NAVI) FakeNavi(fallbackDestination) else PleosNavi(context)).also { navi = it }

    fun release() {
        navi?.stop()
        navi = null
    }
}
