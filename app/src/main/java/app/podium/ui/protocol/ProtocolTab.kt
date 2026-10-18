package app.podium.ui.protocol

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.podium.R
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.components.Chip
import app.podium.ui.components.RunRow
import app.podium.ui.components.SectionTitle
import app.podium.ui.ctx
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

/** Every heat, day by day, like the sheets pinned up after each session. */
@Composable
fun ProtocolTab(arena: Arena, go: Go) {
    val c = ctx()
    var only by rememberSaveable { mutableLongStateOf(0L) }
    val runs = arena.runs.filter { only == 0L || it.eventId == only }
    val days = runs.groupBy { arena.date(it.start) }.toList()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.protocol_eyebrow).uppercase(), style = UType.Eyebrow)
                Text(stringResource(R.string.tab_protocol), style = UType.Display)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.protocol_all), only == 0L, { only = 0L })
                    arena.events.filter { e -> arena.runsOf(e.id).isNotEmpty() }.forEach { e ->
                        Chip(e.name, only == e.id, { only = if (only == e.id) 0L else e.id }, e.ringOf.color())
                    }
                }
            }
        }
        if (days.isEmpty()) item {
            Text(stringResource(R.string.protocol_empty), style = UType.Sub, modifier = Modifier.padding(top = 24.dp, start = 4.dp))
        }
        days.forEach { (day, list) ->
            item(key = "d" + day.toEpochDay()) {
                SectionTitle(
                    if (day == arena.today) stringResource(R.string.today) + " · " + T.day(c, day)
                    else T.weekday(c, day)
                )
            }
            items(list, key = { it.id }) { r -> RunRow(r, arena.event(r.eventId), arena.award(r), { go.run(r.id) }) }
        }
    }
}
