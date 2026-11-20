package app.rono.run

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import app.rono.R
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Draws notification text with the app's fonts. RemoteViews are inflated by the system, which
 * ignores app font resources, so each text slot is an ImageView showing an ALPHA_8 bitmap
 * (one byte per pixel keeps the notification parcel small), tinted via setColorFilter.
 */
internal class NotificationText(private val ctx: Context) {

    enum class Face(@FontRes val res: Int) {
        InterRegular(R.font.inter_regular),
        InterSemiBold(R.font.inter_semibold),
        InterBold(R.font.inter_bold),
        GroteskMedium(R.font.space_grotesk_medium),
        GroteskSemiBold(R.font.space_grotesk_semibold),
    }

    /** A CSS line box: bitmaps drawn in the same box share height and baseline. */
    class LineBox(val height: Int, val baseline: Float)

    private val metrics = ctx.resources.displayMetrics

    private fun typeface(face: Face): Typeface = cache.getOrPut(face) {
        ResourcesCompat.getFont(ctx, face.res) ?: Typeface.DEFAULT
    }

    private fun paint(face: Face, sizeSp: Float, letterSpacingEm: Float = 0f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typeface(face)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
        letterSpacing = letterSpacingEm
        fontFeatureSettings = "'tnum'"
    }

    /** Line box for [face] at [sizeSp]: natural height, or `line-height` × size when given. */
    fun box(face: Face, sizeSp: Float, lineHeight: Float? = null): LineBox {
        val p = paint(face, sizeSp)
        val fm = p.fontMetrics
        val natural = fm.descent - fm.ascent
        val h = if (lineHeight != null) (p.textSize * lineHeight).roundToInt() else ceil(natural).toInt()
        return LineBox(h, (h - natural) / 2f - fm.ascent)
    }

    fun bitmap(text: String, face: Face, sizeSp: Float, box: LineBox = box(face, sizeSp), letterSpacingEm: Float = 0f): Bitmap {
        val p = paint(face, sizeSp, letterSpacingEm)
        val pad = 1
        val w = (ceil(p.measureText(text)).toInt() + pad * 2).coerceAtLeast(1)
        return Bitmap.createBitmap(w, box.height.coerceAtLeast(1), Bitmap.Config.ALPHA_8).apply {
            density = metrics.densityDpi
            Canvas(this).drawText(text, pad.toFloat(), box.baseline, p)
        }
    }

    fun RemoteViews.text(
        @IdRes id: Int,
        text: String,
        face: Face,
        sizeSp: Float,
        color: Int,
        box: LineBox = box(face, sizeSp),
        letterSpacingEm: Float = 0f,
        description: String = text,
    ) {
        setImageViewBitmap(id, bitmap(text, face, sizeSp, box, letterSpacingEm))
        setInt(id, "setColorFilter", color)
        setContentDescription(id, description)
    }

    private companion object {
        val cache = HashMap<Face, Typeface>()
    }
}
