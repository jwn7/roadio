package app.roadio.drive

import app.roadio.data.PassPoint
import app.roadio.data.Side
import app.roadio.geo.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveEngineTest {
    private fun pt(id: String, lat: Double, lng: Double) =
        PassPoint(id, id, lat, lng, Side.R, 600, "", null, false, null)

    private val a = pt("a", 38.10, 128.30)
    private val b = pt("b", 38.10, 128.40)

    @Test fun approaching_then_guiding_then_next() {
        val e = DriveEngine(listOf(a, b))
        val (farLat, farLng) = Geo.offset(a.lat, a.lng, 270.0, 3000.0)   // a의 서쪽 3km
        val s1 = e.update(farLat, farLng)
        assertTrue(s1 is DriveState.Approaching && s1.index == 0)

        val s2 = e.update(a.lat, a.lng)                                    // a 도착
        assertTrue(s2 is DriveState.Guiding && s2.index == 0)

        val (pastLat, pastLng) = Geo.offset(a.lat, a.lng, 90.0, 1500.0)   // a를 지나 동쪽 1.5km
        val s3 = e.update(pastLat, pastLng)
        assertTrue(s3 is DriveState.Approaching && s3.index == 1)
    }

    @Test fun graze_on_curvy_road_keeps_guiding() {
        // 굽은 길: 지점에 닿기 전에 반경(600m)을 스쳤다가(570m) 잠깐 벗어나도(650m) 같은 지점 안내를 이어 간다
        val e = DriveEngine(listOf(a, b))
        val (gLat, gLng) = Geo.offset(a.lat, a.lng, 270.0, 570.0)
        assertTrue(e.update(gLat, gLng).let { it is DriveState.Guiding && it.index == 0 })

        val (oLat, oLng) = Geo.offset(a.lat, a.lng, 270.0, 650.0)
        assertTrue(e.update(oLat, oLng).let { it is DriveState.Guiding && it.index == 0 })

        assertTrue(e.update(a.lat, a.lng).let { it is DriveState.Guiding && it.index == 0 })

        // 반경 + 200m(800m)를 넘어서야 다음 지점으로
        val (nLat, nLng) = Geo.offset(a.lat, a.lng, 90.0, 750.0)
        assertTrue(e.update(nLat, nLng).let { it is DriveState.Guiding && it.index == 0 })
        val (pLat, pLng) = Geo.offset(a.lat, a.lng, 90.0, 900.0)
        assertTrue(e.update(pLat, pLng).let { it is DriveState.Approaching && it.index == 1 })
    }

    @Test fun done_after_last_point() {
        val e = DriveEngine(listOf(a))
        e.update(a.lat, a.lng)
        val (lat, lng) = Geo.offset(a.lat, a.lng, 90.0, 2000.0)
        assertEquals(DriveState.Done(1), e.update(lat, lng))
    }

    @Test fun side_from_heading() {
        assertTrue(Geo.isRight(heading = 90.0, bearingToTarget = 180.0))   // 동쪽으로 가는 중, 남쪽 대상 = 오른쪽
        assertTrue(!Geo.isRight(heading = 90.0, bearingToTarget = 0.0))    // 북쪽 대상 = 왼쪽
    }
}
