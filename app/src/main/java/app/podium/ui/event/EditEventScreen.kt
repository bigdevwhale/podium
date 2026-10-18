package app.podium.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import app.podium.R
import app.podium.container
import app.podium.data.Event
import app.podium.data.Ring
import app.podium.data.Sport
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.art.PictoTile
import app.podium.ui.art.Pictograms
import app.podium.ui.components.Chip
import app.podium.ui.components.ConfirmDialog
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.Segmented
import app.podium.ui.components.TopBar
import app.podium.ui.components.UTextField
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditEventScreen(id: Long, arena: Arena, go: Go) {
    val c = ctx()
    val container = c.container
    val old = arena.event(id)
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    var picto by rememberSaveable { mutableStateOf(old?.picto ?: 0) }
    var ring by rememberSaveable { mutableStateOf(old?.ringOf ?: Ring.HOME) }
    var sport by rememberSaveable { mutableStateOf(old?.sportOf ?: Sport.SPRINT) }
    var target by rememberSaveable { mutableStateOf(old?.target?.takeIf { it > 0 }?.let { (it / 60_000).toString() } ?: "20") }
    var retired by rememberSaveable { mutableStateOf(old?.retired ?: false) }
    val sections = remember { mutableStateListOf<String>().apply { addAll(old?.sections.orEmpty()) } }
    var deleting by remember { mutableStateOf(false) }
    val heats = arena.runsOf(id).size

    fun save() {
        val clean = sections.map { it.trim() }.filter { it.isNotEmpty() }
        val e = (old ?: Event(name = "", order = arena.events.size)).copy(
            name = name.trim(), picto = picto, ring = ring.name, sport = sport.name,
            target = if (sport == Sport.PACE) (target.toLongOrNull() ?: 20L).coerceAtLeast(1) * 60_000 else 0,
            splits = if (clean.size >= 2) clean.joinToString("\n") else "", retired = retired,
        )
        container.write { if (old == null) db.dao().insert(e) else db.dao().update(e) }
        go.back()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.event_new else R.string.event_edit), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PictoTile(picto, ring.color(), size = 64.dp)
                Spacer(Modifier.width(14.dp))
                UTextField(name, { name = it }, Modifier.weight(1f), placeholder = stringResource(R.string.event_name_hint))
            }

            SectionTitle(stringResource(R.string.event_picto))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pictograms.all.indices.forEach { i ->
                    val on = i == picto
                    Box(
                        Modifier.size(52.dp).clip(U.RSm).background(if (on) ring.color() else U.c.surface)
                            .border(1.dp, if (on) ring.color() else U.c.line, U.RSm).clickable { picto = i },
                        contentAlignment = Alignment.Center,
                    ) { app.podium.ui.art.Pictogram(i, if (on) androidx.compose.ui.graphics.Color.White else U.c.ink, size = 36.dp) }
                }
            }

            SectionTitle(stringResource(R.string.event_ring))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Ring.entries.forEach { r -> Chip(T.ring(c, r), r == ring, { ring = r }, r.color()) }
            }
            Text(stringResource(R.string.event_ring_why), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, top = 4.dp))

            SectionTitle(stringResource(R.string.event_sport))
            Segmented(Sport.entries, sport, { T.sport(c, it) }, { sport = it })
            Spacer(Modifier.height(8.dp))
            Text(T.sportRule(c, sport), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            if (sport == Sport.PACE) {
                Spacer(Modifier.height(10.dp))
                UTextField(target, { v -> target = v.filter { it.isDigit() }.take(3) }, placeholder = "20", number = true, suffix = stringResource(R.string.unit_min))
            }

            SectionTitle(stringResource(R.string.event_sections))
            Text(stringResource(R.string.event_sections_why), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            sections.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = UType.Time.copy(color = U.c.faint), modifier = Modifier.width(24.dp))
                    UTextField(s, { sections[i] = it }, Modifier.weight(1f), placeholder = stringResource(R.string.section_hint))
                    IconButton(onClick = { sections.removeAt(i) }) { Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint) }
                }
                Spacer(Modifier.height(6.dp))
            }
            TextButton(onClick = { if (sections.isEmpty()) sections.addAll(listOf("", "")) else sections.add("") }) {
                Icon(Icons.Rounded.Add, null, tint = U.c.accent)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.section_add), color = U.c.accent, style = UType.Button)
            }
            if (old != null && heats > 0 && old.sections.size != sections.count { it.isNotBlank() }) {
                Text(stringResource(R.string.sections_changed), style = UType.Muted, modifier = Modifier.padding(start = 4.dp))
            }

            if (old != null) {
                SectionTitle(stringResource(R.string.event_more))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.event_retire), style = UType.Strong)
                        Text(stringResource(R.string.event_retire_sub), style = UType.Muted)
                    }
                    Switch(retired, { retired = it }, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.event_delete), color = U.c.behind, style = UType.Button) }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            PrimaryButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
        }
    }
    if (deleting && old != null) ConfirmDialog(
        stringResource(R.string.event_delete_title, old.name),
        c.resources.getQuantityString(R.plurals.event_delete_text, heats, heats),
        stringResource(R.string.delete),
        {
            container.write {
                db.dao().deleteRunsOf(old.id)
                db.dao().delete(old)
            }
            go.home()
        },
        { deleting = false },
        danger = true,
    )
}
