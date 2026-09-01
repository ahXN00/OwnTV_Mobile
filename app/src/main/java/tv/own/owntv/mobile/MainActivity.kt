package tv.own.owntv.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.own.owntv.mobile.dev.DevHarnessScreen

/**
 * The single activity the whole app runs in. It currently hosts nothing but the Plan 3 Phase 3
 * harness — core's database, sync path and player, driven from the ugliest possible UI. The real
 * screens arrive in Plan 4 and this body goes with them.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
                    Column(Modifier.padding(insets)) {
                        // Core owns every string in both apps; this one proves the inheritance works
                        // (Phase 4 verifies it properly, with a translated string and a locale switch).
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        DevHarnessScreen()
                    }
                }
            }
        }
    }
}
