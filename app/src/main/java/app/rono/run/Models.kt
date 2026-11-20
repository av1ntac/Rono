package app.rono.run

data class GeoPoint(val lat: Double, val lon: Double)

enum class RunPhase { Running, Paused }

/** Latest GPS fix, used for the "GPS ready · ±4 m" / "GPS strong" labels. */
data class GpsFix(val point: GeoPoint, val accuracyM: Float, val atElapsedMs: Long)

/** Snapshot of the run in progress, published about once a second. */
data class LiveRun(
    val phase: RunPhase,
    val startedAtWall: Long,
    /** Moving time; pauses are excluded. */
    val activeMs: Long,
    /** How long the current pause has lasted (0 while running). */
    val pausedForMs: Long,
    val distanceM: Double,
    /** Seconds per km over the last ~20 s, or null when not moving. */
    val currentPaceSecPerKm: Double?,
    /** Route, one list per running segment (a pause starts a new segment). */
    val segments: List<List<GeoPoint>>,
) {
    val avgPaceSecPerKm: Double?
        get() = if (distanceM < 10.0) null else (activeMs / 1000.0) / (distanceM / 1000.0)

    /** 0..1 progress through the current kilometre. */
    val kmProgress: Float get() = ((distanceM % 1000.0) / 1000.0).toFloat()
    val nextKm: Int get() = (distanceM / 1000.0).toInt() + 1
    val metersToNextKm: Int get() = (nextKm * 1000 - distanceM).toInt().coerceAtLeast(0)
}

data class KmSplit(val label: String, val secPerKm: Double, val km: Int, val partial: Boolean)

/** A finished run, as stored on disk and shown on the summary screen. */
data class RunSummary(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val distanceM: Double,
    val activeMs: Long,
    val segments: List<List<GeoPoint>>,
    /** Seconds taken for each complete 100 m. */
    val splits100: List<Double>,
    val kmSplits: List<KmSplit>,
    /** Where each full kilometre was reached. */
    val kmMarkers: List<GeoPoint>,
) {
    val avgPaceSecPerKm: Double?
        get() = if (distanceM < 10.0) null else (activeMs / 1000.0) / (distanceM / 1000.0)

    val fastestKm: KmSplit? get() = kmSplits.filter { !it.partial }.minByOrNull { it.secPerKm }
}
