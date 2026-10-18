package app.podium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.data.Event
import app.podium.data.Run
import app.podium.domain.Award
import app.podium.domain.Medal
import app.podium.domain.Tally
import app.podium.ui.T
import app.podium.ui.art.MedalMark
import app.podium.ui.art.PictoTile
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

/** A small coloured label: "DSQ", "Q 2/3", "WR". */
@Composable
fun Tag(text: String, color: Color, soft: Color = color.copy(alpha = 0.14f), modifier: Modifier = Modifier) {
    Box(modifier.clip(U.RSm).background(soft).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(text, style = UType.Small.copy(color = color, fontSize = 11.sp, letterSpacing = 0.04.sp), maxLines = 1)
    }
}

/** Gold, silver and bronze counts side by side. */
@Composable
fun TallyRow(t: Tally, modifier: Modifier = Modifier, size: Dp = 18.dp, showZero: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Medal.GOLD to t.gold, Medal.SILVER to t.silver, Medal.BRONZE to t.bronze).forEach { (m, n) ->
            if (n == 0 && !showZero) return@forEach
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedalMark(m, m.color(), Modifier.size(size * 0.8f, size * 1.25f))
                Spacer(Modifier.width(4.dp))
                Text("$n", style = UType.Time.copy(fontSize = 15.sp, color = if (n == 0) U.c.faint else U.c.ink))
            }
        }
    }
}

/** What a heat won, as one short mark for a row. */
@Composable
fun AwardMark(run: Run, award: Award?, modifier: Modifier = Modifier) {
    val c = ctx()
    when {
        !run.counts -> Tag(stringResource(R.string.tag_dsq), U.c.behind, modifier = modifier)
        award == null -> Unit
        award.medal != null -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            if (award.record) {
                Tag(stringResource(R.string.tag_record), U.c.gold)
                Spacer(Modifier.width(6.dp))
            }
            MedalMark(award.medal, award.medal.color(), Modifier.size(16.dp, 24.dp))
        }
        award.qualifying -> Tag(c.getString(R.string.tag_q, award.of, app.podium.domain.Results.QUALIFYING), U.c.accent, modifier = modifier)
        else -> Text(T.place(c, award.place), style = UType.Small, modifier = modifier)
    }
}

/** One heat in a list: the discipline, the time, when, and what it won. */
@Composable
fun RunRow(run: Run, event: Event?, award: Award?, onClick: () -> Unit, showEvent: Boolean = true) {
    val c = ctx()
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showEvent && event != null) {
            PictoTile(event.picto, event.ringOf.color(), size = 38.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(if (showEvent) event?.name.orEmpty() else T.date(c, run.start), style = UType.Strong.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            val bits = buildList {
                add(T.time(c, run.start))
                if (run.manual) add(c.getString(R.string.run_manual_short))
                if (run.pitMs > 0) add(c.getString(R.string.run_pits_short, T.stopwatch(run.pitMs)))
                run.conditionSet.forEach { add(T.condition(c, it).lowercase()) }
            }
            Text(bits.joinToString(" · "), style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(T.precise(run.duration), style = UType.Time.copy(color = if (run.counts) U.c.ink else U.c.faint))
            Spacer(Modifier.height(3.dp))
            AwardMark(run, award)
        }
    }
}

/** A label above a big number, for stat rows. */
@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, color: Color = U.c.ink, sub: String? = null) {
    Column(modifier) {
        Text(label.uppercase(), style = UType.Eyebrow, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = UType.Number.copy(color = color, fontSize = 22.sp), maxLines = 1)
        if (sub != null) Text(sub, style = UType.Small, maxLines = 1)
    }
}
