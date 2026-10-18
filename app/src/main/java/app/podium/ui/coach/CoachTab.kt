package app.podium.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.domain.Insights
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.PictoTile
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.UCard
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The coach's notes: what the protocol says when you read all of it. How good your entries are, what
 * slows you down, where the time goes in the pits, and where you've stopped getting faster.
 */
@Composable
fun CoachTab(arena: Arena, go: Go) {
    val c = ctx()
    val calls = Insights.calls(arena.events, arena.runs)
    val overall = Insights.allCalls(arena.runs)
    val effects = Insights.effects(arena.events, arena.runs)
    val pits = Insights.pits(arena.runs)
    val forms = arena.lineup.map { it to Insights.form(it, arena.runsOf(it.id)) }
    val moved = forms.filter { it.second.trend != null && abs(it.second.trend!!) >= 0.03f }.sortedBy { it.second.trend }
    val plateaus = forms.filter { it.second.plateau }
    val nothing = overall == null && effects.isEmpty() && pits.isEmpty() && moved.isEmpty() && plateaus.isEmpty()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            Text(stringResource(R.string.coach_eyebrow).uppercase(), style = UType.Eyebrow)
            Text(stringResource(R.string.tab_coach), style = UType.Display)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.coach_why), style = UType.Sub)
        }
        if (nothing) item {
            Spacer(Modifier.height(16.dp))
            UCard { Text(stringResource(R.string.coach_empty), style = UType.Sub) }
        }

        if (overall != null) {
            item { SectionTitle(stringResource(R.string.coach_calls)) }
            item {
                UCard {
                    val pct = (overall.bias * 100).roundToInt()
                    Text(
                        when {
                            abs(pct) < 5 -> stringResource(R.string.calls_good)
                            pct > 0 -> stringResource(R.string.calls_under, pct)
                            else -> stringResource(R.string.calls_over, -pct)
                        },
                        style = UType.Name,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.calls_close, overall.close, overall.count), style = UType.Muted)
                    if (calls.size > 1) {
                        Spacer(Modifier.height(10.dp))
                        calls.forEach { k ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp, 18.dp).clip(U.RSm).background(k.ring.color()))
                                Spacer(Modifier.width(10.dp))
                                Text(T.ring(c, k.ring), style = UType.Body, modifier = Modifier.weight(1f))
                                Text(T.percent(k.bias), style = UType.Time.copy(color = if (abs(k.bias) < 0.05f) U.c.ahead else U.c.ink))
                            }
                        }
                        Text(stringResource(R.string.calls_key), style = UType.Small)
                    }
                }
            }
        }

        if (moved.isNotEmpty() || plateaus.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.coach_progress)) }
            item {
                UCard {
                    moved.forEach { (e, f) ->
                        Row(Modifier.clickable { go.event(e.id) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            PictoTile(e.picto, e.ringOf.color(), size = 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(e.name, style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val t = f.trend!!
                            Text(
                                stringResource(if (t < 0) R.string.progress_better else R.string.progress_worse, (abs(t) * 100).roundToInt()),
                                style = UType.Time.copy(fontSize = 15.sp, color = if (t < 0) U.c.ahead else U.c.behind),
                            )
                        }
                    }
                    if (moved.isNotEmpty()) Text(stringResource(R.string.progress_key), style = UType.Small)
                    plateaus.forEach { (e, _) ->
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.accentSoft).clickable { go.event(e.id) }.padding(12.dp)) {
                            Text(stringResource(R.string.coach_plateau, e.name), style = UType.Sub.copy(color = U.c.ink))
                        }
                    }
                }
            }
        }

        if (effects.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.coach_conditions)) }
            item {
                UCard {
                    effects.forEach { ef ->
                        val p = (ef.effect * 100).roundToInt()
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(T.condition(c, ef.condition), style = UType.Body, modifier = Modifier.weight(1f))
                            Text(
                                when {
                                    abs(p) < 3 -> stringResource(R.string.effect_none)
                                    p > 0 -> stringResource(R.string.effect_slower, p)
                                    else -> stringResource(R.string.effect_faster, -p)
                                },
                                style = UType.Time.copy(fontSize = 15.sp, color = if (p > 2) U.c.behind else if (p < -2) U.c.ahead else U.c.muted),
                            )
                        }
                    }
                    Text(stringResource(R.string.effect_key), style = UType.Small)
                }
            }
        }

        if (pits.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.coach_pits)) }
            item {
                UCard {
                    pits.forEach { (r, ms) ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text(T.pit(c, r), style = UType.Body, modifier = Modifier.weight(1f))
                            Text(T.stopwatch(ms), style = UType.Time)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.coach_pits_why), style = UType.Muted)
                }
            }
        }
    }
}
