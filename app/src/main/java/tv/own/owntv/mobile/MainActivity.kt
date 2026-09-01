package tv.own.owntv.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalConfiguration
import tv.own.owntv.mobile.ui.shell.MobileShell
import tv.own.owntv.mobile.ui.theme.MobileTheme

/** The single activity the whole app runs in. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MobileTheme {
                // Width, not device type: a phone in landscape and a tablet in split-screen are the
                // same problem, and the configuration re-reads itself on every rotation and resize.
                MobileShell(windowWidthDp = LocalConfiguration.current.screenWidthDp)
            }
        }
    }
}
