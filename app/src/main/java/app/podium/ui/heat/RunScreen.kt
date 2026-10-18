package app.podium.ui.heat

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.container
import app.podium.data.Condition
import app.podium.data.Event
import app.podium.data.Form
import app.podium.data.Run
import app.podium.data.Sport
import app.podium.domain.Award
import app.podium.domain.Relays
import app.podium.domain.Results
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.MedalMark
import app.podium.ui.art.PictoTile
import app.podium.ui.art.Pictograms
import app.podium.ui.art.picto
import app.podium.ui.components.Chip
import app.podium.ui.components.ConfirmDialog
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.components.UTextField
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color
import kotlin.math.abs

/** A heat's page in the protocol; straight after the finish it's the result, with the form to fill in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunScreen(id: Long, fresh: Boolean, arena: Arena, go: Go) {
    val c = ctx()
    val container = c.container
    val run = arena.run(id)
    val e = run?.let { arena.event(it.eventId) }
    if (run == null || e == null) {
        // Just written: the list catches up in a moment.
        LaunchedEffect(arena.runs.size) { if (arena.runs.isNotEmpty() && !fresh) go.back() }
        Box(Modifier.fillMaxSize().background(U.c.bg))
        return
    }
    val award = arena.award(run)
    var note by remember(run.id) { mutableStateOf(run.note) }
    var deleting by remember { mutableStateOf(false) }
    val legs = if (run.relay > 0) arena.runs.filter { it.relay == run.relay }.sortedBy { it.start } else listOf(run)

    /** Form and conditions belong to the whole relay when it was one. */
    fun change(f: (Run) -> Run) = container.write { legs.forEach { l -> arena.run(l.id)?.let { db.dao().update(f(it)) } } }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(if (fresh) stringResource(R.string.run_result) else T.date(c, run.start), if (fresh) null else go::back) {
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.faint) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Headline(e, run, award)
            Spacer(Modifier.height(14.dp))
            Versus(e, run, arena.runsOf(e.id))
            Sections(e, run, arena.runsOf(e.id))
            if (run.relay > 0) RelayBox(run, arena)

            if (run.guess > 0) {
                SectionTitle(stringResource(R.string.run_guess))
                UCard {
                    val err = (run.duration - run.guess).toFloat() / run.guess
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.run_guess_line, T.stopwatch(run.guess), T.precise(run.duration)), style = UType.Body)
                            Text(
                                when {
                                    abs(err) <= 0.05f -> stringResource(R.string.guess_close)
                                    err > 0 -> stringResource(R.string.guess_longer)
                                    else -> stringResource(R.string.guess_shorter)
                                },
                                style = UType.Muted,
                            )
                        }
                        Text(T.percent(err), style = UType.Number.copy(fontSize = 20.sp, color = if (abs(err) <= 0.05f) U.c.ahead else U.c.ink))
                    }
                }
            }

            if (run.pitList.isNotEmpty()) {
                SectionTitle(stringResource(R.string.run_pits))
                UCard {
                    run.pitList.forEach { (r, ms) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(T.pit(c, r), style = UType.Body, modifier = Modifier.weight(1f))
                            Text(T.stopwatch(ms), style = UType.Time)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.run_pits_why), style = UType.Muted)
                }
            }

            SectionTitle(stringResource(R.string.run_form))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Form.entries.forEach { f ->
                    Chip(T.form(c, f), run.formOf == f, { change { it.copy(form = f.name) } }, if (f == Form.RUSHED) U.c.behind else U.c.accent)
                }
            }
            Text(
                stringResource(if (run.formOf == Form.RUSHED) R.string.form_rushed_sub else R.string.form_sub),
                style = UType.Muted, modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )

            SectionTitle(stringResource(R.string.run_conditions))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Condition.entries.forEach { k ->
                    val on = k in run.conditionSet
                    Chip(T.condition(c, k), on, {
                        change { r -> r.copy(conditions = Run.conditionsOf(if (on) r.conditionSet - k else r.conditionSet + k)) }
                    })
                }
            }

            SectionTitle(stringResource(R.string.run_note))
            UTextField(note, { note = it }, placeholder = stringResource(R.string.run_note_hint), minLines = 2)
            LaunchedEffect(note) {
                kotlinx.coroutines.delay(500)
                if (note != run.note) container.write { db.dao().run(run.id)?.let { db.dao().update(it.copy(note = note)) } }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (fresh) Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            PrimaryButton(stringResource(R.string.run_done), {
                if (note != run.note) container.write { db.dao().run(run.id)?.let { db.dao().update(it.copy(note = note)) } }
                go.home()
            }, Modifier.fillMaxWidth())
        }
    }
    if (deleting) ConfirmDialog(
        stringResource(R.string.run_delete_title),
        stringResource(R.string.run_delete_text),
        stringResource(R.string.delete),
        {
            container.write { db.dao().delete(run) }
            if (fresh) go.home() else go.back()
        },
        { deleting = false },
        danger = true,
    )
}

/** The medal, the time and what it means. */
@Composable
private fun Headline(e: Event, run: Run, award: Award?) {
    val c = ctx()
    Row(verticalAlignment = Alignment.CenterVertically) {
        PictoTile(e.picto, e.ringOf.color(), size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(T.weekday(c, T.local(run.start).toLocalDate()) + ", " + T.time(c, run.start), style = UType.Muted)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(T.precise(run.duration), style = UType.Hero.copy(color = if (run.counts) U.c.ink else U.c.faint))
            val line = when {
                !run.counts -> stringResource(R.string.res_dsq)
                award == null -> ""
                award.debut -> stringResource(R.string.res_debut)
                award.record && award.medal != null -> stringResource(R.string.res_record)
                award.medal != null -> c.getString(R.string.res_medal, T.medal(c, award.medal), T.place(c, award.place), award.of)
                award.qualifying -> c.getString(R.string.res_qualifying, award.of, Results.QUALIFYING)
                else -> c.getString(R.string.res_place, T.place(c, award.place), award.of)
            }
            Text(line, style = UType.Name.copy(color = if (award?.record == true && run.counts) U.c.gold else U.c.ink))
            if (award?.record == true && award.qualifying && run.counts) Text(stringResource(R.string.res_record_q, Results.QUALIFYING), style = UType.Muted)
            if (run.manual) Text(stringResource(R.string.run_manual), style = UType.Muted)
        }
        if (award?.medal != null && run.counts) MedalMark(award.medal, award.medal.color(), Modifier.size(54.dp, 84.dp))
    }
}

/** Against the record, the standard and the last time, as they stood before this heat. */
@Composable
private fun Versus(e: Event, run: Run, all: List<Run>) {
    val before = Results.valid(all).filter { it.start < run.start && it.id != run.id }
    if (before.isEmpty()) return
    val c = ctx()
    val record = Results.record(e, before)
    val standard = Results.standard(e, before)
    val last = before.maxBy { it.start }
    UCard {
        fun diff(ref: Long) = Results.score(e, run.duration) - Results.score(e, ref)
        @Composable
        fun line(label: String, ref: Long, neutral: Boolean = false) {
            val d = diff(ref)
            val shown = if (e.sportOf == Sport.MARATHON) -d else d
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = UType.Body, modifier = Modifier.weight(1f))
                Text(T.precise(ref), style = UType.Time.copy(color = U.c.muted, fontSize = 15.sp))
                Text(
                    T.delta(shown),
                    style = UType.Time.copy(fontSize = 15.sp, color = if (neutral) U.c.muted else if (d <= 0) U.c.ahead else U.c.behind),
                    modifier = Modifier.width(88.dp), textAlign = TextAlign.End,
                )
            }
        }
        // Off the target is how far from it, neither good nor bad on its own.
        if (e.sportOf == Sport.PACE) line(stringResource(R.string.vs_target), e.target, neutral = true)
        record?.let { line(stringResource(R.string.vs_record), it.duration) }
        if (e.sportOf != Sport.PACE) standard?.let { line(stringResource(R.string.vs_standard), it) }
        line(stringResource(R.string.vs_last), last.duration)
        if (record != null && run.counts && abs(diff(record.duration)) < 1500) {
            Spacer(Modifier.height(10.dp))
            PhotoFinish(run.duration, record.duration, e)
            Text(c.getString(R.string.photo_finish, T.precise(abs(run.duration - record.duration))), style = UType.Muted)
        }
    }
}

/** Two figures at the line, a hair apart: the photo the judges look at. */
@Composable
private fun PhotoFinish(mine: Long, record: Long, e: Event) {
    val you = U.c.accent
    val ghost = U.c.ghost
    val line = U.c.behind
    val paper = U.c.surface2
    val mineWins = Results.score(e, mine) <= Results.score(e, record)
    Canvas(Modifier.fillMaxWidth().height(86.dp).clip(U.RSm).background(paper)) {
        val fx = size.width * 0.62f
        drawLine(line, Offset(fx, 0f), Offset(fx, size.height), strokeWidth = 3f)
        for (i in 1..6) drawLine(Color.White, Offset(fx - i * 26f, 0f), Offset(fx - i * 26f, size.height), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        val s = size.height * 0.8f
        val run = Pictograms.all[0]
        val lead = fx - s * 0.62f
        val lag = lead - s * 0.28f
        picto(run, ghost.copy(alpha = 0.75f), Offset(if (mineWins) lag else lead, size.height * 0.1f), s)
        picto(run, you, Offset(if (mineWins) lead else lag, size.height * 0.1f), s)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Sections(e: Event, run: Run, all: List<Run>) {
    val n = e.sections.size
    if (n < 2 || run.splitTimes.size != n) return
    val gold = Results.goldSections(e, run, all)
    SectionTitle(stringResource(R.string.event_sections))
    UCard {
        run.sections.forEachIndexed { i, ms ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}. " + e.sections[i], style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (gold.getOrNull(i) == true) {
                    Icon(Icons.Rounded.Star, stringResource(R.string.sec_gold), Modifier.size(16.dp), tint = U.c.gold)
                    Spacer(Modifier.width(4.dp))
                }
                Text(T.precise(ms), style = UType.Time.copy(color = if (gold.getOrNull(i) == true) U.c.gold else U.c.ink))
            }
        }
        if (gold.any { it }) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.sec_gold_why), style = UType.Muted)
        }
    }
}

@Composable
private fun RelayBox(run: Run, arena: Arena) {
    val relay = arena.relay(run.relayId) ?: return
    val c = ctx()
    val me = Relays.of(relay, arena.runs).firstOrNull { it.key == run.relay } ?: return
    val best = Relays.best(relay, arena.runs.filter { it.relay != run.relay })
    SectionTitle(stringResource(R.string.relay_eyebrow) + " · " + relay.name)
    UCard {
        me.legs.forEach { l ->
            val e = arena.event(l.eventId)
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (e != null) PictoTile(e.picto, e.ringOf.color(), size = 28.dp)
                Spacer(Modifier.width(10.dp))
                Text(e?.name.orEmpty(), style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(T.precise(l.duration), style = UType.Time)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(if (me.complete) R.string.relay_total else R.string.relay_unfinished), style = UType.Strong, modifier = Modifier.weight(1f))
            Text(T.precise(me.total), style = UType.Number.copy(fontSize = 20.sp))
        }
        if (best != null && me.complete) {
            val d = me.total - best.total
            Text(
                c.getString(R.string.relay_vs_best, T.delta(d)),
                style = UType.Muted.copy(color = if (d <= 0) U.c.ahead else U.c.muted),
            )
        }
    }
}
