package app.rono.run

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

object Fmt {
    const val NO_PACE = "–:––"

    /** 27:48, or 1:02:03 past an hour. */
    fun duration(ms: Long): String {
        val t = ms / 1000
        val h = t / 3600
        val m = (t % 3600) / 60
        val s = t % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    /** 5:06 (min:sec per km), or –:–– when there is no meaningful pace. */
    fun pace(secPerKm: Double?): String {
        if (secPerKm == null || !secPerKm.isFinite() || secPerKm <= 0 || secPerKm >= 3600) return NO_PACE
        val t = secPerKm.roundToInt()
        return String.format(Locale.US, "%d:%02d", t / 60, t % 60)
    }

    /** 5.24 — truncated rather than rounded, so a km is only shown once it is done. */
    fun km(meters: Double): String =
        String.format(Locale.getDefault(), "%.2f", floor(meters / 10.0) / 100.0)

    /** 0:31.8 */
    fun split(sec: Double): String {
        val m = floor(sec / 60).toInt()
        return String.format(Locale.US, "%d:%04.1f", m, sec - m * 60)
    }

    /** 18.11.2026 (Wednesday). English weekday, like the rest of the UI. */
    fun runTitle(startedAt: Long): String = SimpleDateFormat("dd.MM.yyyy (EEEE)", Locale.ENGLISH).format(startedAt)

    /** 18:24 – 18:52 */
    fun timeRange(startedAt: Long, endedAt: Long): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "${time.format(startedAt)} – ${time.format(endedAt)}"
    }

    /** November 2026 (the History screen upper-cases it). */
    fun month(at: Long): String = SimpleDateFormat("LLLL yyyy", Locale.ENGLISH).format(at)

    /** 43.8: one decimal, truncated like [km]. */
    fun km1(meters: Double): String =
        String.format(Locale.getDefault(), "%.1f", floor(meters / 100.0) / 10.0)

    /** 0.1, 0.2, … : the distance at the end of the n-th 100 m split (1-based). */
    fun hundredMark(n: Int): String = String.format(Locale.getDefault(), "%.1f", n / 10.0)
}
