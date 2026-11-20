package app.rono.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.rono.R

/** Colour tokens, taken verbatim from the mockups in design/. */
object RonoColors {
    val Accent = Color(0xFFC8F031)
    /** Accent + "29" alpha (Main.dc.html nav pill). */
    val AccentSoftNav = Color(0x29C8F031)
    /** Accent + "24" alpha (Run.dc.html status chip). */
    val AccentSoftChip = Color(0x24C8F031)
    val Slow = Color(0xFFFF9F43)

    val Background = Color(0xFF0E0F11)
    val Text = Color(0xFFF4F5F2)
    val TextSecondary = Color(0xFFC9CDD1)
    val TextMuted = Color(0xFF9BA1A6)

    val Surface = Color(0xFF17191C)
    val SurfaceRaised = Color(0xFF202327)
    val NavBar = Color(0xFF141619)
    val Track = Color(0xFF26292E)
    /** Inactive page dot (update1/Summary.dc.html). */
    val DotOff = Color(0xFF3A3F45)

    val MapBackground = Color(0xFF15171A)
    val MapPark = Color(0xFF18261C)
    val MapWater = Color(0xFF152634)
    val MapMinorRoad = Color(0xFF23272C)
    val MapMajorRoad = Color(0xFF2C3137)
}

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_light, FontWeight.Light),
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Text behaves like the CSS in the mockups: half-leading centred in the line box, no
 * extra font padding, and tabular figures (`font-variant-numeric: tabular-nums` on body).
 */
private val CssLike = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    fontFeatureSettings = "tnum",
    color = RonoColors.Text,
)

/** Space Grotesk, used for numbers and display headings. `lineHeight` is a CSS multiplier. */
fun grotesk(
    size: Int,
    weight: FontWeight,
    color: Color = RonoColors.Text,
    letterSpacingEm: Float = 0f,
    lineHeight: Float? = null,
): TextStyle = CssLike.copy(
    fontFamily = SpaceGrotesk,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = if (letterSpacingEm == 0f) TextUnit.Unspecified else letterSpacingEm.em,
    lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
)

/** Inter, used for all running text and labels. */
fun inter(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    color: Color = RonoColors.Text,
    letterSpacingEm: Float = 0f,
): TextStyle = CssLike.copy(
    fontFamily = Inter,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = if (letterSpacingEm == 0f) TextUnit.Unspecified else letterSpacingEm.em,
)
