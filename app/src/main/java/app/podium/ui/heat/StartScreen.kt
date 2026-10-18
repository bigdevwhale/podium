package app.podium.ui.heat

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.data.GhostMode
import app.podium.data.Heats
import app.podium.data.Sport
import app.podium.domain.Relays
import app.podium.domain.Results
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.PictoTile
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The start line: who runs in the next lane, an entry for the time if you want one, then the gun. */
@Composable
fun StartScreen(eventId: Long, relayId: Long, arena: Arena, go: Go) {
    val c = ctx()
    val relay = arena.relay(relayId)
    val legs = relay?.legIds?.mapNotNull { arena.event(it) }.orEmpty()
    val e = if (relay != null) legs.firstOrNull() else arena.event(eventId)
    if (e == null) {
        LaunchedEffect(Unit) { go.back() }
        return
    }
    val runs = arena.runsOf(e.id)
    val ghosts = GhostMode.entries.associateWith { Results.ghost(e, runs, it, arena.now) }
    var mode by rememberSaveable { mutableStateOf(arena.settings.ghost.takeIf { ghosts[it] != null } ?: GhostMode.entries.firstOrNull { ghosts[it] != null } ?: GhostMode.RECORD) }
    val standard = Results.standard(e, runs)
    var guessing by rememberSaveable { mutableStateOf(false) }
    var guess by rememberSaveable { mutableLongStateOf(((standard ?: ghosts[mode]?.total ?: 600_000L) / 10_000) * 10_000) }
    var countdown by remember { mutableIntStateOf(-1) }
    val haptic = LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.start_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (relay != null) {
                Text(stringResource(R.string.relay_eyebrow).uppercase(), style = UType.Eyebrow)
                Text(relay.name, style = UType.Display)
                Spacer(Modifier.height(12.dp))
                legs.forEachIndexed { i, l ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", style = UType.Time.copy(color = U.c.faint), modifier = Modifier.width(22.dp))
                        PictoTile(l.picto, l.ringOf.color(), size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(l.name, style = UType.Strong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Results.record(l, arena.runsOf(l.id))?.let { Text(T.precise(it.duration), style = UType.Time.copy(color = U.c.gold)) }
                    }
                }
                Relays.best(relay, arena.runs)?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.relay_best, T.precise(it.total)), style = UType.Sub)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PictoTile(e.picto, e.ringOf.color(), size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(T.sport(c, e.sportOf).uppercase(), style = UType.Eyebrow.copy(color = e.ringOf.color()))
                        Text(e.name, style = UType.Title)
                    }
                }
            }

            SectionTitle(stringResource(R.string.start_ghost))
            when {
                e.sportOf == Sport.PACE -> UCard {
                    Text(stringResource(R.string.ghost_pacer), style = UType.Strong)
                    Text(stringResource(R.string.ghost_pacer_sub, T.stopwatch(e.target)), style = UType.Muted)
                }
                ghosts.values.all { it == null } -> UCard {
                    Text(stringResource(R.string.ghost_none), style = UType.Strong)
                    Text(stringResource(R.string.ghost_none_sub), style = UType.Muted)
                }
                else -> GhostMode.entries.forEach { m ->
                    val g = ghosts[m]
                    val on = m == mode
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(U.RMd)
                            .background(if (on) U.c.accentSoft else U.c.surface)
                            .border(1.dp, if (on) U.c.accent else U.c.line, U.RMd)
                            .clickable(enabled = g != null) { mode = m }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(T.ghost(c, m), style = UType.Strong.copy(color = if (g == null) U.c.faint else U.c.ink))
                            Text(
                                if (g?.run != null && m != GhostMode.STANDARD) T.ghostSub(c, m) + " · " + T.day(c, g.run.start) else T.ghostSub(c, m),
                                style = UType.Muted,
                            )
                        }
                        Text(g?.let { T.precise(it.total) } ?: "—", style = UType.Time.copy(fontSize = 18.sp, color = if (on) U.c.accent else U.c.muted))
                    }
                }
            }

            if (relay == null) {
                SectionTitle(stringResource(R.string.start_guess))
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.guess_switch), style = UType.Strong)
                            Text(stringResource(R.string.guess_sub), style = UType.Muted)
                        }
                        Switch(guessing, { guessing = it }, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
                    }
                    if (guessing) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Step("−1′") { guess = (guess - 60_000).coerceAtLeast(10_000) }
                            Step("−10″") { guess = (guess - 10_000).coerceAtLeast(10_000) }
                            Text(T.stopwatch(guess), style = UType.Hero.copy(fontSize = 34.sp, textAlign = TextAlign.Center), modifier = Modifier.weight(1f))
                            Step("+10″") { guess += 10_000 }
                            Step("+1′") { guess += 60_000 }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            PrimaryButton(stringResource(R.string.start_go), { countdown = 0 }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        }
    }

    if (countdown >= 0) {
        LaunchedEffect(Unit) {
            for (i in 0..2) {
                countdown = i
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(if (i == 2) 350 else 750)
            }
            scope.launch {
                Heats.start(c, e.id, mode, if (guessing && relay == null) guess else 0, relay)
                go.heat()
            }
        }
        Box(Modifier.fillMaxSize().background(U.c.accent), contentAlignment = Alignment.Center) {
            AnimatedContent(countdown, label = "countdown") { i ->
                Text(
                    stringResource(listOf(R.string.cd_1, R.string.cd_2, R.string.cd_3)[i.coerceIn(0, 2)]),
                    style = UType.Hero.copy(color = Color.White, fontSize = 44.sp, textAlign = TextAlign.Center),
                )
            }
        }
    }
}

@Composable
private fun Step(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp, 40.dp).clip(U.RSm).background(U.c.surface2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = UType.Small.copy(color = U.c.ink, fontSize = 13.sp)) }
}

/** The start button's sheet: every discipline and relay, one tap from the start line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPicker(arena: Arena, go: Go, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = U.c.bg) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
            item {
                Text(stringResource(R.string.picker_title), style = UType.Title)
                Spacer(Modifier.height(10.dp))
            }
            items(arena.lineup, key = { it.id }) { e ->
                Row(
                    Modifier.fillMaxWidth().clip(U.RMd).clickable { onDismiss(); go.start(e.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PictoTile(e.picto, e.ringOf.color(), size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(e.name, style = UType.Strong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Results.record(e, arena.runsOf(e.id))?.let { Text(T.precise(it.duration), style = UType.Time.copy(color = U.c.gold)) }
                }
            }
            if (arena.relays.isNotEmpty()) item { SectionTitle(stringResource(R.string.stadium_relays)) }
            items(arena.relays, key = { "r" + it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clip(U.RMd).clickable { onDismiss(); go.startRelay(r.id) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.name, style = UType.Strong, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.PlayArrow, null, tint = U.c.accent)
                }
            }
            if (arena.lineup.isEmpty()) item {
                Text(stringResource(R.string.stadium_empty), style = UType.Sub)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.event_new), { onDismiss(); go.edit(0) }, Modifier.fillMaxWidth())
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
