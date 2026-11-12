package app.roadio.drive

import app.roadio.data.PassPoint
import app.roadio.geo.Geo

sealed interface DriveState {
    val index: Int
    /** 다음 지점으로 접근 중 */
    data class Approaching(override val index: Int, val point: PassPoint, val distanceM: Int) : DriveState
    /** 지금 창밖으로 보이는 구간 */
    data class Guiding(override val index: Int, val point: PassPoint) : DriveState
    /** 모든 지점을 지남 */
    data class Done(override val index: Int) : DriveState
}

/**
 * 위치가 들어올 때마다 "다음 지점 / 안내 중 / 끝"을 판정한다. 안드로이드 의존성 없음(단위 테스트 대상).
 *
 * 규칙
 * 1. 다음 지점까지 거리가 noticeDistanceM 이하가 되면 안내(Guiding)로 바뀐다.
 * 2. 안내 중에 거리가 다시 noticeDistanceM 보다 멀어지면 지나친 것으로 보고 다음 지점으로 넘어간다.
 */
class DriveEngine(private val points: List<PassPoint>) {
    private var index = 0
    private var guiding = false

    fun update(lat: Double, lng: Double): DriveState {
        while (index < points.size) {
            val p = points[index]
            val d = Geo.distanceM(lat, lng, p.lat, p.lng)
            if (d <= p.noticeDistanceM) {
                guiding = true
                return DriveState.Guiding(index, p)
            }
            if (guiding) {            // 안내하던 지점을 벗어남 → 다음 지점으로
                guiding = false
                index++
                continue
            }
            return DriveState.Approaching(index, p, d.toInt())
        }
        return DriveState.Done(index)
    }
}

fun formatDistance(m: Int): Pair<String, String> =
    if (m >= 1000) String.format("%.1f", m / 1000.0) to "km" else ((m / 50) * 50).toString() to "m"
