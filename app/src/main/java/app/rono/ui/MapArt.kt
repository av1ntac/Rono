package app.rono.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.rono.run.GeoPoint
import app.rono.ui.theme.Inter
import app.rono.ui.theme.RonoColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/*
 * The stylised maps from the mockups. The SVG geometry is drawn as-is with the same viewBox and
 * preserveAspectRatio (xMidYMid slice/meet), and the real GPS route is projected into the area
 * the mockup's route occupies.
 */

private class ViewBox(val x: Float, val y: Float, val w: Float, val h: Float)

private class SvgFit(val scale: Float, val tx: Float, val ty: Float) {
    fun map(p: Offset) = Offset(tx + p.x * scale, ty + p.y * scale)
}

private fun DrawScope.fit(vb: ViewBox, slice: Boolean): SvgFit {
    val sx = size.width / vb.w
    val sy = size.height / vb.h
    val s = if (slice) max(sx, sy) else min(sx, sy)
    return SvgFit(s, (size.width - vb.w * s) / 2 - vb.x * s, (size.height - vb.h * s) / 2 - vb.y * s)
}

private inline fun DrawScope.inSvg(f: SvgFit, crossinline block: DrawScope.() -> Unit) =
    withTransform({
        translate(f.tx, f.ty)
        scale(f.scale, f.scale, pivot = Offset.Zero)
    }) { block() }

private fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

private fun DrawScope.ringedDot(c: Offset, r: Float, fill: Color, ring: Float = 3f) {
    drawCircle(fill, r, c)
    drawCircle(RonoColors.Background, r, c, style = Stroke(ring))
}

private fun roadStroke(w: Float) = Stroke(w)

/** Fits GPS points into [target] (SVG units), keeping aspect ratio and a minimum extent. */
private class RouteProjection(points: List<GeoPoint>, private val target: Rect, minExtentM: Double = 250.0) {
    private val lat0 = points.map { it.lat }.average()
    private val kx = 111_320.0 * cos(Math.toRadians(lat0))
    private val ky = 110_540.0
    private val minX: Double
    private val minY: Double
    private val s: Double
    private val ox: Double
    private val oy: Double

    init {
        val xs = points.map { it.lon * kx }
        val ys = points.map { -it.lat * ky }
        val w = max(xs.max() - xs.min(), minExtentM)
        val h = max(ys.max() - ys.min(), minExtentM)
        s = min(target.width / w, target.height / h)
        minX = (xs.max() + xs.min()) / 2
        minY = (ys.max() + ys.min()) / 2
        ox = target.center.x.toDouble()
        oy = target.center.y.toDouble()
    }

    fun project(p: GeoPoint) = Offset(
        (ox + (p.lon * kx - minX) * s).toFloat(),
        (oy + (-p.lat * ky - minY) * s).toFloat(),
    )
}

private fun polyline(points: List<Offset>) = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
}

private fun DrawScope.drawRoute(paths: List<Path>, under: Float, over: Float, color: Color) {
    paths.forEach { drawPath(it, RonoColors.Background, style = Stroke(under, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    paths.forEach { drawPath(it, color, style = Stroke(over, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
}

// ---------------------------------------------------------------- Home (Main.dc.html)

private object HomeArt {
    val vb = ViewBox(40f, 90f, 380f, 470f)
    val park = svgPath("M 40 380 L 170 330 L 200 470 L 60 520 Z")
    val water = svgPath("M -10 120 C 80 150, 160 90, 250 130 S 380 200, 470 170")
    val minor = svgPath("M 0 330 L 460 300 M 0 460 L 460 440 M 60 0 L 90 600 M 240 0 L 255 600 M 370 0 L 400 600 M 0 560 L 460 540")
    val major = svgPath("M 0 250 L 460 210 M 130 0 L 190 600 M 300 0 L 330 600")
}

@Composable
fun HomeMap(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(RonoColors.MapBackground)
        inSvg(fit(HomeArt.vb, slice = true)) {
            drawPath(HomeArt.park, RonoColors.MapPark)
            drawPath(HomeArt.water, RonoColors.MapWater, style = roadStroke(28f))
            drawPath(HomeArt.minor, RonoColors.MapMinorRoad, style = roadStroke(3f))
            drawPath(HomeArt.major, RonoColors.MapMajorRoad, style = roadStroke(7f))
            val me = Offset(230f, 318f)
            drawCircle(RonoColors.Accent.copy(alpha = 0.14f), 30f, me)
            ringedDot(me, 9f, RonoColors.Accent)
        }
    }
}

// ---------------------------------------------------------------- Run / Paused

private object RunArt {
    val vb = ViewBox(40f, 130f, 380f, 400f)
    val park = svgPath("M 40 380 L 170 330 L 200 470 L 60 520 Z")
    val minor = svgPath("M -300 350 L 760 280 M -300 473 L 760 427 M 60 0 L 90 600 M 240 0 L 255 600 M 370 0 L 400 600 M -60 0 L -40 600 M 500 0 L 520 600")
    val major = svgPath("M -300 276 L 760 184 M 130 0 L 190 600 M 300 0 L 330 600")
    /** Bounding box of the mockup route; the live route is fitted into it. */
    val routeArea = Rect(95f, 150f, 360f, 500f)
}

@Composable
fun RouteMap(segments: List<List<GeoPoint>>, paused: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(RonoColors.MapBackground)
        inSvg(fit(RunArt.vb, slice = false)) {
            drawPath(RunArt.park, RonoColors.MapPark)
            drawPath(RunArt.minor, RonoColors.MapMinorRoad, style = roadStroke(3f))
            drawPath(RunArt.major, RonoColors.MapMajorRoad, style = roadStroke(7f))

            val all = segments.flatten()
            if (all.isEmpty()) return@inSvg
            val proj = RouteProjection(all, RunArt.routeArea)
            val paths = segments.filter { it.size > 1 }.map { seg -> polyline(seg.map(proj::project)) }
            drawRoute(paths, 10f, 5f, if (paused) RonoColors.Accent.copy(alpha = 0.55f) else RonoColors.Accent)

            ringedDot(proj.project(all.first()), 7f, RonoColors.Text)
            val here = proj.project(all.last())
            if (paused) {
                ringedDot(here, 9f, RonoColors.Slow)
            } else {
                drawCircle(RonoColors.Accent.copy(alpha = 0.2f), 16f, here)
                ringedDot(here, 7f, RonoColors.Accent)
            }
        }
    }
}

// ---------------------------------------------------------------- Summary

private object SummaryArt {
    val vb = ViewBox(20f, 240f, 380f, 420f)
    val park = svgPath("M 20 560 L 170 520 L 200 720 L 40 770 Z")
    val minor = svgPath("M -200 500 L 600 470 M -200 600 L 600 580 M -200 250 L 600 220 M 50 0 L 80 915 M 230 0 L 250 915 M 380 0 L 400 915 M -60 0 L -40 915 M 480 0 L 500 915")
    val major = svgPath("M -200 390 L 600 325 M 120 0 L 180 915 M 300 0 L 330 915")
    val routeArea = Rect(90f, 262f, 378f, 640f)
}

@Composable
fun SummaryMap(segments: List<List<GeoPoint>>, kmMarkers: List<GeoPoint>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        drawRect(RonoColors.MapBackground)
        val f = fit(SummaryArt.vb, slice = false)
        val all = segments.flatten()
        val proj = if (all.isEmpty()) null else RouteProjection(all, SummaryArt.routeArea)
        inSvg(f) {
            drawPath(SummaryArt.park, RonoColors.MapPark)
            drawPath(SummaryArt.minor, RonoColors.MapMinorRoad, style = roadStroke(3f))
            drawPath(SummaryArt.major, RonoColors.MapMajorRoad, style = roadStroke(8f))
            if (proj == null) return@inSvg
            val paths = segments.filter { it.size > 1 }.map { seg -> polyline(seg.map(proj::project)) }
            drawRoute(paths, 12f, 6f, RonoColors.Accent)
            kmMarkers.forEach { m ->
                val c = proj.project(m)
                drawCircle(RonoColors.Background, 11f, c)
                drawCircle(RonoColors.Accent, 11f, c, style = Stroke(2f))
            }
            ringedDot(proj.project(all.first()), 9f, RonoColors.Text)
            val end = proj.project(all.last())
            val sq = Rect(end.x - 9f, end.y - 9f, end.x + 9f, end.y + 9f)
            drawRoundRect(RonoColors.Accent, sq.topLeft, Size(18f, 18f), CornerRadius(4f))
            drawRoundRect(RonoColors.Background, sq.topLeft, Size(18f, 18f), CornerRadius(4f), style = Stroke(3f))
        }
        // Marker numbers: SVG text is 11 units, so scale it with the map.
        if (proj != null) {
            val style = TextStyle(
                fontFamily = Inter, fontWeight = FontWeight.Bold, color = RonoColors.Text,
                fontSize = (11f * f.scale / density / fontScale).sp,
            )
            kmMarkers.forEachIndexed { i, m ->
                val c = f.map(proj.project(m))
                val layout = measurer.measure("${i + 1}", style)
                drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
            }
        }
    }
}
