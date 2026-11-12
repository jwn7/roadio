package app.roadio.geo

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val R = 6_371_000.0

    /** 두 좌표 사이 직선 거리(m) */
    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }

    /** 1에서 2를 바라보는 방위각(0=북, 시계 방향, 도) */
    fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dL = Math.toRadians(lng2 - lng1)
        val y = sin(dL) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dL)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** 진행 방향(heading) 기준으로 대상이 오른쪽이면 true */
    fun isRight(heading: Double, bearingToTarget: Double): Boolean =
        ((bearingToTarget - heading + 360) % 360) < 180

    /** 한 점에서 방위각 방향으로 distM 만큼 이동한 좌표 */
    fun offset(lat: Double, lng: Double, bearingDeg: Double, distM: Double): Pair<Double, Double> {
        val d = distM / R
        val b = Math.toRadians(bearingDeg)
        val p1 = Math.toRadians(lat)
        val l1 = Math.toRadians(lng)
        val p2 = asin(sin(p1) * cos(d) + cos(p1) * sin(d) * cos(b))
        val l2 = l1 + atan2(sin(b) * sin(d) * cos(p1), cos(d) - sin(p1) * sin(p2))
        return Math.toDegrees(p2) to Math.toDegrees(l2)
    }
}
