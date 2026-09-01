package tv.own.owntv.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tv.own.owntv.mobile.dev.DevHarnessScreen
import tv.own.owntv.mobile.dev.ThemeGalleryScreen
import tv.own.owntv.mobile.ui.components.FilterChipRow
import tv.own.owntv.mobile.ui.theme.MobileTheme

/**
 * The single activity the whole app runs in. It currently hosts the Plan 4 Phase 1 theme gallery
 * and the Plan 3 harness — core's database, sync path and player, driven from the ugliest possible
 * UI. The real screens arrive with the shell and this body goes with them.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
                    DevRoot(Modifier.padding(insets))
                }
            }
        }
    }
}

@Composable
private fun DevRoot(modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier) {
        // Two throwaway tabs, labelled with core's own strings so this scaffolding never adds a
        // literal of its own: the component gallery, and the Plan 3 playlist/playback harness.
        FilterChipRow(
            labels = listOf(
                stringResource(R.string.settings_theme),
                stringResource(R.string.common_nav_live_tv),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        when (tab) {
            0 -> ThemeGalleryScreen()
            else -> DevHarnessScreen()
        }
    }
}
