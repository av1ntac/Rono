package app.rono.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The inline SVG icons from the mockups, ported 1:1 (24×24 viewBox, currentColor). */
object RonoIcons {

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
        "M${x + rx},$y h${w - 2 * rx} a$rx,$rx 0 0,1 $rx,$rx v${h - 2 * rx} " +
            "a$rx,$rx 0 0,1 ${-rx},$rx h${-(w - 2 * rx)} a$rx,$rx 0 0,1 ${-rx},${-rx} " +
            "v${-(h - 2 * rx)} a$rx,$rx 0 0,1 $rx,${-rx} z"

    private fun stroked(name: String, vararg paths: String, width: Float = 2f) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(name: String, vararg paths: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.White)) }
        }.build()

    val Settings = stroked(
        "settings",
        circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
    )

    val Recenter = stroked("recenter", circle(12f, 12f, 7f), "M12 2v3M12 19v3M2 12h3M19 12h3")

    val Lock = stroked("lock", rect(5f, 11f, 14f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4")

    val Runner = stroked(
        "runner",
        circle(14f, 4f, 2f),
        "M7 21l3-6 3 2v5M10 15l1-5 4 3 3 1M11 10l-3 1-2 3",
    )

    val History = stroked("history", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5M12 7v5l3 2")

    val Profile = stroked("profile", circle(12f, 8f, 4f), "M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6")

    val Pin = stroked(
        "pin",
        "M12 21s-7-6.2-7-12a7 7 0 0 1 14 0c0 5.8-7 12-7 12z",
        circle(12f, 9f, 2.5f),
    )

    val Back = stroked("back", "M15 5l-7 7 7 7")

    val Chevron = stroked("chevron", "M9 5l7 7-7 7")

    val Share = stroked(
        "share",
        circle(18f, 5f, 3f),
        circle(6f, 12f, 3f),
        circle(18f, 19f, 3f),
        "M8.6 13.5l6.8 4M15.4 6.5l-6.8 4",
    )

    val Pause = filled("pause", rect(6f, 4.5f, 4f, 15f, 1.2f), rect(14f, 4.5f, 4f, 15f, 1.2f))

    val Play = filled("play", "M8 4.8v14.4a1 1 0 0 0 1.5.9l11.2-7.2a1 1 0 0 0 0-1.8L9.5 3.9A1 1 0 0 0 8 4.8z")

    val Stop = filled("stop", rect(5f, 5f, 14f, 14f, 2.5f))
}
