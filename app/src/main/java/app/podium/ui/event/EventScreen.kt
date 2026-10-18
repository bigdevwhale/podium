package app.podium.ui.event

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.data.Event
import app.podium.data.Run
import app.podium.data.Sport
import app.podium.domain.Insights
import app.podium.domain.Medal
import app.podium.domain.Results
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.Chart
import app.podium.ui.art.MedalMark
import app.podium.ui.art.PictoTile
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.RunRow
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.SoftButton
import app.podium.ui.components.Stat
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EventScreen(id: Long, arena: Arena, go: Go) {
    val e = arena.event(id) ?: return
    val c = ctx()
    val runs = arena.runsOf(id)
    val valid = Results.valid(runs)
    val form = Insights.form(e, runs)
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(e.name, go::back) {
            IconButton(onClick = { go.edit(e.id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PictoTile(e.picto, e.ringOf.color(), size = 72.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text((T.ring(c, e.ringOf) + " · " + T.sport(c, e.sportOf)).uppercase(), style = UType.Eyebrow.copy(color = e.ringOf.color()))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (e.sportOf == Sport.PACE) c.getString(R.string.sport_pace_rule_n, T.stopwatch(e.target)) else T.sportRule(c, e.sportOf),
                        style = UType.Sub,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            if (valid.size < Results.QUALIFYING) {
                UCard {
                    Text(stringResource(R.string.qual_title).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
                    Spacer(Modifier.height(4.dp))
                    val left = Results.QUALIFYING - valid.size
                    Text(c.resources.getQuantityString(R.plurals.qual_left, left, left), style = UType.Name)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.qual_why), style = UType.Sub)
                }
                Spacer(Modifier.height(12.dp))
            }
            val podium = Results.podium(e, runs)
            if (podium.isNotEmpty()) Podium(podium)

            if (valid.size >= 2) {
                SectionTitle(stringResource(R.string.event_curve))
                UCard {
                    val points = runs.sortedBy { it.start }.map { it.duration to it.counts }
                    val better: (Long, Long) -> Boolean = { a, b -> Results.score(e, a) < Results.score(e, b) }
                    Chart(points, better, Results.standard(e, runs).takeIf { e.sportOf != Sport.PACE } ?: e.target.takeIf { e.sportOf == Sport.PACE }, Modifier.fillMaxWidth().height(150.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.event_curve_key), style = UType.Small)
                }
            }

            if (form.heats > 0) {
                SectionTitle(stringResource(R.string.event_form))
                UCard {
                    Row {
                        Stat(stringResource(R.string.stat_heats), "${form.heats}", Modifier.weight(1f))
                        Stat(stringResource(R.string.stat_standard), form.median?.let(T::precise) ?: "—", Modifier.weight(1.2f))
                        Stat(stringResource(R.string.stat_spread), form.spread?.let { "±" + (it * 100).roundToInt() + "%" } ?: "—", Modifier.weight(1f))
                    }
                    form.trend?.let { t ->
                        Spacer(Modifier.height(14.dp))
                        val pct = abs(t * 100).roundToInt()
                        Text(
                            when {
                                pct < 2 -> stringResource(R.string.trend_flat)
                                t < 0 -> stringResource(R.string.trend_better, pct)
                                else -> stringResource(R.string.trend_worse, pct)
                            },
                            style = UType.Body.copy(color = if (t < 0) U.c.ahead else U.c.ink),
                        )
                    }
                    if (form.sinceRecord > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(c.resources.getQuantityString(R.plurals.since_record, form.sinceRecord, form.sinceRecord), style = UType.Muted)
                    }
                    if (form.plateau) {
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.accentSoft).padding(12.dp)) {
                            Text(stringResource(R.string.plateau), style = UType.Sub.copy(color = U.c.ink))
                        }
                    }
                }
            }

            Sections(e, runs)

            if (runs.isNotEmpty()) {
                SectionTitle(stringResource(R.string.event_heats))
                runs.take(30).forEach { r -> RunRow(r, e, arena.award(r), { go.run(r.id) }, showEvent = false) }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.fillMaxWidth().background(U.c.bg).navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(stringResource(R.string.manual_add), { go.manual(e.id) }, icon = Icons.Rounded.EditCalendar)
            PrimaryButton(stringResource(R.string.start_cd), { go.start(e.id) }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
        }
    }
}

/** Three steps, gold in the middle, as at a medal ceremony. */
@Composable
private fun Podium(top: List<Run>) {
    val c = ctx()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val order = listOf(1, 0, 2)
        order.forEach { i ->
            val r = top.getOrNull(i)
            val m = Medal.entries[i]
            val h: Dp = when (i) { 0 -> 84.dp; 1 -> 60.dp; else -> 44.dp }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (r != null) {
                    MedalMark(m, m.color(), Modifier.size(26.dp, 40.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(T.precise(r.duration), style = UType.Time.copy(fontSize = if (i == 0) 20.sp else 16.sp))
                    Text(T.day(c, r.start), style = UType.Small)
                    Spacer(Modifier.height(6.dp))
                }
                Box(
                    Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(if (r != null) U.c.accent.copy(alpha = 0.85f - i * 0.15f) else U.c.line),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text("${i + 1}", style = UType.Display.copy(color = Color.White, textAlign = TextAlign.Center), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Each section's best and its usual, and the perfect heat they'd make together. */
@Composable
private fun Sections(e: Event, runs: List<Run>) {
    val names = e.sections
    if (names.size < 2) return
    val full = Results.split(e, runs)
    if (full.isEmpty()) return
    val best = Results.bestSections(e, runs)
    SectionTitle(stringResource(R.string.event_sections))
    UCard {
        Row {
            Text("", Modifier.weight(1f))
            Text(stringResource(R.string.sec_best).uppercase(), style = UType.Eyebrow, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
            Text(stringResource(R.string.sec_usual).uppercase(), style = UType.Eyebrow, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
        }
        names.forEachIndexed { i, n ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}. $n", style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1)
                Text(T.precise(best[i]), style = UType.Time.copy(color = U.c.gold), modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
                Text(T.precise(Results.median(full.map { it.sections[i] })), style = UType.Time.copy(color = U.c.muted), modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
            }
        }
        Results.sumOfBest(e, runs)?.let { sob ->
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.surface2).padding(12.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.sum_of_best), style = UType.Strong, modifier = Modifier.weight(1f))
                        Text(T.precise(sob), style = UType.Time.copy(color = U.c.gold, fontSize = 18.sp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.sum_of_best_why), style = UType.Muted)
                }
            }
        }
    }
}
