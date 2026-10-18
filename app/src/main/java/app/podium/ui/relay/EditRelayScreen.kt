package app.podium.ui.relay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.podium.R
import app.podium.container
import app.podium.data.Relay
import app.podium.domain.Relays
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.PictoTile
import app.podium.ui.components.ConfirmDialog
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.SoftButton
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.components.UTextField
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

/** A relay's legs in order, and its past runnings. */
@Composable
fun EditRelayScreen(id: Long, arena: Arena, go: Go) {
    val c = ctx()
    val container = c.container
    val old = arena.relay(id)
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    val legs = remember { mutableStateListOf<Long>().apply { addAll(old?.legIds.orEmpty()) } }
    var deleting by remember { mutableStateOf(false) }

    fun save(then: () -> Unit) {
        val r = (old ?: Relay(name = "", legs = "")).copy(name = name.trim(), legs = legs.joinToString(","))
        container.write { if (old == null) db.dao().insert(r) else db.dao().update(r) }
        then()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.relay_new else R.string.relay_eyebrow), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.relay_why), style = UType.Sub)
            Spacer(Modifier.height(12.dp))
            UTextField(name, { name = it }, placeholder = stringResource(R.string.relay_name_hint))
            SectionTitle(stringResource(R.string.relay_legs))
            legs.forEachIndexed { i, legId ->
                val e = arena.event(legId) ?: return@forEachIndexed
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = UType.Time.copy(color = U.c.faint), modifier = Modifier.width(22.dp))
                    PictoTile(e.picto, e.ringOf.color(), size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(e.name, style = UType.Strong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (i > 0) IconButton(onClick = { legs.add(i - 1, legs.removeAt(i)) }) { Icon(Icons.Rounded.KeyboardArrowUp, null, tint = U.c.muted) }
                    IconButton(onClick = { legs.removeAt(i) }) { Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint) }
                }
            }
            if (legs.isEmpty()) Text(stringResource(R.string.relay_legs_empty), style = UType.Muted, modifier = Modifier.padding(start = 4.dp))
            SectionTitle(stringResource(R.string.relay_add_leg))
            arena.lineup.filter { it.id !in legs }.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().clip(U.RMd).clickable { legs.add(e.id) }.padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PictoTile(e.picto, e.ringOf.color(), size = 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(e.name, style = UType.Body, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Add, null, tint = U.c.accent)
                }
            }

            if (old != null) {
                val past = Relays.of(old, arena.runs)
                if (past.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.relay_history))
                    val best = Relays.best(old, arena.runs)
                    UCard {
                        past.take(20).forEach { r ->
                            Row(
                                Modifier.fillMaxWidth().clickable { go.run(r.legs.last().id) }.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(T.date(c, r.key), style = UType.Body, modifier = Modifier.weight(1f))
                                if (!r.complete) Text(stringResource(R.string.relay_unfinished_short) + "  ", style = UType.Small)
                                Text(T.precise(r.total), style = UType.Time.copy(color = if (r.key == best?.key) U.c.gold else U.c.ink))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.relay_delete), color = U.c.behind, style = UType.Button) }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ok = name.isNotBlank() && legs.size >= 2
            if (old != null) SoftButton(stringResource(R.string.start_cd), { save { go.startRelay(old.id) } }, icon = Icons.Rounded.PlayArrow)
            Box(Modifier.weight(1f)) { PrimaryButton(stringResource(R.string.save), { save(go::back) }, Modifier.fillMaxWidth(), enabled = ok) }
        }
    }
    if (deleting && old != null) ConfirmDialog(
        stringResource(R.string.relay_delete_title, old.name),
        stringResource(R.string.relay_delete_text),
        stringResource(R.string.delete),
        { container.write { db.dao().delete(old) }; go.back() },
        { deleting = false },
        danger = true,
    )
}
