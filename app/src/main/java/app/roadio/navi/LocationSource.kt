package app.roadio.navi

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import app.roadio.data.PassPoint
import app.roadio.geo.Geo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

data class Fix(val lat: Double, val lng: Double)

object LocationSource {
    /** AAOS 표준 위치(Pleos "Standard First" 정책). 에뮬레이터 Extended controls > Location 으로 값을 넣을 수 있다. */
    @SuppressLint("MissingPermission")
    fun gps(context: Context): Flow<Fix> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val l = LocationListener { loc -> trySend(Fix(loc.latitude, loc.longitude)) }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, l)
        awaitClose { lm.removeUpdates(l) }
    }

    /** 시연·개발용: 각 지점을 서쪽에서 동쪽으로 지나가는 가짜 주행 */
    fun simulated(points: List<PassPoint>, stepMs: Long = 1500L): Flow<Fix> = flow {
        for (p in points) {
            for (d in listOf(-3000.0, -2400.0, -1500.0, -900.0, -400.0, 0.0, 300.0, 900.0)) {
                val (lat, lng) = Geo.offset(p.lat, p.lng, if (d < 0) 270.0 else 90.0, kotlin.math.abs(d))
                emit(Fix(lat, lng))
                delay(stepMs)
            }
        }
    }
}
