package tv.own.owntv.mobile.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.SharedFlow
import tv.own.owntv.mobile.BuildConfig
import tv.own.owntv.mobile.dev.DevHarnessScreen
import tv.own.owntv.mobile.dev.ThemeGalleryScreen
import tv.own.owntv.mobile.ui.screens.DevRoute
import tv.own.owntv.mobile.ui.screens.MoreScreen
import tv.own.owntv.mobile.ui.screens.PlaceholderScreen
import tv.own.owntv.mobile.ui.player.PlayerScreen
import tv.own.owntv.mobile.ui.screens.live.ChannelDetailScreen
import tv.own.owntv.mobile.ui.screens.live.LiveScreen

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
                when (destination) {
                    MobileDestination.MORE -> MoreScreen(
                        scrollToTop = scrollToTop,
                        onNavigate = onNavigate,
                        onDevRoute = { navController.navigate(it.route) },
                    )
                    MobileDestination.LIVE -> LiveScreen(
                        scrollToTop = scrollToTop,
                        onOpenChannel = { channelId, openCatchup ->
                            navController.navigate(channelRoute(channelId, openCatchup))
                        },
                    )
                    else -> PlaceholderScreen(destination = destination, scrollToTop = scrollToTop)
                }
            }
        }
        composable(
            route = CHANNEL_ROUTE,
            arguments = listOf(
                navArgument(ARG_CHANNEL_ID) { type = NavType.LongType },
                navArgument(ARG_CATCHUP) { type = NavType.BoolType },
            ),
        ) { entry ->
            ChannelDetailScreen(
                channelId = entry.arguments?.getLong(ARG_CHANNEL_ID) ?: return@composable,
                openCatchup = entry.arguments?.getBoolean(ARG_CATCHUP) == true,
                onFullscreen = { navController.navigate(PLAYER_ROUTE) },
            )
        }
        composable(PLAYER_ROUTE) {
            PlayerScreen(onExit = { navController.popBackStack() })
        }
        if (BuildConfig.DEV_TOOLS) {
            composable(DevRoute.GALLERY.route) { ThemeGalleryScreen() }
            composable(DevRoute.HARNESS.route) { DevHarnessScreen() }
        }
    }
}

/**
 * A channel, watched. It sits under Live's own route so the shell keeps the Live tab selected and
 * its title while the channel is open — a detail screen is not a fifth tab.
 */
private fun channelRoute(channelId: Long, openCatchup: Boolean) =
    "${MobileDestination.LIVE.route}/$channelId/$openCatchup"

private const val ARG_CHANNEL_ID = "channelId"
private const val ARG_CATCHUP = "catchup"
private const val CHANNEL_ROUTE = "live/{$ARG_CHANNEL_ID}/{$ARG_CATCHUP}"

/**
 * The full screen player. A route of its own, outside the tabs, because it is the one destination
 * that hides the shell — the bars would be covering the picture, and the back stack is what returns
 * the user to whatever they were looking at.
 */
const val PLAYER_ROUTE = "player"
