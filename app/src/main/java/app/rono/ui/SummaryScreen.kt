package app.rono.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rono.run.Fmt
import app.rono.run.RunSummary
import app.rono.ui.theme.RonoColors
import app.rono.ui.theme.RonoIcons
import app.rono.ui.theme.grotesk
import app.rono.ui.theme.inter
import kotlinx.coroutines.launch

/** design/update1/Summary.dc.html */
@Composable
fun SummaryScreen(run: RunSummary, backLabel: String, onBack: () -> Unit, onShare: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(RonoColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()),
    ) {
        // App bar: height 64, padding 8 8 0, gap 4
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RoundButton(RonoIcons.Back, backLabel, onBack, iconSize = 24.dp)
            Txt(
                Fmt.runTitle(run.startedAt),
                grotesk(22, FontWeight.SemiBold),
                Modifier.weight(1f).semantics { heading() },
                maxLines = 1,
            )
            RoundButton(RonoIcons.Share, "Share run", onShare)
        }
        Txt(Fmt.timeRange(run.startedAt, run.endedAt), inter(14, color = RonoColors.TextMuted), Modifier.padding(horizontal = 20.dp))

        // Map: margin 12 16 0, 260 tall, radius 28
        Box(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(RonoColors.MapBackground)
                .semantics { contentDescription = "Full route map from start to finish with kilometre markers" },
        ) {
            SummaryMap(run.segments, run.kmMarkers, Modifier.fillMaxSize())
            MapChip(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(10.dp, RonoColors.Text)
                        Txt("Start", inter(12, color = RonoColors.TextSecondary))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(10.dp, RonoColors.Accent, RoundedCornerShape(3.dp))
                        Txt("Finish", inter(12, color = RonoColors.TextSecondary))
                    }
                }
            }
        }

        // Headline stats: margin 12 16 0, 3 columns, gap 10
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Stat("Distance", withUnit(Fmt.km(run.distanceM), " km", 15, unitWeight = FontWeight.Medium), RonoColors.Text, Modifier.weight(1f))
            Stat("Time", withUnit(Fmt.duration(run.activeMs), "", 15), RonoColors.Text, Modifier.weight(1f))
            Stat("Avg pace", withUnit(Fmt.pace(run.avgPaceSecPerKm), " /km", 15, unitWeight = FontWeight.Medium), RonoColors.Accent, Modifier.weight(1f))
        }

        // Fastest km: margin 10 16 0, padding 14 18, radius 20
        run.fastestKm?.let { fastest ->
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    .fillMaxWidth()
                    .background(RonoColors.Surface, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Fastest km", inter(14, color = RonoColors.TextMuted))
                Txt(
                    withUnit(Fmt.pace(fastest.secPerKm), " /km · km ${fastest.km}", 14, unitWeight = FontWeight.Medium),
                    grotesk(22, FontWeight.SemiBold),
                )
            }
        }

        if (run.kmSplits.isNotEmpty() || run.splits100.isNotEmpty()) SplitsCard(run)

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun Stat(label: String, value: androidx.compose.ui.text.AnnotatedString, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Txt(label, inter(12, color = RonoColors.TextMuted))
        Txt(value, grotesk(34, FontWeight.SemiBold, color, lineHeight = 1.1f), maxLines = 1)
    }
}


private val SplitPages = listOf("Kilometre splits", "100 m splits")

/**
 * Kilometre splits and 100 m splits in one card: a segmented toggle on top, a HorizontalPager
 * (swipe to switch), and page dots below. The 100 m list scrolls inside the card.
 */
@Composable
private fun SplitsCard(run: RunSummary) {
    val pager = rememberPagerState { SplitPages.size }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    // Rows are 40dp with a 4dp gap. The mockup's 100 m page is 260dp; it also grows to the height
    // of a long kilometre list instead of leaving empty space beside it.
    val kmHeight = (run.kmSplits.size * 44 - 4).coerceAtLeast(0).dp
    val pageHeight = maxOf(kmHeight, 260.dp)

    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .background(RonoColors.Surface, RoundedCornerShape(24.dp))
            .padding(top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Toggle: margin 0 12, padding 4, gap 4, two 40dp pills
        Row(
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .background(RonoColors.Background, Pill)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SplitPages.forEachIndexed { i, label ->
                val on = page == i
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(Pill)
                        .background(if (on) RonoColors.Text else Color.Transparent)
                        .selectable(on, role = Role.Tab) { scope.launch { pager.animateScrollToPage(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Txt(label, inter(14, FontWeight.SemiBold, if (on) RonoColors.Background else RonoColors.TextSecondary), maxLines = 1)
                }
            }
        }

        HorizontalPager(
            pager,
            Modifier.fillMaxWidth().height(pageHeight),
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
        ) { p ->
            if (p == 0) KmSplitsPage(run) else HundredsPage(run)
        }

        // Page dots: 20×6 for the current page, 6×6 otherwise, gap 6; label 8dp further.
        Row(
            Modifier.fillMaxWidth().clearAndSetSemantics { },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SplitPages.indices.forEach { i ->
                val w by animateDpAsState(if (page == i) 20.dp else 6.dp, label = "dot")
                if (i > 0) Spacer(Modifier.width(6.dp))
                Box(Modifier.size(w, 6.dp).background(if (page == i) RonoColors.Accent else RonoColors.DotOff, Pill))
            }
            Spacer(Modifier.width(14.dp))
            Txt("Swipe to switch", inter(12, color = RonoColors.TextMuted))
        }
    }
}

/** Rows of 64dp label | 56dp pace | bar, 40dp tall, gap 12 between columns and 4 between rows. */
@Composable
private fun KmSplitsPage(run: RunSummary) {
    val splits = run.kmSplits
    if (splits.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Txt("No full kilometre yet", inter(14, color = RonoColors.TextMuted))
        }
        return
    }
    val avg = run.avgPaceSecPerKm ?: 0.0
    val slow = splits.maxOf { it.secPerKm } + 5
    val fast = splits.minOf { it.secPerKm }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        splits.forEach { k ->
            // Mockup: 40 + (335 − s) · 1.8 %, i.e. 40 % for slowest + 5 s up to 100 % for fastest.
            val pct = (40 + (slow - k.secPerKm) * (60 / (slow - fast))).coerceIn(0.0, 100.0)
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(k.label, inter(14, color = RonoColors.TextMuted), Modifier.width(64.dp), maxLines = 1)
                Spacer(Modifier.width(12.dp))
                Txt(Fmt.pace(k.secPerKm), grotesk(17, FontWeight.SemiBold), Modifier.width(56.dp), maxLines = 1)
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(RonoColors.Track, Pill),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((pct / 100).toFloat())
                            .fillMaxHeight()
                            .background(if (k.secPerKm <= avg) RonoColors.Accent else RonoColors.Slow, Pill),
                    )
                }
            }
        }
    }
}

/** DISTANCE | PACE header, then one 40dp row per 100 m with a 1dp top rule; scrolls vertically. */
@Composable
private fun HundredsPage(run: RunSummary) {
    val splits = run.splits100
    // Seconds per 100 m over the whole run; faster-or-equal splits are drawn in the accent colour.
    val avg = if (run.distanceM > 0) (run.activeMs / 1000.0) / (run.distanceM / 100.0) else 0.0
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        val head = inter(11, FontWeight.SemiBold, RonoColors.TextMuted, letterSpacingEm = 0.08f)
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Txt("DISTANCE", head)
            Txt("PACE", head)
        }
        if (splits.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Txt("No full 100 m yet", inter(14, color = RonoColors.TextMuted))
            }
            return@Column
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(splits) { i, x ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .drawBehind { drawRect(RonoColors.Track, size = Size(size.width, 1.dp.toPx())) }
                        .semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("${Fmt.hundredMark(i + 1)} km", inter(15, color = RonoColors.TextSecondary), maxLines = 1)
                    Txt(
                        "${Fmt.pace(x * 10)} /km",
                        grotesk(17, FontWeight.SemiBold, if (x <= avg) RonoColors.Accent else RonoColors.TextSecondary),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
