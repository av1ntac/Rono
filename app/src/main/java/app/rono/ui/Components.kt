package app.rono.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rono.ui.theme.RonoColors
import app.rono.ui.theme.RonoIcons
import app.rono.ui.theme.inter

/** Fully rounded (CSS `border-radius: 999px`). */
val Pill = RoundedCornerShape(999.dp)

@Composable
fun Txt(text: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Txt(AnnotatedString(text), style, modifier, maxLines)

@Composable
fun Txt(text: AnnotatedString, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    if (style.lineHeight.isEm) {
        // CSS `line-height` on a single line: the box is exactly lineHeight × font-size tall and
        // the glyphs' natural box is centred in it (negative half-leading included). Compose's own
        // lineHeight won't shrink the first/last line below the font's ascent/descent.
        val box = style.lineHeight.value * style.fontSize.value
        BasicText(
            text,
            modifier.layout { m, c ->
                val p = m.measure(c.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val h = box.sp.roundToPx()
                layout(p.width, h) { p.place(0, (h - p.height) / 2) }
            },
            style.copy(lineHeight = TextUnit.Unspecified),
            maxLines = maxLines,
        )
    } else {
        BasicText(text, modifier, style, maxLines = maxLines)
    }
}

@Composable
fun Icon(icon: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier) =
    Image(rememberVectorPainter(icon), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))

@Composable
fun Dot(size: Dp, color: Color, shape: androidx.compose.ui.graphics.Shape = CircleShape) =
    Box(Modifier.size(size).background(color, shape))

/** `5.24<span style="font-size: 22px; color: #9BA1A6"> km</span>` */
fun withUnit(
    value: String,
    unit: String,
    unitSize: Int,
    unitColor: Color = RonoColors.TextMuted,
    unitWeight: FontWeight? = null,
): AnnotatedString = buildAnnotatedString {
    append(value)
    withStyle(SpanStyle(fontSize = unitSize.sp, color = unitColor, fontWeight = unitWeight)) { append(unit) }
}

/** Round icon button, e.g. the 48dp settings / back / share buttons. */
@Composable
fun RoundButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    background: Color = Color.Transparent,
    tint: Color = RonoColors.Text,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(background)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, iconSize, tint) }
}

enum class Tab { Run, History, Profile }

/** Bottom navigation from Main.dc.html: 80dp, three equal columns, 64×32 selected pill. */
@Composable
fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(RonoColors.NavBar)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Tab.Run to RonoIcons.Runner, Tab.History to RonoIcons.History, Tab.Profile to RonoIcons.Profile)
                .forEach { (tab, icon) ->
                    val on = tab == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Tab) { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier
                                .size(64.dp, 32.dp)
                                .background(if (on) RonoColors.AccentSoftNav else Color.Transparent, Pill),
                            contentAlignment = Alignment.Center,
                        ) { Icon(icon, 22.dp, if (on) RonoColors.Accent else RonoColors.TextMuted) }
                        Txt(
                            tab.name,
                            inter(12, if (on) FontWeight.SemiBold else FontWeight.Medium, if (on) RonoColors.Text else RonoColors.TextMuted),
                        )
                    }
                }
        }
    }
}

/** Big circular primary button (START 136dp, Pause/Resume 104dp). */
@Composable
fun CircleAction(
    size: Dp,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

/** Small dark chip that sits on top of a map ("Route", "GPS ready · ±4 m"). */
@Composable
fun MapChip(modifier: Modifier = Modifier, h: Dp = 12.dp, v: Dp = 6.dp, content: @Composable () -> Unit) {
    Box(modifier.background(RonoColors.Background, Pill).padding(horizontal = h, vertical = v)) { content() }
}
