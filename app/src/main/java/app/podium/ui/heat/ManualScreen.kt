package app.podium.ui.heat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.podium.R
import app.podium.container
import app.podium.data.Form
import app.podium.data.Run
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.T
import app.podium.ui.components.Chip
import app.podium.ui.components.DateTimeDialog
import app.podium.ui.components.InfoRow
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.components.UTextField
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType

/** A heat timed some other way (a watch, the oven clock), written into the protocol by hand. */
@Composable
fun ManualScreen(eventId: Long, arena: Arena, go: Go) {
    val c = ctx()
    val e = arena.event(eventId) ?: return
    var h by rememberSaveable { mutableStateOf("") }
    var m by rememberSaveable { mutableStateOf("") }
    var s by rememberSaveable { mutableStateOf("") }
    var form by rememberSaveable { mutableStateOf(Form.CLEAN) }
    var start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis() - 30 * 60_000) }
    var picking by remember { mutableStateOf(false) }
    val duration = ((h.toLongOrNull() ?: 0) * 3600 + (m.toLongOrNull() ?: 0) * 60 + (s.toLongOrNull() ?: 0)) * 1000

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.manual_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(e.name, style = UType.Title)
            Text(stringResource(R.string.manual_why), style = UType.Sub)
            SectionTitle(stringResource(R.string.manual_time))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UTextField(h, { v -> h = v.filter { it.isDigit() }.take(2) }, Modifier.weight(1f), placeholder = "0", number = true, suffix = stringResource(R.string.unit_h))
                UTextField(m, { v -> m = v.filter { it.isDigit() }.take(2) }, Modifier.weight(1f), placeholder = "0", number = true, suffix = stringResource(R.string.unit_min))
                UTextField(s, { v -> s = v.filter { it.isDigit() }.take(2) }, Modifier.weight(1f), placeholder = "0", number = true, suffix = stringResource(R.string.unit_s))
            }
            SectionTitle(stringResource(R.string.manual_when))
            UCard { InfoRow(Icons.Rounded.Event, stringResource(R.string.manual_started), T.date(c, start) + ", " + T.time(c, start)) { picking = true } }
            SectionTitle(stringResource(R.string.run_form))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Form.entries.forEach { f -> Chip(T.form(c, f), form == f, { form = f }, if (f == Form.RUSHED) U.c.behind else U.c.accent) }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            PrimaryButton(stringResource(R.string.save), {
                c.container.write { db.dao().insert(Run(eventId = e.id, start = start, duration = duration, form = form.name, manual = true)) }
                go.back()
            }, Modifier.fillMaxWidth(), enabled = duration > 0)
        }
    }
    if (picking) DateTimeDialog(start, { picking = false }) { start = it }
}
