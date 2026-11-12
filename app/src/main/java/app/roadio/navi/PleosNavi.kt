package app.roadio.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import app.roadio.data.Place

/**
 * NaviHelper 3.0.5 연동.
 *
 * SDK 호출은 전부 이 파일에 모은다. 시그니처는 NaviHelper 3.0.5 / navi-data 3.0.4 의
 * sources.jar 로 확인했다(2026-11-12):
 *   - 패키지: 상수는 navi.constants, 데이터는 navi.data, 리스너는 navi.helper.listener
 *   - NaviHelperEventListener 는 모든 메서드에 기본 구현이 있다. 필요한 것만 override
 *   - onWayPointArrived(WaypointArrivedInfo), onWayPointInfo(List<WaypointInfo>) — 둘 다 대문자 P.
 *     onWaypointChanged(WaypointChangedInfo) 만 소문자 p
 *   - DestinationInfo: name, address, latitude, longitude (non-null), poiId, poiSubId?
 *   - RequestWaypointInfo: latitude/longitude Double, poiName String?, poiId/address/poiSubId String (non-null)
 * 에뮬레이터에서 확인이 필요한 것:
 *   - poiId·poiSubId·address 를 빈 문자열로 넣어도 경유지가 등록되는지 (타입상으로는 통과)
 *   - 경유지를 그냥 지나칠 때 onWayPointArrived 가 오는지
 */
class PleosNavi(context: Context) : NaviGateway {
    private val tag = "PleosNavi"
    private val helper = NaviHelper(context.applicationContext)
    private var onDestination: ((Place?) -> Unit)? = null

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            Log.d(tag, "onDestinationInfo $destinationInfo")
            onDestination?.invoke(destinationInfo.toPlace())
            onDestination = null
        }

        override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) {
            // 확인할 것: 경유지를 "지나갈" 때 이 콜백이 오는지, 안내가 끊기는지
            Log.d(tag, "onWayPointArrived $waypointArrivedInfo")
        }

        // 아래는 경유지 동작 확인용 로그(docs/verify.md). `adb logcat -s PleosNavi` 로 본다.
        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) {
            Log.d(tag, "onWaypointChanged $waypointChangedInfo")
        }

        override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
            Log.d(tag, "onWayPointInfo $waypointInfoList")
        }

        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            Log.d(tag, "onRouteStarted $routeStartInfo")
        }

        override fun onRoutingCompleted() {
            Log.d(tag, "onRoutingCompleted")
        }

        override fun onReRouteEnded() {
            Log.d(tag, "onReRouteEnded")
        }

        override fun onRouteCancelled() {
            Log.d(tag, "onRouteCancelled")
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(tag, "onError $errorCode")
            onDestination?.invoke(null)
            onDestination = null
        }
    }

    override fun start(onReady: (Boolean) -> Unit) {
        helper.initialize(
            onConnected = {
                helper.addListener(listener)
                onReady(true)
            },
            onFailed = { t ->
                Log.e(tag, "initialize failed", t)
                onReady(false)
            },
        )
    }

    override fun stop() {
        helper.removeListener(listener)
        helper.release()
    }

    override fun requestDestination(onResult: (Place?) -> Unit) {
        onDestination = onResult
        helper.getDestinationInfo()
    }

    override fun addWaypoint(slot: Int, place: Place) {
        helper.addWaypoint(
            RequestWaypointInfo(
                latitude = place.lat,
                longitude = place.lng,
                waypointIndex = if (slot == 1) WaypointIndex.FIRST else WaypointIndex.SECOND,
                poiId = "",
                poiName = place.name,
                poiSubId = "",
                address = "",
            )
        )
    }

    override fun clearWaypoints() {
        helper.removeWaypoint(WaypointIndex.ALL)
    }

    // 필드 이름은 navi-data 3.0.4 소스로 확인: name, latitude, longitude 모두 non-null.
    private fun DestinationInfo.toPlace(): Place =
        Place(name = name, lat = latitude, lng = longitude)
}
