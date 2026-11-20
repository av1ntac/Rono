package app.rono.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rono.ui.theme.RonoColors
import app.rono.ui.theme.RonoIcons
import app.rono.ui.theme.grotesk
import app.rono.ui.theme.inter

data class GpsChip(val label: String, val dot: Color)

/** design/Main.dc.html */
@Composable
fun HomeScreen(
    gps: GpsChip,
    lockStatsOn: Boolean,
    onStart: () -> Unit,
    onLockStatsClick: () -> Unit,
    onSettings: () -> Unit,
    onRecenter: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(RonoColors.Background)) {
        Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.statusBars)) {
            // Header: padding 28 20 16
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt("Rono", grotesk(20, FontWeight.Bold, RonoColors.Accent, letterSpacingEm = -0.02f))
                    Txt("Ready to run", grotesk(30, FontWeight.SemiBold, letterSpacingEm = -0.01f))
                }
                RoundButton(RonoIcons.Settings, "Settings", onSettings, background = RonoColors.Surface)
            }

            // Map card: margin 0 16, flex-grow, radius 28
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(RonoColors.MapBackground),
            ) {
                HomeMap(Modifier.fillMaxSize())
                MapChip(Modifier.align(Alignment.TopStart).padding(14.dp), h = 14.dp, v = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dot(8.dp, gps.dot)
                        Txt(gps.label, inter(13, FontWeight.Medium))
                    }
                }
                RoundButton(
                    RonoIcons.Recenter, "Recenter map", onRecenter,
                    Modifier.align(Alignment.BottomEnd).padding(14.dp),
                    background = RonoColors.Background,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            // START: padding 32 0 28, gap 16
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircleAction(136.dp, RonoColors.Accent, "Start run", onStart) {
                    Txt("START", grotesk(22, FontWeight.Bold, RonoColors.Background, letterSpacingEm = 0.08f))
                }
                Row(
                    Modifier.clickable(role = Role.Button, onClick = onLockStatsClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(RonoIcons.Lock, 16.dp, RonoColors.TextMuted)
                    Txt("Live stats on lock screen · ${if (lockStatsOn) "On" else "Off"}", inter(13, color = RonoColors.TextMuted))
                }
            }
        }
        BottomNav(Tab.Run, onTab)
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun HomePreview() = HomeScreen(GpsChip("GPS ready · ±4 m", RonoColors.Accent), true, {}, {}, {}, {}, {})
