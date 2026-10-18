package app.podium.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Stadium
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.podium.BuildConfig
import app.podium.R
import app.podium.container
import app.podium.data.Event
import app.podium.data.Finished
import app.podium.data.Heats
import app.podium.data.Relay
import app.podium.data.Run
import app.podium.data.Settings
import app.podium.domain.Award
import app.podium.domain.Games
import app.podium.domain.GamesCalendar
import app.podium.domain.Placed
import app.podium.notify.Alarms
import app.podium.ui.coach.CoachTab
import app.podium.ui.components.rememberNow
import app.podium.ui.event.EditEventScreen
import app.podium.ui.event.EventScreen
import app.podium.ui.heat.HeatScreen
import app.podium.ui.heat.ManualScreen
import app.podium.ui.heat.RunScreen
import app.podium.ui.heat.StartPicker
import app.podium.ui.heat.StartScreen
import app.podium.ui.medals.GamesScreen
import app.podium.ui.medals.MedalsTab
import app.podium.ui.onboarding.OnboardingScreen
import app.podium.ui.protocol.ProtocolTab
import app.podium.ui.relay.EditRelayScreen
import app.podium.ui.settings.SettingsScreen
import app.podium.ui.stadium.StadiumTab
import app.podium.ui.theme.Sans
import app.podium.ui.theme.U
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { STADIUM, PROTOCOL, MEDALS, COACH }

/** Everything stored, plus what is derived from it, recomputed every minute or on change. */
@Immutable
class Arena(
    val events: List<Event>,
    val runs: List<Run>,
    val relays: List<Relay>,
    val settings: Settings,
    val now: Long,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)
    private val byEvent = runs.groupBy { it.eventId }

    val placed: List<Placed> by lazy { GamesCalendar.placed(events, runs) }
    private val awards: Map<Long, Award> by lazy { placed.associate { it.run.id to it.award } }

    fun event(id: Long) = events.firstOrNull { it.id == id }
    fun run(id: Long) = runs.firstOrNull { it.id == id }
    fun relay(id: Long) = relays.firstOrNull { it.id == id }
    fun award(r: Run): Award? = awards[r.id]

    /** A discipline's heats, newest first. */
    fun runsOf(id: Long): List<Run> = byEvent[id].orEmpty()

    val lineup: List<Event> get() = events.filter { !it.retired }

    val games: Games = GamesCalendar.of(today, settings.gamesFrom, settings.gamesWeeks)
    fun games(n: Int): Games = GamesCalendar.number(n, settings.gamesFrom, settings.gamesWeeks)
    fun date(ms: Long): LocalDate = GamesCalendar.date(ms, zone)

    val flame: Int by lazy { GamesCalendar.flame(runs, today, zone) }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController, val pick: () -> Unit) {
    fun event(id: Long) = nav.navigate("event/$id") { launchSingleTop = true }
    fun edit(id: Long) = nav.navigate("edit/$id") { launchSingleTop = true }
    fun start(event: Long) = nav.navigate("start/$event?relay=0") { launchSingleTop = true }
    fun startRelay(relay: Long) = nav.navigate("start/0?relay=$relay") { launchSingleTop = true }
    fun heat() = nav.navigate("heat") {
        popUpTo("main") { inclusive = false }
        launchSingleTop = true
    }
    fun finished(run: Long) = nav.navigate("run/$run?fresh=true") { popUpTo("main") { inclusive = false } }
    fun run(id: Long) = nav.navigate("run/$id?fresh=false") { launchSingleTop = true }
    fun manual(event: Long) = nav.navigate("manual/$event") { launchSingleTop = true }
    fun games(n: Int) = nav.navigate("games/$n") { launchSingleTop = true }
    fun relay(id: Long) = nav.navigate("relay/$id") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun home() { nav.popBackStack("main", false) }

    /** Off the track when the heat is gone, unless something has already taken us elsewhere (the result). */
    fun leaveHeat() { if (nav.currentDestination?.route == "heat") home() }
    fun back() { nav.popBackStack() }
}

@Composable
fun PodiumRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.STADIUM) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()

    val events by dao.observeEvents().collectAsStateWithLifecycle(initialValue = null)
    val runs by dao.observeRuns().collectAsStateWithLifecycle(initialValue = null)
    val relays by dao.observeRelays().collectAsStateWithLifecycle(initialValue = null)
    val tick = rememberNow(60_000)
    val arena = remember(events, runs, relays, settings, tick) {
        Arena(events.orEmpty(), runs.orEmpty(), relays.orEmpty(), settings, System.currentTimeMillis())
    }
    var picker by remember { mutableStateOf(false) }
    val go = remember(nav) { Go(nav, pick = { picker = true }) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Opened while a heat is under way: straight back to its clock.
    LaunchedEffect(Unit) { if (settings.onboarded && settings.heat.active && container.open.value == null) go.heat() }
    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome screens.
    // Keyed on the route too: the demo seed can finish before the nav graph has its first destination.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    // A tap from the shade, the tile or a shortcut.
    val open by container.open.collectAsStateWithLifecycle()
    LaunchedEffect(open, events != null, settings.onboarded) {
        val (where, at) = open ?: return@LaunchedEffect
        if (events == null || !settings.onboarded) return@LaunchedEffect
        container.open.value = null
        when {
            where == "finish" -> scope.launch {
                when (val f = Heats.finish(context, at)) {
                    is Finished.Done -> go.finished(f.runId)
                    is Finished.NextLeg, null -> if (container.settings.now().heat.active) go.heat() else go.home()
                }
            }
            where == "heat" -> if (settings.heat.active) go.heat() else go.home()
            settings.heat.active -> go.heat()
            where == "start" -> { go.home(); picker = true }
            where.startsWith("go:") -> where.removePrefix("go:").toLongOrNull()?.let { go.home(); go.start(it) }
            where.startsWith("games:") -> where.removePrefix("games:").toIntOrNull()?.let { go.home(); tab = Tab.MEDALS; go.games(it) }
        }
    }

    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(settings.heat.active) {
        // Asked when there is a heat to show in the shade.
        if (settings.heat.active && android.os.Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable("welcome") {
                OnboardingScreen { picked ->
                    container.write {
                        picked.forEachIndexed { i, e -> db.dao().insert(e.copy(order = i)) }
                        this.settings.update { it.copy(onboarded = true, gamesFrom = GamesCalendar.monday(LocalDate.now()).toEpochDay()) }
                    }
                    nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                }
            }
            composable("main") { MainTabs(tab, { tab = it }, go, arena) }
            composable("event/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                EventScreen(e.arguments?.getLong("id") ?: 0L, arena, go)
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                EditEventScreen(e.arguments?.getLong("id") ?: 0L, arena, go)
            }
            composable(
                "start/{event}?relay={relay}",
                arguments = listOf(navArgument("event") { type = NavType.LongType }, navArgument("relay") { type = NavType.LongType; defaultValue = 0L }),
            ) { e ->
                StartScreen(e.arguments?.getLong("event") ?: 0L, e.arguments?.getLong("relay") ?: 0L, arena, go)
            }
            composable("heat") { HeatScreen(arena, go) }
            composable(
                "run/{id}?fresh={fresh}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("fresh") { type = NavType.BoolType; defaultValue = false }),
            ) { e ->
                RunScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getBoolean("fresh") ?: false, arena, go)
            }
            composable("manual/{event}", arguments = listOf(navArgument("event") { type = NavType.LongType })) { e ->
                ManualScreen(e.arguments?.getLong("event") ?: 0L, arena, go)
            }
            composable("games/{n}", arguments = listOf(navArgument("n") { type = NavType.IntType })) { e ->
                GamesScreen(e.arguments?.getInt("n") ?: 1, arena, go)
            }
            composable("relay/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                EditRelayScreen(e.arguments?.getLong("id") ?: 0L, arena, go)
            }
            composable("settings") { SettingsScreen(arena, go) }
        }

        if (picker) StartPicker(arena, go, onDismiss = { picker = false })
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, arena: Arena) {
    val running = arena.settings.heat.active
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.STADIUM -> StadiumTab(arena, go)
                Tab.PROTOCOL -> ProtocolTab(arena, go)
                Tab.MEDALS -> MedalsTab(arena, go)
                Tab.COACH -> CoachTab(arena, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.Stadium, stringResource(R.string.tab_stadium), tab == Tab.STADIUM) { onTab(Tab.STADIUM) }
            NavItem(Icons.AutoMirrored.Rounded.ListAlt, stringResource(R.string.tab_protocol), tab == Tab.PROTOCOL) { onTab(Tab.PROTOCOL) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.offset(y = (-10).dp).size(62.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                        .background(if (running) U.c.ahead else U.c.accent)
                        .border(3.dp, U.c.surface, CircleShape)
                        .clickable { if (running) go.heat() else go.pick() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Timer, stringResource(R.string.start_cd), Modifier.size(28.dp), tint = Color.White) }
            }
            NavItem(Icons.Rounded.MilitaryTech, stringResource(R.string.tab_medals), tab == Tab.MEDALS) { onTab(Tab.MEDALS) }
            NavItem(Icons.Rounded.Insights, stringResource(R.string.tab_coach), tab == Tab.COACH) { onTab(Tab.COACH) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Sans, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
