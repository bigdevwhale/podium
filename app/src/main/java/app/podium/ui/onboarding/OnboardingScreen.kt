package app.podium.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.podium.R
import app.podium.data.Event
import app.podium.data.Starters
import app.podium.domain.Medal
import app.podium.ui.T
import app.podium.ui.art.MedalMark
import app.podium.ui.art.PictoTile
import app.podium.ui.art.Stadium
import app.podium.ui.components.PrimaryButton
import app.podium.ui.components.rememberNow
import app.podium.ui.ctx
import app.podium.ui.theme.U
import app.podium.ui.theme.UType
import app.podium.ui.theme.color

/** Three pages: the ghost, the medals, and the first disciplines. */
@Composable
fun OnboardingScreen(onDone: (List<Event>) -> Unit) {
    val c = ctx()
    var page by rememberSaveable { mutableIntStateOf(0) }
    val picked = remember { mutableStateListOf<Int>().apply { addAll(Starters.all.indices.filter { Starters.all[it].picked }) } }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            when (page) {
                0 -> {
                    val now = rememberNow(50)
                    val t = (now % 8000) / 8000f
                    Stadium(t * 0.94f, t, 4, Modifier.fillMaxWidth().height(180.dp))
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.welcome_1_title), style = UType.Display)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.welcome_1_text), style = UType.Body.copy(color = U.c.muted))
                }
                1 -> {
                    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                        listOf(Medal.SILVER to 90.dp, Medal.GOLD to 120.dp, Medal.BRONZE to 74.dp).forEach { (m, h) ->
                            MedalMark(m, m.color(), Modifier.size(56.dp, h))
                            Spacer(Modifier.width(10.dp))
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.welcome_2_title), style = UType.Display)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.welcome_2_text), style = UType.Body.copy(color = U.c.muted))
                }
                else -> {
                    Text(stringResource(R.string.welcome_3_title), style = UType.Display)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.welcome_3_text), style = UType.Body.copy(color = U.c.muted))
                    Spacer(Modifier.height(16.dp))
                    Starters.all.forEachIndexed { i, s ->
                        val on = i in picked
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(U.RMd)
                                .background(if (on) U.c.surface else U.c.bg)
                                .border(1.dp, if (on) U.c.accent else U.c.line, U.RMd)
                                .clickable { if (on) picked.remove(i) else picked.add(i) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PictoTile(s.picto, if (on) s.ring.color() else U.c.ghost, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(s.name), style = UType.Strong)
                                Text(
                                    T.sport(c, s.sport) + if (s.targetMin > 0) " · " + T.stopwatch(s.targetMin * 60_000L) else if (s.sections != 0) " · " + stringResource(s.sections).replace("|", ", ") else "",
                                    style = UType.Muted,
                                )
                            }
                            if (on) Icon(Icons.Rounded.Check, null, tint = U.c.accent)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                Box(Modifier.padding(4.dp).size(if (i == page) 22.dp else 8.dp, 8.dp).clip(U.RSm).background(if (i == page) U.c.accent else U.c.line))
            }
        }
        PrimaryButton(
            stringResource(if (page < 2) R.string.next else R.string.welcome_start),
            { if (page < 2) page++ else onDone(picked.sorted().map { Starters.event(c, Starters.all[it]) }) },
            Modifier.fillMaxWidth(),
        )
    }
}
