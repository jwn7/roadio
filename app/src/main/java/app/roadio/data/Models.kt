package app.roadio.data

enum class Side { L, R }

data class Place(val name: String, val lat: Double, val lng: Double, val region: String = "")

/** 창밖으로 보며 지나가는 지점. 내비 경유지와는 별개다(경유지는 최대 2개). */
data class PassPoint(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val side: Side,
    val noticeDistanceM: Int,
    val guideText: String,
    val audioFile: String?,
    /** true면 이 지점을 내비 경유지로 등록해 경로를 이 길로 끌어온다 */
    val steerWaypoint: Boolean,
    val visit: Place?,
)

data class ThemeRoute(
    val id: String,
    val name: String,
    val theme: String,
    val minutes: Int,
    val curvy: Boolean,
    val passPoints: List<PassPoint>,
)

data class RouteSet(
    val origin: Place,
    val destination: Place,
    val fastMinutes: Int,
    val routes: List<ThemeRoute>,
)

fun formatMinutes(min: Int): String =
    if (min < 60) "${min}분" else if (min % 60 == 0) "${min / 60}시간" else "${min / 60}시간 ${min % 60}분"
