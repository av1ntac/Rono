package app.rono.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
import java.util.Calendar

/** design/update1/History.dc.html. [runs] are newest first, as kept by RunStore. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    runs: List<RunSummary>,
    listState: LazyListState,
    onOpen: (RunSummary) -> Unit,
    onTab: (Tab) -> Unit,
) {
    val months = remember(runs) { runs.groupBy { monthKey(it.startedAt) }.values.toList() }
    Column(Modifier.fillMaxSize().background(RonoColors.Background)) {
        Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.statusBars)) {
            // Header: padding 28 20 8, gap 2
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Txt("Rono", grotesk(20, FontWeight.Bold, RonoColors.Accent, letterSpacingEm = -0.02f))
                Txt("History", grotesk(30, FontWeight.SemiBold, letterSpacingEm = -0.01f), Modifier.semantics { heading() })
            }

            if (runs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Txt("No runs yet", inter(15, color = RonoColors.TextMuted))
                }
            } else {
                // The mockup shows one month: its heading stays put while the cards scroll.
                // With several months each heading sticks until the next one pushes it out.
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    months.forEach { month ->
                        stickyHeader(key = "m${monthKey(month.first().startedAt)}") { MonthHeading(month) }
                        items(month, key = { it.id }) { run ->
                            RunCard(run, { onOpen(run) }, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                        }
                    }
                }
            }
        }
        BottomNav(Tab.History, onTab)
    }
}

private fun monthKey(at: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = at }
    return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH)
}

/** "NOVEMBER 2026 … 7 runs · 43.8 km": padding 12 20 10, baseline-aligned. */
@Composable
private fun MonthHeading(runs: List<RunSummary>) {
    val count = if (runs.size == 1) "1 run" else "${runs.size} runs"
    Row(
        Modifier
            .fillMaxWidth()
            .background(RonoColors.Background)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Txt(
            Fmt.month(runs.first().startedAt).uppercase(),
            inter(13, FontWeight.SemiBold, RonoColors.TextMuted, letterSpacingEm = 0.1f),
            Modifier.alignByBaseline().semantics { heading() },
        )
        Txt("$count · ${Fmt.km1(runs.sumOf { it.distanceM })} km", inter(13, color = RonoColors.TextMuted), Modifier.alignByBaseline())
    }
}

/** One run: padding 14 12 14 16, radius 20, chevron on the right. */
@Composable
private fun RunCard(run: RunSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val value = grotesk(20, FontWeight.SemiBold)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RonoColors.Surface)
            .clickable(onClickLabel = "Open run summary", role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(Fmt.runTitle(run.startedAt), inter(15, FontWeight.SemiBold), Modifier.weight(1f).alignByBaseline(), maxLines = 1)
                Txt(Fmt.timeRange(run.startedAt, run.endedAt), inter(13, color = RonoColors.TextMuted), Modifier.alignByBaseline(), maxLines = 1)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(withUnit(Fmt.km(run.distanceM), " km", 13, unitWeight = FontWeight.Medium), value, Modifier.weight(1f), maxLines = 1)
                Txt(Fmt.duration(run.activeMs), value, Modifier.weight(1f), maxLines = 1)
                Txt(withUnit(Fmt.pace(run.avgPaceSecPerKm), " /km", 13, unitWeight = FontWeight.Medium), value, Modifier.weight(1f), maxLines = 1)
            }
        }
        Icon(RonoIcons.Chevron, 20.dp, RonoColors.TextMuted)
    }
}
