package app.podium.ui.medals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.container
import app.podium.data.Ring
import app.podium.domain.GamesCalendar
import app.podium.domain.Placed
import app.podium.domain.Results
import app.podium.domain.Tally
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.PictoTile
import app.podium.ui.components.RunRow
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.Stat
import app.podium.ui.components.TallyRow
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

@Composable
fun MedalsTab(arena: Arena, go: Go) {
    val c = ctx()
    val g = arena.games
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            Text((if (g.number >= 1) c.getString(R.string.games_n, g.number) else "").uppercase(), style = UType.Eyebrow)
            Text(stringResource(R.string.tab_medals), style = UType.Display)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.medals_why), style = UType.Sub)
        }
        if (g.number >= 1) {
            item { SectionTitle(stringResource(R.string.medals_table)) }
            item { MedalTable(GamesCalendar.table(arena.placed, g, arena.zone)) { go.games(g.number) } }
        }
        val past = (g.number - 1 downTo 1).toList()
        if (past.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.medals_games)) }
            items(past, key = { "g$it" }) { n ->
                val gg = arena.games(n)
                val inG = arena.placed.filter { arena.date(it.run.start) in gg }
                UCard(onClick = { go.games(n) }, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.getString(R.string.games_n, n), style = UType.Strong)
                            Text(T.range(c, gg.from, gg.to) + " · " + pluralStringResource(R.plurals.heats, inG.size, inG.size), style = UType.Muted)
                        }
                        TallyRow(GamesCalendar.tally(inG), size = 16.dp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        item { SectionTitle(stringResource(R.string.medals_cabinet)) }
        item {
            val all = GamesCalendar.tally(arena.placed)
            UCard {
                Row {
                    Stat(stringResource(R.string.cab_medals), "${all.total}", Modifier.weight(1f))
                    Stat(stringResource(R.string.cab_records), "${arena.placed.count { it.award.record }}", Modifier.weight(1f))
                    Stat(stringResource(R.string.stat_heats), "${arena.runs.size}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                TallyRow(all)
            }
            Spacer(Modifier.height(10.dp))
        }
        items(arena.events.filter { arena.runsOf(it.id).isNotEmpty() }, key = { "e" + it.id }) { e ->
            val mine = arena.placed.filter { it.event.id == e.id }
            Row(
                Modifier.fillMaxWidth().clip(U.RMd).padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PictoTile(e.picto, e.ringOf.color(), size = 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = UType.Strong.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Results.record(e, arena.runsOf(e.id))?.let { Text(stringResource(R.string.cab_record, T.precise(it.duration), T.date(c, it.start)), style = UType.Muted) }
                }
                TallyRow(GamesCalendar.tally(mine), size = 15.dp, showZero = false)
            }
        }
    }
}

/** The table of one Games: parts of life instead of nations, ordered gold first. */
@Composable
fun MedalTable(table: Map<Ring, Tally>, onClick: (() -> Unit)? = null) {
    val c = ctx()
    UCard(onClick = onClick) {
        val rows = table.toList().sortedByDescending { it.second.rank }
        if (rows.isEmpty()) {
            Text(stringResource(R.string.medals_none), style = UType.Sub)
            return@UCard
        }
        rows.forEachIndexed { i, (ring, t) ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = UType.Time.copy(color = U.c.faint), modifier = Modifier.width(22.dp))
                Box(Modifier.size(6.dp, 22.dp).clip(U.RSm).background(ring.color()))
                Spacer(Modifier.width(10.dp))
                Text(T.ring(c, ring), style = UType.Strong, modifier = Modifier.weight(1f))
                TallyRow(t, size = 16.dp)
            }
        }
        if (rows.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.medals_total), style = UType.Muted, modifier = Modifier.weight(1f).padding(start = 38.dp))
                TallyRow(rows.fold(Tally()) { a, b -> a + b.second }, size = 16.dp)
            }
        }
    }
}

/** One Games, or its closing ceremony once it's over. */
@Composable
fun GamesScreen(n: Int, arena: Arena, go: Go) {
    val c = ctx()
    val g = arena.games(n)
    val over = n < arena.games.number
    val inG: List<Placed> = arena.placed.filter { arena.date(it.run.start) in g }
    LaunchedEffect(n) { if (over) c.container.write { settings.update { it.copy(closedSeen = maxOf(it.closedSeen, n)) } } }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(c.getString(R.string.games_n, n), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(
                (if (over) stringResource(R.string.ceremony_title) else stringResource(R.string.games_running)).uppercase() + " · " + T.range(c, g.from, g.to),
                style = UType.Eyebrow.copy(color = U.c.accent),
            )
            Spacer(Modifier.height(4.dp))
            val tally = GamesCalendar.tally(inG)
            Text(
                if (inG.isEmpty()) stringResource(R.string.games_empty)
                else c.getString(R.string.games_summary, tally.gold, tally.silver, tally.bronze),
                style = UType.Title,
            )
            SectionTitle(stringResource(R.string.medals_table))
            MedalTable(GamesCalendar.table(arena.placed, g, arena.zone))
            Spacer(Modifier.height(10.dp))
            UCard {
                Row {
                    Stat(stringResource(R.string.stat_heats), "${inG.size}", Modifier.weight(1f))
                    Stat(stringResource(R.string.games_days), "${inG.map { arena.date(it.run.start) }.distinct().size}", Modifier.weight(1f))
                    Stat(stringResource(R.string.cab_records), "${inG.count { it.award.record }}", Modifier.weight(1f))
                }
            }
            val records = inG.filter { it.award.record }
            if (records.isNotEmpty()) {
                SectionTitle(stringResource(R.string.games_records_set))
                records.forEach { p -> RunRow(p.run, p.event, p.award, { go.run(p.run.id) }) }
            }
            val best = GamesCalendar.gamesRecords(arena.placed, g, arena.zone)
            if (best.isNotEmpty()) {
                SectionTitle(stringResource(R.string.games_bests))
                Text(stringResource(R.string.games_bests_why), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                best.forEach { (id, r) -> RunRow(r, arena.event(id), arena.award(r), { go.run(r.id) }) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
