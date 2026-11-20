package app.rono.run

import android.location.Location
import android.os.SystemClock
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Debug helper: restores the run shown in the mockups (27:48, 5.24 km, the same route and
 * 100 m splits) and keeps it moving at 5:06 /km. Start it with
 * `adb shell am start -n app.rono/.MainActivity --ez demo true` on a debug build.
 */
internal object Demo {

    // Route from design/Run.dc.html, in SVG units.
    private const val ROUTE =
        "M 95 500 C 120 450, 110 400, 150 360 S 230 320, 240 260 S 260 170, 310 150 S 370 190, 345 250 S 300 330, 330 380"

    // 100 m splits from design/Summary.dc.html.
    private val SPLITS = doubleArrayOf(
        33.6, 33.2, 32.9, 33.4, 32.8, 33.1, 32.7, 33.0, 32.6, 32.7,
        32.9, 32.4, 32.8, 32.5, 32.6, 32.3, 32.7, 32.6, 32.5, 32.7,
        32.3, 32.0, 32.4, 31.9, 32.1, 31.8, 32.2, 31.8, 32.4, 31.9,
        32.1, 31.6, 31.8, 31.5, 31.9, 31.2, 31.4, 31.0, 31.6, 31.3,
        30.9, 31.2, 31.5, 30.8, 31.0, 30.4, 30.1, 29.6, 29.2, 28.9,
        29.4, 30.6,
    )

    private const val TOTAL_M = 5240.0
    private const val TOTAL_S = 27 * 60 + 48.0
    private const val SPEED = 1000.0 / 306.0 // 5:06 /km
    private const val LAT0 = 47.0
    private const val LON0 = 8.0
    private val KX = 111_320.0 * cos(Math.toRadians(LAT0))
    private const val KY = 110_540.0

    private var head = GeoPoint(LAT0, LON0)
    private var dirX = 0.0
    private var dirY = 0.0

    private fun toGeo(xm: Double, ym: Double) = GeoPoint(LAT0 - ym / KY, LON0 + xm / KX)

    fun seed() {
        val path = PathParser().parsePathString(ROUTE).toPath().asAndroidPath()
        val pm = android.graphics.PathMeasure(path, false)
        val metersPerUnit = TOTAL_M / pm.length
        val pos = FloatArray(2)
        fun at(m: Double): GeoPoint {
            pm.getPosTan((m / metersPerUnit).toFloat().coerceAtMost(pm.length), pos, null)
            return toGeo(pos[0] * metersPerUnit, pos[1] * metersPerUnit)
        }

        val route = (0..262).map { at(it * 20.0) }
        val markers = (1..5).map { at(it * 1000.0) }

        // Cumulative (time, distance) from the splits, scaled so the total is exactly 27:48.
        val raw = SPLITS.sum() + 0.4 * SPLITS.last()
        val k = TOTAL_S / raw
        val cumulative = ArrayList<Pair<Long, Double>>()
        cumulative.add(0L to 0.0)
        var t = 0.0
        SPLITS.forEachIndexed { i, s -> t += s * k; cumulative.add((t * 1000).toLong() to (i + 1) * 100.0) }
        cumulative.add((TOTAL_S * 1000).toLong() to TOTAL_M)

        // Keep running in the direction the route ends.
        pm.getPosTan(pm.length, pos, null)
        val tan = FloatArray(2)
        pm.getPosTan(pm.length, null, tan)
        val n = hypot(tan[0].toDouble(), tan[1].toDouble())
        dirX = tan[0] / n; dirY = tan[1] / n
        head = route.last()

        RunTracker.seedDemo(route, cumulative, markers)
    }

    /**
     * Debug helper: the seven runs from design/update1/History.dc.html. Each one follows the
     * mockup route with the mockup's 100 m split profile, scaled to its distance and time.
     */
    fun history(): List<RunSummary> = listOf(
        historyRun("2026-11-18 18:24", "18:52", 5240.0, 27 * 60 + 48),
        historyRun("2026-11-16 07:10", "07:38", 5020.0, 27 * 60 + 21),
        historyRun("2026-11-14 09:30", "10:28", 10120.0, 57 * 60 + 5),
        historyRun("2026-11-12 18:05", "18:27", 4100.0, 21 * 60 + 33),
        historyRun("2026-11-10 17:48", "18:23", 6350.0, 34 * 60 + 40),
        historyRun("2026-11-08 10:12", "10:57", 8000.0, 44 * 60 + 24),
        historyRun("2026-11-05 18:30", "18:57", 5000.0, 26 * 60 + 50),
    )

    private fun historyRun(start: String, endHm: String, meters: Double, activeS: Int): RunSummary {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val startedAt = fmt.parse(start)!!.time
        val endedAt = fmt.parse(start.take(11) + endHm)!!.time

        val path = PathParser().parsePathString(ROUTE).toPath().asAndroidPath()
        val pm = android.graphics.PathMeasure(path, false)
        val metersPerUnit = meters / pm.length
        val pos = FloatArray(2)
        fun at(m: Double): GeoPoint {
            pm.getPosTan((m / metersPerUnit).toFloat().coerceAtMost(pm.length), pos, null)
            return toGeo(pos[0] * metersPerUnit, pos[1] * metersPerUnit)
        }

        // Stretch the mockup's split profile over this run, then scale it to the run's time.
        val n = (meters / 100).toInt()
        val raw = List(n) { SPLITS[it * SPLITS.size / n] }
        val k = activeS / (raw.sum() + (meters / 100 - n) * raw.last())
        val splits = raw.map { it * k }

        val kms = splits.chunked(10).filter { it.size == 10 }
            .mapIndexed { i, c -> KmSplit("km ${i + 1}", c.sum(), i + 1, false) }.toMutableList()
        val partialM = meters - kms.size * 1000.0
        if (partialM >= 10.0) {
            val sec = activeS - kms.sumOf { it.secPerKm }
            kms.add(KmSplit("${Fmt.km(partialM)} km", sec / (partialM / 1000.0), kms.size + 1, true))
        }
        return RunSummary(
            id = startedAt,
            startedAt = startedAt,
            endedAt = endedAt,
            distanceM = meters,
            activeMs = activeS * 1000L,
            segments = listOf((0..200).map { at(it * meters / 200) }),
            splits100 = splits,
            kmSplits = kms,
            kmMarkers = (1..(meters / 1000).toInt()).map { at(it * 1000.0) },
        )
    }

    fun nextLocation(): Location {
        head = GeoPoint(head.lat - dirY * SPEED / KY, head.lon + dirX * SPEED / KX)
        return Location("demo").apply {
            latitude = head.lat
            longitude = head.lon
            accuracy = 4f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }
}
