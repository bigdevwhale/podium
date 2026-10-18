package app.podium.ui.heat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.data.Event
import app.podium.data.Finished
import app.podium.data.Heat
import app.podium.data.Heats
import app.podium.data.Sport
import app.podium.domain.Board
import app.podium.domain.Ghost
import app.podium.domain.Results
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.Stadium
import app.podium.ui.components.Chip
import app.podium.ui.components.ConfirmDialog
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.SoftButton
import app.podium.ui.components.rememberNow
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The heat under way: the stadium, the clock, the scoreboard against the ghost, and the buttons. */
@Composable
fun HeatScreen(arena: Arena, go: Go) {
    val c = ctx()
    val h = arena.settings.heat
    val e = arena.event(h.eventId)
    // Remembered before the heat can vanish below: the finish's coroutine must outlive the cleared heat.
    val scope = rememberCoroutineScope()
    // Set on the way to the result, so the cleared heat doesn't send us home first.
    var leaving by remember { mutableStateOf(false) }
    if (!h.active || e == null) {
        LaunchedEffect(Unit) {
            // On the way to the result: give it a moment, then make sure we're not left on an empty track.
            if (leaving) delay(2000)
            go.leaveHeat()
        }
        return
    }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val prior = arena.runsOf(e.id).filter { it.start < h.since }
    val ghost = remember(e, h.ghost, prior.size, h.since) { Results.ghost(e, prior, h.ghost, h.since) }
    val now = rememberNow(100)
    val t = h.elapsed(now)
    val board = Results.board(e, ghost, h.splits, t)
    var quitting by remember { mutableStateOf(false) }
    val relay = arena.relay(h.relayId)

    // A buzz when a marathon passes the record or a pace reaches its target.
    var passed by remember(h.since) { mutableStateOf(board.toMark < 0) }
    LaunchedEffect(board.toMark < 0) {
        if (board.toMark < 0 && !passed && ghost != null && e.sportOf != Sport.SPRINT && arena.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        passed = board.toMark < 0
    }

    fun split() {
        val at = System.currentTimeMillis()
        if (arena.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            if (h.splits.size + 1 >= e.sections.size) leaving = true
            when (val f = Heats.split(c, at)) {
                is Finished.Done -> go.finished(f.runId)
                else -> leaving = false
            }
        }
    }

    fun finish() {
        val at = System.currentTimeMillis()
        if (arena.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        leaving = relay == null || h.leg + 1 >= relay.legIds.size
        scope.launch {
            when (val f = Heats.finish(c, at)) {
                is Finished.Done -> go.finished(f.runId)
                else -> leaving = false
            }
        }
    }

    val n = e.sections.size
    val last = n >= 2 && h.splits.size + 1 >= n
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(
                (if (relay != null) c.getString(R.string.heat_leg, h.leg + 1, relay.legIds.size, relay.name) else T.sport(c, e.sportOf)).uppercase(),
                style = UType.Eyebrow,
            )
            Text(e.name, style = UType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Stadium(if (ghost != null) board.ghostAt else null, board.youAt, n, Modifier.fillMaxWidth().height(190.dp))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(U.c.accent))
                Text(" " + stringResource(R.string.heat_you) + "    ", style = UType.Small)
                if (ghost != null) {
                    Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(U.c.ghost))
                    Text(" " + (if (ghost.pacer) stringResource(R.string.ghost_pacer) else T.ghost(c, ghost.mode)) + " " + T.precise(ghost.total), style = UType.Small)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (n >= 2) {
                Text(
                    c.getString(R.string.live_section, h.splits.size + 1, n, e.sections[h.splits.size.coerceAtMost(n - 1)]).uppercase(),
                    style = UType.Eyebrow.copy(color = U.c.accent),
                )
            }
            Text(T.precise(t), style = UType.Clock.copy(textAlign = TextAlign.Center, color = if (h.paused) U.c.faint else U.c.ink))
            Scoreboard(e, ghost, board, h)
            Spacer(Modifier.height(14.dp))
            Taken(e, ghost, h, prior)
            if (h.guess > 0) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.heat_guess, T.stopwatch(h.guess)), style = UType.Muted)
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            if (h.paused) PitStop(h) else {
                if (n >= 2 && !last) {
                    PrimaryButton(
                        stringResource(R.string.heat_split_to, e.sections[(h.splits.size + 1).coerceAtMost(n - 1)]),
                        ::split, Modifier.fillMaxWidth().height(72.dp), icon = Icons.Rounded.Flag,
                    )
                } else {
                    PrimaryButton(stringResource(R.string.heat_finish), ::finish, Modifier.fillMaxWidth().height(72.dp), color = U.c.ahead, icon = Icons.Rounded.Flag)
                }
                Spacer(Modifier.height(10.dp))
                SoftButton(stringResource(R.string.heat_pit), { scope.launch { Heats.pause(c) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.LocalGasStation)
            }
            TextButton(onClick = { quitting = true }, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.heat_quit), color = U.c.faint, style = UType.Button)
            }
        }
    }
    if (quitting) ConfirmDialog(
        stringResource(R.string.heat_quit_title),
        stringResource(if (relay != null) R.string.heat_quit_relay else R.string.heat_quit_text),
        stringResource(R.string.heat_quit_go),
        { scope.launch { Heats.cancel(c); go.home() } },
        { quitting = false },
        danger = true,
    )
}

/** The line under the clock: ahead, behind, or how long the ghost still has. */
@Composable
private fun Scoreboard(e: Event, ghost: Ghost?, board: Board, h: Heat) {
    val c = ctx()
    val (text, color) = when {
        ghost == null -> stringResource(R.string.board_first) to U.c.muted
        e.sportOf == Sport.SPRINT -> when {
            board.toMark < 0 -> c.getString(R.string.board_behind, T.precise(-board.toMark)) to U.c.behind
            board.lastSplit != null -> c.getString(R.string.board_split, T.delta(board.lastSplit)) to (if (board.lastSplit <= 0) U.c.ahead else U.c.behind)
            board.finishMark -> c.getString(R.string.board_ghost_finish, T.stopwatch(board.toMark)) to U.c.muted
            else -> c.getString(R.string.board_ghost_split, T.stopwatch(board.toMark)) to U.c.muted
        }
        e.sportOf == Sport.MARATHON -> if (board.toMark >= 0) c.getString(R.string.board_to_record, T.stopwatch(board.toMark)) to U.c.muted
        else c.getString(R.string.board_record, T.precise(-board.toMark)) to U.c.ahead
        else -> if (board.toMark >= 0) c.getString(R.string.board_to_target, T.stopwatch(board.toMark)) to U.c.muted
        else c.getString(R.string.board_over, T.precise(-board.toMark)) to U.c.behind
    }
    Box(Modifier.clip(U.RSm).background(if (color == U.c.muted) U.c.surface2 else color.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(if (h.paused) stringResource(R.string.board_paused) else text, style = UType.Strong.copy(color = if (h.paused) U.c.muted else color, textAlign = TextAlign.Center))
    }
}

/** The splits so far, against the ghost's, with a star for a section never run faster. */
@Composable
private fun Taken(e: Event, ghost: Ghost?, h: Heat, prior: List<app.podium.data.Run>) {
    if (h.splits.isEmpty()) return
    val best = Results.bestSections(e, prior)
    h.splits.forEachIndexed { i, s ->
        val sec = s - (h.splits.getOrNull(i - 1) ?: 0L)
        val g = ghost?.splits?.getOrNull(i)?.takeIf { ghost.splits.size == e.sections.size }
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}. " + e.sections.getOrElse(i) { "" }, style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (best.size == e.sections.size && sec < best[i]) {
                Icon(Icons.Rounded.Star, null, Modifier.size(16.dp), tint = U.c.gold)
                Spacer(Modifier.width(4.dp))
            }
            Text(T.precise(sec), style = UType.Time.copy(color = U.c.muted, fontSize = 15.sp))
            Spacer(Modifier.width(12.dp))
            Text(
                g?.let { T.delta(s - it) } ?: "",
                style = UType.Time.copy(fontSize = 15.sp, color = if (g != null && s <= g) U.c.ahead else U.c.behind),
                modifier = Modifier.width(78.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

/** In the pits: the clock is stopped; say what took the time, and you're back on the track. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PitStop(h: Heat) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val now = rememberNow(1000)
    Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.behindSoft).padding(14.dp)) {
        Text(stringResource(R.string.pit_title, T.stopwatch(now - h.pausedAt)), style = UType.Name)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.pit_sub), style = UType.Muted)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            T.pitReasons.forEach { r -> Chip(T.pit(c, r), false, { scope.launch { Heats.resume(c, r) } }, U.c.behind) }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton(stringResource(R.string.live_resume), { scope.launch { Heats.resume(c) } }, Modifier.fillMaxWidth(), color = U.c.accent)
    }
}
