package app.roadio.navi

import app.roadio.data.Place

/**
 * 차량 내비게이션과의 접점. 앱의 나머지 코드는 이 인터페이스만 안다.
 * - PleosNavi : 실제 NaviHelper SDK
 * - FakeNavi  : SDK 없이 화면 흐름만 확인할 때
 */
interface NaviGateway {
    /** SDK 연결. 끝나면 onReady(성공 여부) */
    fun start(onReady: (Boolean) -> Unit)
    fun stop()
    /** 내비에 설정된 목적지. 없으면 null */
    fun requestDestination(onResult: (Place?) -> Unit)
    /** slot: 1 또는 2 (Pleos 내비 경유지는 최대 2개) */
    fun addWaypoint(slot: Int, place: Place)
    fun clearWaypoints()

    companion object {
        const val MAX_WAYPOINTS = 2
    }
}

class FakeNavi(private val destination: Place?) : NaviGateway {
    val waypoints = arrayOfNulls<Place>(NaviGateway.MAX_WAYPOINTS)
    override fun start(onReady: (Boolean) -> Unit) = onReady(true)
    override fun stop() {}
    override fun requestDestination(onResult: (Place?) -> Unit) = onResult(destination)
    override fun addWaypoint(slot: Int, place: Place) { waypoints[slot - 1] = place }
    override fun clearWaypoints() { waypoints.fill(null) }
}
