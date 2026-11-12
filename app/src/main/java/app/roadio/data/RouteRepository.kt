package app.roadio.data

import android.content.Context
import org.json.JSONObject

/**
 * 1차 MVP: 조망 판정 스크립트가 만든 JSON을 assets에서 읽는다.
 * 2차: 같은 형식을 서버 응답으로 받도록 이 클래스만 바꾼다.
 */
class RouteRepository(private val context: Context) {

    fun load(file: String = "routes_seoul_yangyang.json"): RouteSet {
        val set = parse(read(file))
        // 긴 설명은 경로 스크립트 산출물과 따로 둔다(narrations.json). 없으면 화면 문구를 그대로 읽는다.
        val narrations = runCatching { parseNarrations(read(NARRATIONS)) }.getOrDefault(emptyMap())
        if (narrations.isEmpty()) return set
        return set.copy(routes = set.routes.map { r ->
            r.copy(passPoints = r.passPoints.map { p -> narrations[p.id]?.let { p.copy(narration = it) } ?: p })
        })
    }

    private fun read(file: String) = context.assets.open(file).bufferedReader().use { it.readText() }

    companion object {
        const val NARRATIONS = "narrations.json"

        fun parse(text: String): RouteSet {
            val o = JSONObject(text)
            val routes = o.getJSONArray("routes")
            return RouteSet(
                origin = place(o.getJSONObject("origin")),
                destination = place(o.getJSONObject("destination")),
                fastMinutes = o.getInt("fastMinutes"),
                routes = (0 until routes.length()).map { route(routes.getJSONObject(it)) },
            )
        }

        /** { "narrations": { "지점 id": "긴 설명", … } } → id 별 설명. 빈 문자열은 뺀다 */
        fun parseNarrations(text: String): Map<String, String> {
            val o = JSONObject(text).optJSONObject("narrations") ?: return emptyMap()
            return o.keys().asSequence().map { it to o.optString(it).trim() }.filter { it.second.isNotEmpty() }.toMap()
        }

        private fun place(o: JSONObject) =
            Place(o.getString("name"), o.getDouble("lat"), o.getDouble("lng"), o.optString("region"))

        private fun route(o: JSONObject): ThemeRoute {
            val pts = o.getJSONArray("passPoints")
            return ThemeRoute(
                id = o.getString("id"),
                name = o.getString("name"),
                theme = o.getString("theme"),
                minutes = o.getInt("minutes"),
                curvy = o.optBoolean("curvy"),
                passPoints = (0 until pts.length()).map { point(pts.getJSONObject(it)) },
            )
        }

        private fun point(o: JSONObject) = PassPoint(
            id = o.getString("id"),
            name = o.getString("name"),
            lat = o.getDouble("lat"),
            lng = o.getDouble("lng"),
            side = if (o.getString("side") == "R") Side.R else Side.L,
            noticeDistanceM = o.optInt("noticeDistanceM", 600),
            guideText = descriptionOnly(o.optString("guideText"), o.getString("name")),
            audioFile = o.optString("audioFile").ifBlank { null },
            steerWaypoint = o.optBoolean("steerWaypoint"),
            visit = o.optJSONObject("visit")?.let { place(it) },
        )
    }
}
