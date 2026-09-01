package tv.own.owntv.mobile.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.SharedFlow
import tv.own.owntv.mobile.BuildConfig
import tv.own.owntv.mobile.dev.DevHarnessScreen
import tv.own.owntv.mobile.dev.ThemeGalleryScreen
import tv.own.owntv.mobile.ui.screens.DevRoute
import tv.own.owntv.mobile.ui.screens.MoreScreen
import tv.own.owntv.mobile.ui.screens.PlaceholderScreen

/**
 * Every route in the app.
 *
 * All but More are placeholders until their own phase builds them — but they are real destinations
 * with real scroll state, so navigation, back and scroll restoration can be tested now rather than
 * discovered to be broken in Phase 7.
 */
@Composable
fun MobileNavHost(
    navController: NavHostController,
    scrollToTop: SharedFlow<String>,
    onNavigate: (MobileDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MobileDestination.HOME.route,
        modifier = modifier,
    ) {
        MobileDestination.entries.forEach { destination ->
            composable(destination.route) {
                if (destination == MobileDestination.MORE) {
                    MoreScreen(
                        scrollToTop = scrollToTop,
                        onNavigate = onNavigate,
                        onDevRoute = { navController.navigate(it.route) },
                    )
                } else {
                    PlaceholderScreen(destination = destination, scrollToTop = scrollToTop)
                }
            }
        }
        if (BuildConfig.DEV_TOOLS) {
            composable(DevRoute.GALLERY.route) { ThemeGalleryScreen() }
            composable(DevRoute.HARNESS.route) { DevHarnessScreen() }
        }
    }
}
