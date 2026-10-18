package app.podium

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.podium.data.Settings
import app.podium.notify.Alarms
import app.podium.ui.PodiumRoot
import app.podium.ui.theme.PodiumTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // White paper and a light-blue sky: dark icons in the bars.
        val style = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (savedInstanceState == null) opened(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            PodiumTheme {
                // Wait for settings so the welcome screen doesn't flash for someone already here.
                settings?.let { PodiumRoot(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened(intent)
    }

    /** A tap from the shade, a tile or a shortcut: remember where it wants to go, and when. */
    private fun opened(intent: Intent?) {
        val open = intent?.getStringExtra(Alarms.EXTRA_OPEN) ?: return
        container.open.value = open to System.currentTimeMillis()
    }
}
