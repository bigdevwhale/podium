package app.podium.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.podium.R
import app.podium.container
import app.podium.data.Backup
import app.podium.data.Everything
import app.podium.data.GhostMode
import app.podium.domain.GamesCalendar
import app.podium.ui.Arena
import app.podium.ui.Go
import app.podium.ui.Language
import app.podium.ui.T
import app.podium.ui.components.Chip
import app.podium.ui.components.ConfirmDialog
import app.podium.ui.components.InfoRow
import app.podium.ui.components.SectionTitle
import app.podium.ui.components.TopBar
import app.podium.ui.components.UCard
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(arena: Arena, go: Go) {
    val c = ctx()
    val container = c.container
    val s = arena.settings
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var restoring by remember { mutableStateOf<android.net.Uri?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.write(c, uri, Everything(arena.events, arena.runs, arena.relays)) }.isSuccess }
            Toast.makeText(c, if (ok) R.string.backup_done else R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoring = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            SectionTitle(stringResource(R.string.set_games))
            UCard {
                Text(stringResource(R.string.set_games_len), style = UType.Strong)
                Text(stringResource(R.string.set_games_len_sub), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4, 8).forEach { w ->
                        Chip(pluralStringResource(R.plurals.weeks, w, w), s.gamesWeeks == w, {
                            // Keep today's Games starting where it is so the numbering doesn't jump about.
                            container.write { settings.update { it.copy(gamesWeeks = w, gamesFrom = GamesCalendar.monday(LocalDate.now()).toEpochDay() - 7L * w * (arena.games.number - 1).coerceAtLeast(0)) } }
                        })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Toggle(stringResource(R.string.set_ceremony), stringResource(R.string.set_ceremony_sub), s.ceremony) { v -> container.write { settings.update { it.copy(ceremony = v) } } }
            }

            SectionTitle(stringResource(R.string.set_heat))
            UCard {
                Text(stringResource(R.string.set_ghost), style = UType.Strong)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostMode.entries.forEach { g -> Chip(T.ghost(c, g), s.ghost == g, { container.write { settings.update { it.copy(ghost = g) } } }) }
                }
                Text(T.ghostSub(c, s.ghost), style = UType.Muted)
                Spacer(Modifier.height(14.dp))
                Toggle(stringResource(R.string.set_haptics), stringResource(R.string.set_haptics_sub), s.haptics) { v -> container.write { settings.update { it.copy(haptics = v) } } }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.set_tile), style = UType.Muted)
            }

            SectionTitle(stringResource(R.string.set_language))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cur = Language.current()
                Chip(stringResource(R.string.set_lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
            }

            SectionTitle(stringResource(R.string.set_data))
            UCard {
                InfoRow(Icons.Rounded.Upload, stringResource(R.string.backup_export_sub), stringResource(R.string.backup_export)) { export.launch("podium-${LocalDate.now()}.json") }
                InfoRow(Icons.Rounded.Download, stringResource(R.string.backup_import_sub), stringResource(R.string.backup_import)) { import.launch(arrayOf("application/json", "text/plain", "*/*")) }
            }

            SectionTitle(stringResource(R.string.set_about))
            Text(stringResource(R.string.set_privacy), style = UType.Sub)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.set_rings_note), style = UType.Muted)
            Spacer(Modifier.height(24.dp))
        }
    }
    restoring?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.backup_import_title),
            stringResource(R.string.backup_import_text),
            stringResource(R.string.backup_import_go),
            {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val all = Backup.read(c, uri)
                            container.db.dao().replaceAll(all)
                            val first = all.runs.minOfOrNull { it.start } ?: System.currentTimeMillis()
                            container.settings.update {
                                // A fresh install has no Games yet: they open on the Monday of the first heat.
                                it.copy(onboarded = true, gamesFrom = if (it.onboarded) it.gamesFrom else GamesCalendar.monday(GamesCalendar.date(first)).toEpochDay())
                            }
                            container.changed()
                        }.isSuccess
                    }
                    Toast.makeText(c, if (ok) R.string.backup_restored else R.string.backup_bad, Toast.LENGTH_SHORT).show()
                }
            },
            { restoring = null },
        )
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            Text(sub, style = UType.Muted)
        }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
    }
}
