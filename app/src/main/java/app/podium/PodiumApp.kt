package app.podium

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.podium.data.DemoSeed
import app.podium.data.PodiumDatabase
import app.podium.data.SettingsStore
import app.podium.notify.Alarms
import app.podium.notify.Live
import app.podium.notify.Shortcuts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = PodiumDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where a tap from outside wants to go ("start", "go:3", "finish", "games:2") and when it was made. */
    val open = MutableStateFlow<Pair<String, Long>?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch {
            block()
            changed()
        }
    }

    /** Something changed: the shortcuts and the ceremony may be different now. */
    suspend fun changed() {
        val dao = db.dao()
        Shortcuts.update(context, dao.events(), dao.runs())
        Alarms.reschedule(context)
    }
}

class PodiumApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@PodiumApp, container)
            val h = container.settings.now().heat
            if (h.active) Live.show(this@PodiumApp, h)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as PodiumApp).container

/** Resources in the in-app language, for receivers that AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
