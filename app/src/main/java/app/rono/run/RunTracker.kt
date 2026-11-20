package app.rono.run

import android.location.Location
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

/**
 * Single source of truth for the run in progress. Fed by [RunService] (GPS + 1 s ticker),
 * observed by the UI and the live notification. All calls happen on the main thread.
 */
object RunTracker {

    private val _live = MutableStateFlow<LiveRun?>(null)
    val live: StateFlow<LiveRun?> = _live

    private val _gps = MutableStateFlow<GpsFix?>(null)
    val gps: StateFlow<GpsFix?> = _gps

    private class Sample(val ms: Long, val d: Double)

    private var active = false
    private var phase = RunPhase.Running
    private var startedAtWall = 0L
    private var accumulatedMs = 0L
    private var segmentStart = 0L
    private var pausedAt = 0L
    private var distance = 0.0
    private val samples = ArrayList<Sample>()
    private val segments = ArrayList<ArrayList<GeoPoint>>()
    private val kmMarkers = ArrayList<GeoPoint>()
    private var lastAccepted: Location? = null

    private fun now() = SystemClock.elapsedRealtime()

    val isActive: Boolean get() = active

    private fun activeMs(): Long = accumulatedMs + if (phase == RunPhase.Running) now() - segmentStart else 0

    private fun reset() {
        accumulatedMs = 0; distance = 0.0; lastAccepted = null
        samples.clear(); segments.clear(); kmMarkers.clear()
    }

    fun start() {
        if (active) return
        reset()
        active = true
        phase = RunPhase.Running
        startedAtWall = System.currentTimeMillis()
        segmentStart = now()
        segments.add(ArrayList())
        samples.add(Sample(0, 0.0))
        // Seed the route with the fix we already have, so the map has a start point at once.
        _gps.value?.takeIf { now() - it.atElapsedMs < 10_000 }?.let { segments.last().add(it.point) }
        publish()
    }

    fun pause() {
        if (!active || phase != RunPhase.Running) return
        accumulatedMs += now() - segmentStart
        pausedAt = now()
        phase = RunPhase.Paused
        lastAccepted = null // the gap while paused never counts as distance
        publish()
    }

    fun resume() {
        if (!active || phase != RunPhase.Paused) return
        segmentStart = now()
        phase = RunPhase.Running
        segments.add(ArrayList())
        publish()
    }

    fun tick() {
        if (active) publish()
    }

    fun onLocation(loc: Location) {
        val point = GeoPoint(loc.latitude, loc.longitude)
        _gps.value = GpsFix(point, if (loc.hasAccuracy()) loc.accuracy else 50f, now())
        if (!active || phase != RunPhase.Running) return
        if (loc.hasAccuracy() && loc.accuracy > 30f) return

        val prev = lastAccepted
        if (prev == null) {
            lastAccepted = loc
            segments.last().add(point)
            publish()
            return
        }
        val dt = (loc.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1e9
        if (dt <= 0) return
        val d = prev.distanceTo(loc).toDouble()
        if (d / dt > 12.0) return // faster than a sprinter: a GPS jump
        // Ignore jitter while standing still.
        if (d < max(3.0, (if (loc.hasAccuracy()) loc.accuracy else 10f) * 0.5)) return

        val before = distance
        distance += d
        lastAccepted = loc
        segments.last().add(point)
        samples.add(Sample(activeMs(), distance))
        var km = (before / 1000).toInt() + 1
        while (distance >= km * 1000.0) {
            kmMarkers.add(point); km++
        }
        publish()
    }

    /** Ends the run and returns its summary, or null when no run was active. */
    fun finish(): RunSummary? {
        if (!active) return null
        if (phase == RunPhase.Running) accumulatedMs += now() - segmentStart
        val total = accumulatedMs
        val summary = RunSummary(
            id = startedAtWall,
            startedAt = startedAtWall,
            endedAt = System.currentTimeMillis(),
            distanceM = distance,
            activeMs = total,
            segments = segments.filter { it.isNotEmpty() }.map { it.toList() },
            splits100 = splitTimes(100.0).toDeltas(),
            kmSplits = kmSplits(total),
            kmMarkers = kmMarkers.toList(),
        )
        active = false
        reset()
        _live.value = null
        return summary
    }

    private fun publish() {
        val a = activeMs()
        _live.value = LiveRun(
            phase = phase,
            startedAtWall = startedAtWall,
            activeMs = a,
            pausedForMs = if (phase == RunPhase.Paused) now() - pausedAt else 0,
            distanceM = distance,
            currentPaceSecPerKm = currentPace(a),
            segments = segments.map { it.toList() },
        )
    }

    /** Pace over the last ~20 s of moving time; null when not moving. */
    private fun currentPace(activeNow: Long): Double? {
        if (phase != RunPhase.Running || samples.size < 2) return null
        val last = samples.last()
        if (activeNow - last.ms > 10_000) return null
        val cutoff = activeNow - 20_000
        var i = samples.size - 1
        while (i > 0 && samples[i].ms > cutoff) i--
        val ref = samples[i]
        val dd = last.d - ref.d
        if (dd < 15) return null
        return ((activeNow - ref.ms) / 1000.0) / (dd / 1000.0)
    }

    /** Moving time (ms) at which each multiple of [step] metres was reached, interpolated. */
    private fun splitTimes(step: Double): List<Double> {
        val out = ArrayList<Double>()
        var b = step
        for (j in 1 until samples.size) {
            val a = samples[j - 1]
            val c = samples[j]
            while (b <= c.d && c.d > a.d) {
                val f = (b - a.d) / (c.d - a.d)
                out.add(a.ms + f * (c.ms - a.ms))
                b += step
            }
        }
        return out
    }

    private fun List<Double>.toDeltas(): List<Double> {
        var prev = 0.0
        return map { t -> ((t - prev) / 1000.0).also { prev = t } }
    }

    private fun kmSplits(totalMs: Long): List<KmSplit> {
        val times = splitTimes(1000.0)
        val out = times.toDeltas().mapIndexed { i, s -> KmSplit("km ${i + 1}", s, i + 1, false) }.toMutableList()
        val partialM = distance - times.size * 1000.0
        if (partialM >= 10.0) {
            val sec = (totalMs - (times.lastOrNull() ?: 0.0)) / 1000.0
            out.add(KmSplit("${Fmt.km(partialM)} km", sec / (partialM / 1000.0), times.size + 1, true))
        }
        return out
    }

    /** Debug-only: restores a run in progress from canned data (see [Demo]). */
    internal fun seedDemo(route: List<GeoPoint>, cumulative: List<Pair<Long, Double>>, markers: List<GeoPoint>) {
        reset()
        active = true
        phase = RunPhase.Running
        accumulatedMs = cumulative.last().first
        startedAtWall = System.currentTimeMillis() - accumulatedMs
        segmentStart = now()
        distance = cumulative.last().second
        cumulative.forEach { samples.add(Sample(it.first, it.second)) }
        segments.add(ArrayList(route))
        kmMarkers.addAll(markers)
        lastAccepted = Location("demo").apply {
            latitude = route.last().lat; longitude = route.last().lon
            accuracy = 4f; elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        publish()
    }
}
