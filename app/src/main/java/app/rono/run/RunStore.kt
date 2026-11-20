package app.rono.run

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Finished runs, newest first, kept in a JSON file in app storage. */
object RunStore {

    private val _runs = MutableStateFlow<List<RunSummary>>(emptyList())
    val runs: StateFlow<List<RunSummary>> = _runs
    private var loaded = false

    private fun file(ctx: Context) = File(ctx.filesDir, "runs.json")

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val f = file(ctx)
        if (!f.exists()) return
        _runs.value = runCatching {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /** Debug-only demo runs: shown in History but never written to disk. */
    private val demoIds = HashSet<Long>()

    internal fun seedDemo(demo: List<RunSummary>) {
        demoIds += demo.map { it.id }
        _runs.value = (demo + _runs.value.filter { it.id !in demoIds }).sortedByDescending { it.startedAt }
    }

    fun add(ctx: Context, run: RunSummary) {
        _runs.value = listOf(run) + _runs.value.filter { it.id != run.id }
        val arr = JSONArray().apply { _runs.value.filter { it.id !in demoIds }.forEach { put(toJson(it)) } }
        val f = file(ctx)
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }

    fun get(id: Long): RunSummary? = _runs.value.firstOrNull { it.id == id }

    private fun points(list: List<GeoPoint>) = JSONArray().apply {
        list.forEach { put(it.lat); put(it.lon) }
    }

    private fun points(arr: JSONArray) = (0 until arr.length() / 2).map {
        GeoPoint(arr.getDouble(it * 2), arr.getDouble(it * 2 + 1))
    }

    private fun toJson(r: RunSummary) = JSONObject().apply {
        put("id", r.id)
        put("startedAt", r.startedAt)
        put("endedAt", r.endedAt)
        put("distanceM", r.distanceM)
        put("activeMs", r.activeMs)
        put("segments", JSONArray().apply { r.segments.forEach { put(points(it)) } })
        put("splits100", JSONArray().apply { r.splits100.forEach { put(it) } })
        put("kmSplits", JSONArray().apply {
            r.kmSplits.forEach {
                put(JSONObject().put("label", it.label).put("sec", it.secPerKm).put("km", it.km).put("partial", it.partial))
            }
        })
        put("kmMarkers", points(r.kmMarkers))
    }

    private fun fromJson(o: JSONObject): RunSummary {
        val segs = o.getJSONArray("segments")
        val splits = o.getJSONArray("splits100")
        val kms = o.getJSONArray("kmSplits")
        return RunSummary(
            id = o.getLong("id"),
            startedAt = o.getLong("startedAt"),
            endedAt = o.getLong("endedAt"),
            distanceM = o.getDouble("distanceM"),
            activeMs = o.getLong("activeMs"),
            segments = (0 until segs.length()).map { points(segs.getJSONArray(it)) },
            splits100 = (0 until splits.length()).map { splits.getDouble(it) },
            kmSplits = (0 until kms.length()).map {
                val k = kms.getJSONObject(it)
                KmSplit(k.getString("label"), k.getDouble("sec"), k.getInt("km"), k.getBoolean("partial"))
            },
            kmMarkers = points(o.getJSONArray("kmMarkers")),
        )
    }
}
