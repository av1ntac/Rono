package app.rono.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rono.run.Fmt
import app.rono.run.LiveRun
import app.rono.run.RunPhase
import app.rono.ui.theme.RonoColors
import app.rono.ui.theme.RonoIcons
import app.rono.ui.theme.grotesk
import app.rono.ui.theme.inter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** design/Run.dc.html (running) and design/Paused.dc.html (paused). */
@Composable
fun RunScreen(
    run: LiveRun,
    gpsLabel: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    val paused = run.phase == RunPhase.Paused
    Column(
        Modifier
            .fillMaxSize()
            .background(RonoColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(Modifier.weight(1f)) {
            // Status row: padding 24 20 0
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(run)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(RonoIcons.Pin, 16.dp, RonoColors.TextMuted)
                    Txt(gpsLabel, inter(13, color = RonoColors.TextMuted))
                }
            }

            // Time + distance: padding 40 20 0, gap 2
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Txt(
                    if (paused) "TIME · STOPPED" else "TIME",
                    inter(12, FontWeight.SemiBold, if (paused) RonoColors.Slow else RonoColors.TextMuted, letterSpacingEm = 0.12f),
                )
                Txt(
                    Fmt.duration(run.activeMs),
                    grotesk(100, FontWeight.SemiBold, if (paused) RonoColors.TextSecondary else RonoColors.Text, -0.03f, lineHeight = 1f),
                    maxLines = 1,
                )
                Txt(
                    withUnit(Fmt.km(run.distanceM), " km", 22),
                    grotesk(52, FontWeight.Medium, if (paused) RonoColors.TextSecondary else RonoColors.Text, lineHeight = 1f),
                    Modifier.padding(top = 10.dp),
                )
            }

            // Pace cards: margin 24 16 0, gap 10
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val current = run.currentPaceSecPerKm
                val avg = run.avgPaceSecPerKm
                if (paused || current == null) {
                    PaceCard(
                        "Current pace",
                        withUnit(Fmt.NO_PACE, " /km", 15, unitWeight = FontWeight.Medium),
                        RonoColors.TextMuted,
                        "Not moving",
                        Modifier.weight(1f),
                    )
                } else {
                    PaceCard(
                        "Current pace",
                        withUnit(Fmt.pace(current), " /km", 15, unitWeight = FontWeight.Medium),
                        RonoColors.Accent,
                        paceDelta(current, avg),
                        Modifier.weight(1f),
                    )
                }
                PaceCard(
                    "Avg pace",
                    withUnit(Fmt.pace(avg), " /km", 15, unitWeight = FontWeight.Medium),
                    RonoColors.Text,
                    if (paused) "Pauses not counted" else "Whole run so far",
                    Modifier.weight(1f),
                )
            }

            // Route map: margin 10 16 0, 290 tall, radius 22. Shrinks on short screens.
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    .heightIn(max = 290.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(RonoColors.MapBackground),
            ) {
                RouteMap(run.segments, paused, Modifier.fillMaxSize())
                MapChip(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Txt(if (paused) "Paused here" else "Route", inter(12, FontWeight.Medium))
                }
            }
        }

        // Controls: padding 0 28 34
        if (paused) {
            Row(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleAction(104.dp, RonoColors.Accent, "Resume run", onResume) {
                        Icon(RonoIcons.Play, 40.dp, RonoColors.Background)
                    }
                    Txt("Resume", inter(12, color = RonoColors.TextMuted))
                }
                Column(
                    Modifier.padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HoldToFinish(onFinish)
                    Txt("Hold to finish", inter(12, color = RonoColors.TextMuted))
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircleAction(104.dp, RonoColors.Accent, "Pause run", onPause) {
                    Icon(RonoIcons.Pause, 40.dp, RonoColors.Background)
                }
                Txt("Pause", inter(12, color = RonoColors.TextMuted))
            }
        }
    }
}

@Composable
private fun StatusChip(run: LiveRun) {
    if (run.phase == RunPhase.Paused) {
        Row(
            Modifier.height(36.dp).background(RonoColors.Slow, Pill).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(RonoIcons.Pause, 14.dp, RonoColors.Background)
            Txt("Paused · ${Fmt.duration(run.pausedForMs)}", inter(14, FontWeight.Bold, RonoColors.Background))
        }
    } else {
        Row(
            Modifier.height(36.dp).background(RonoColors.AccentSoftChip, Pill).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Dot(8.dp, RonoColors.Accent)
            Txt("Running", inter(14, FontWeight.SemiBold, RonoColors.Accent))
        }
    }
}

/** Card: padding 14 16, radius 22, #17191C, gap 4. */
@Composable
private fun PaceCard(label: String, value: AnnotatedString, valueColor: androidx.compose.ui.graphics.Color, caption: String, modifier: Modifier) {
    Column(
        modifier
            .background(RonoColors.Surface, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Txt(label, inter(13, color = RonoColors.TextMuted))
        Txt(value, grotesk(34, FontWeight.SemiBold, valueColor, lineHeight = 1.1f), maxLines = 1)
        Txt(caption, inter(12, color = RonoColors.TextSecondary), maxLines = 1)
    }
}

private fun paceDelta(current: Double, avg: Double?): String {
    if (avg == null) return "Whole run so far"
    val d = (avg - current).roundToInt()
    return when {
        d == 0 -> "Same as avg"
        d > 0 -> "▲ $d s faster than avg"
        else -> "▼ ${abs(d)} s slower than avg"
    }
}

/** 64dp stop button that finishes the run after a 1 s press; a ring shows the hold progress. */
@Composable
private fun HoldToFinish(onFinish: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(64.dp)
            .semantics {
                contentDescription = "Finish run (press and hold)"
                role = Role.Button
                onClick("Finish run") { onFinish(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val hold = scope.launch {
                        progress.animateTo(1f, tween(1000, easing = LinearEasing))
                        onFinish()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        hold.cancel()
                        scope.launch { progress.animateTo(0f, tween(150)) }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(RonoColors.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) { Icon(RonoIcons.Stop, 22.dp, RonoColors.Text) }
        if (progress.value > 0f) {
            Canvas(Modifier.size(64.dp)) {
                val w = 3.dp.toPx()
                drawArc(
                    RonoColors.Accent, -90f, 360f * progress.value, false,
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    style = Stroke(w, cap = StrokeCap.Round),
                )
            }
        }
    }
}
