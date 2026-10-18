package app.podium.ui.stadium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.container
import app.podium.data.Event
import app.podium.data.Relay
import app.podium.data.Sport
import app.podium.domain.GamesCalendar
import app.podium.domain.Relays
import app.podium.domain.Results
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.PictoTile
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.SoftButton
import app.podium.ui.components.Stat
import app.podium.ui.components.Tag
import app.podium.ui.components.TallyRow
import app.podium.ui.components.UCard
import app.podium.ui.components.rememberNow
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

@Composable
fun StadiumTab(arena: Arena, go: Go) {
    val c = ctx()
    val g = arena.games
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (g.number >= 1) c.getString(R.string.games_eyebrow, g.number, T.range(c, g.from, g.to)).uppercase() else "",
                        style = UType.Eyebrow,
                    )
                    Text(stringResource(R.string.tab_stadium), style = UType.Display)
                }
                IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
            }
            Spacer(Modifier.height(12.dp))
        }
        item { GamesStrip(arena, go) }
        if (arena.settings.heat.active) item { Running(arena, go) }
        val closed = g.number - 1
        if (closed >= 1 && arena.settings.closedSeen < closed && arena.settings.ceremony) {
            val past = arena.games(closed)
            if (arena.placed.any { arena.date(it.run.start) in past }) item { Ceremony(closed, arena, go) }
        }

        item { SectionTitle(stringResource(R.string.stadium_events)) }
        if (arena.lineup.isEmpty()) item {
            UCard {
                Text(stringResource(R.string.stadium_empty), style = UType.Sub)
                Spacer(Modifier.height(12.dp))
                SoftButton(stringResource(R.string.event_new), { go.edit(0) }, icon = Icons.Rounded.Add)
            }
        }
        items(arena.lineup, key = { it.id }) { e ->
            EventCard(e, arena, go)
            Spacer(Modifier.height(10.dp))
        }
        if (arena.lineup.isNotEmpty()) item {
            SoftButton(stringResource(R.string.event_new), { go.edit(0) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        }

        item { SectionTitle(stringResource(R.string.stadium_relays)) }
        items(arena.relays, key = { "r" + it.id }) { r ->
            RelayCard(r, arena, go)
            Spacer(Modifier.height(10.dp))
        }
        item {
            if (arena.relays.isEmpty()) {
                Text(stringResource(R.string.relay_why), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
            }
            SoftButton(stringResource(R.string.relay_new), { go.relay(0) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        }
        val retired = arena.events.filter { it.retired }
        if (retired.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.stadium_retired)) }
            items(retired, key = { "x" + it.id }) { e ->
                Row(
                    Modifier.fillMaxWidth().clip(U.RMd).clickable { go.event(e.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PictoTile(e.picto, U.c.ghost, size = 34.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(e.name, style = UType.Body.copy(color = U.c.muted), modifier = Modifier.weight(1f))
                    Results.record(e, arena.runsOf(e.id))?.let { Text(T.precise(it.duration), style = UType.Time.copy(color = U.c.muted)) }
                }
            }
        }
    }
}

/** This Games at a glance: days to the closing ceremony, the flame, the medals so far. */
@Composable
private fun GamesStrip(arena: Arena, go: Go) {
    val g = arena.games
    val inGames = arena.placed.filter { arena.date(it.run.start) in g }
    val tally = GamesCalendar.tally(inGames)
    val left = (g.to.toEpochDay() - arena.today.toEpochDay()).toInt()
    val today = arena.runs.count { arena.date(it.start) == arena.today }
    UCard(onClick = { if (g.number >= 1) go.games(g.number) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(stringResource(R.string.games_left), pluralStringResource(R.plurals.days, left, left), Modifier.weight(1f))
            Stat(stringResource(R.string.games_today), "$today", Modifier.weight(0.7f))
            Column(Modifier.weight(0.8f)) {
                Text(stringResource(R.string.games_flame).uppercase(), style = UType.Eyebrow, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(22.dp), tint = if (arena.flame > 0) U.c.behind else U.c.line)
                    Text("${arena.flame}", style = UType.Number.copy(fontSize = 22.sp, color = if (arena.flame > 0) U.c.ink else U.c.faint))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.games_medals), style = UType.Muted, modifier = Modifier.weight(1f))
            TallyRow(tally)
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Running(arena: Arena, go: Go) {
    val h = arena.settings.heat
    val e = arena.event(h.eventId) ?: return
    val now = rememberNow(1000)
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(U.c.aheadSoft).clickable(onClick = go::heat).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PictoTile(e.picto, U.c.ahead, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(if (h.paused) R.string.running_pit else R.string.running_now).uppercase(), style = UType.Eyebrow.copy(color = U.c.ahead))
            Text(e.name, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(T.stopwatch(h.elapsed(now)), style = UType.Number.copy(color = U.c.ahead))
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Ceremony(n: Int, arena: Arena, go: Go) {
    val container = ctx().container
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(U.c.accent).clickable {
            container.write { settings.update { it.copy(closedSeen = maxOf(it.closedSeen, n)) } }
            go.games(n)
        }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ceremony_banner_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = Color.White.copy(alpha = 0.8f)))
            Text(stringResource(R.string.ceremony_banner, n), style = UType.Name.copy(color = Color.White))
        }
        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun EventCard(e: Event, arena: Arena, go: Go) {
    val c = ctx()
    val runs = arena.runsOf(e.id)
    val record = Results.record(e, runs)
    val last = runs.firstOrNull()
    val valid = Results.valid(runs).size
    UCard(onClick = { go.event(e.id) }, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PictoTile(e.picto, e.ringOf.color(), size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.name, style = UType.Name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(
                            T.sport(c, e.sportOf),
                            if (e.sections.size >= 2) c.resources.getQuantityString(R.plurals.sections, e.sections.size, e.sections.size) else null,
                            if (e.sportOf == Sport.PACE) T.stopwatch(e.target) else null,
                        ).joinToString(" · "),
                        style = UType.Muted,
                    )
                    if (valid < Results.QUALIFYING) {
                        Spacer(Modifier.width(8.dp))
                        Tag(c.getString(R.string.tag_q, valid, Results.QUALIFYING), U.c.accent)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(U.c.accentSoft).clickable { go.start(e.id) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.start_cd), tint = U.c.accent, modifier = Modifier.size(28.dp)) }
        }
        if (record != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(U.RSm).background(U.c.surface2).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.event_record).uppercase(), style = UType.Eyebrow)
                    Text(T.precise(record.duration), style = UType.Time.copy(color = U.c.gold, fontSize = 18.sp))
                }
                if (last != null) Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.event_last).uppercase(), style = UType.Eyebrow)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(T.precise(last.duration), style = UType.Time.copy(fontSize = 18.sp))
                        if (last.id != record.id && last.counts) {
                            val d = Results.score(e, last.duration) - Results.score(e, record.duration)
                            Spacer(Modifier.width(6.dp))
                            Text(T.delta(if (e.sportOf == Sport.MARATHON) -d else d), style = UType.Small.copy(color = U.c.faint))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelayCard(r: Relay, arena: Arena, go: Go) {
    val legs = r.legIds.mapNotNull { arena.event(it) }
    val best = Relays.best(r, arena.runs)
    UCard(onClick = { go.relay(r.id) }, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, style = UType.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    legs.forEachIndexed { i, e ->
                        if (i > 0) Box(Modifier.size(8.dp, 2.dp).background(U.c.line))
                        PictoTile(e.picto, e.ringOf.color(), size = 28.dp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (best != null) {
                    Text(stringResource(R.string.event_record).uppercase(), style = UType.Eyebrow)
                    Text(T.precise(best.total), style = UType.Time.copy(color = U.c.gold, fontSize = 18.sp))
                    Spacer(Modifier.height(6.dp))
                }
                Box(
                    Modifier.size(42.dp).clip(CircleShape).border(1.5.dp, U.c.accent, CircleShape).clickable(enabled = legs.isNotEmpty()) { go.startRelay(r.id) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.start_cd), tint = U.c.accent) }
            }
        }
    }
}
