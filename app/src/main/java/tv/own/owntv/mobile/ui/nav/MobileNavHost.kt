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
import tv.own.owntv.mobile.ui.screens.guide.GuideScreen
import tv.own.owntv.mobile.ui.screens.home.HomeScreen
import tv.own.owntv.mobile.ui.screens.library.DetailScreen
import tv.own.owntv.mobile.ui.screens.library.LibraryScreen
import tv.own.owntv.mobile.ui.screens.library.LibraryTab
import tv.own.owntv.mobile.ui.screens.live.ChannelDetailScreen
import tv.own.owntv.mobile.ui.screens.live.LiveScreen
import tv.own.owntv.mobile.ui.setup.SetupFlow

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
                        onAddSource = { navController.navigate(SETUP_ROUTE) },
                    )
                    MobileDestination.HOME -> HomeScreen(
                        scrollToTop = scrollToTop,
                        onOpenChannel = { channelId ->
                            navController.navigate(channelRoute(destination, channelId, false))
                        },
                        onOpenMovie = { id ->
                            navController.navigate(detailRoute(destination.route, LibraryTab.MOVIES, id))
                        },
                        onOpenSeries = { id ->
                            navController.navigate(detailRoute(destination.route, LibraryTab.SERIES, id))
                        },
                        onPlayerOpened = { navController.navigate(PLAYER_ROUTE) },
                        onAddSource = { navController.navigate(SETUP_ROUTE) },
                    )
                    MobileDestination.GUIDE -> GuideScreen(
                        scrollToTop = scrollToTop,
                        onOpenChannel = { channelId ->
                            navController.navigate(channelRoute(MobileDestination.LIVE, channelId, false))
                        },
                    )
                    MobileDestination.LIVE -> LiveScreen(
                        scrollToTop = scrollToTop,
                        onOpenChannel = { channelId, openCatchup ->
                            navController.navigate(channelRoute(destination, channelId, openCatchup))
                        },
                    )
                    MobileDestination.LIBRARY, MobileDestination.MOVIES, MobileDestination.SERIES ->
                        LibraryScreen(
                            scrollToTop = scrollToTop,
                            route = destination.route,
                            fixedTab = destination.fixedTab(),
                            onOpenItem = { tab, id ->
                                navController.navigate(detailRoute(destination.route, tab, id))
                            },
                        )
                    else -> PlaceholderScreen(destination = destination, scrollToTop = scrollToTop)
                }
            }
        }
        // A film or a show sits under the tab it was opened from, so the bar keeps that tab selected
        // — the same reason the channel screen lives under Live. Three parents, because a tablet
        // opens the same item from Movies or Series and a phone opens it from Library.
        LIBRARY_PARENTS.forEach { parent ->
            composable(
                route = "${parent.route}/{$ARG_TAB}/{$ARG_ITEM_ID}",
                arguments = listOf(
                    navArgument(ARG_TAB) { type = NavType.StringType },
                    navArgument(ARG_ITEM_ID) { type = NavType.LongType },
                ),
            ) { entry ->
                val tab = entry.arguments?.getString(ARG_TAB) ?: return@composable
                DetailScreen(
                    tab = LibraryTab.valueOf(tab),
                    itemId = entry.arguments?.getLong(ARG_ITEM_ID) ?: return@composable,
                    onPlay = { navController.navigate(PLAYER_ROUTE) },
                )
            }
        }
        CHANNEL_PARENTS.forEach { parent ->
            composable(
                route = "${parent.route}/$CHANNEL_SEGMENT/{$ARG_CHANNEL_ID}/{$ARG_CATCHUP}",
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
        }
        composable(PLAYER_ROUTE) {
            PlayerScreen(onExit = { navController.popBackStack() })
        }
        composable(SETUP_ROUTE) {
            SetupFlow(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        if (BuildConfig.DEV_TOOLS) {
            composable(DevRoute.GALLERY.route) { ThemeGalleryScreen() }
            composable(DevRoute.HARNESS.route) { DevHarnessScreen() }
        }
    }
}

/**
 * A channel, watched. It sits under the tab it was opened from so the shell keeps that tab selected
 * and its title while the channel is open — a detail screen is not a fifth tab.
 *
 * The `channel` segment is what keeps this apart from a film's route under the same parent: both are
 * "parent then two arguments", and the matcher goes by shape, not by argument type.
 */
private fun channelRoute(parent: MobileDestination, channelId: Long, openCatchup: Boolean) =
    "${parent.route}/$CHANNEL_SEGMENT/$channelId/$openCatchup"

private const val ARG_CHANNEL_ID = "channelId"
private const val ARG_CATCHUP = "catchup"
private const val CHANNEL_SEGMENT = "channel"

/** The tabs a channel can be opened from. Guide sends its channels to Live's. */
private val CHANNEL_PARENTS = listOf(MobileDestination.LIVE, MobileDestination.HOME)

/** The tabs a film or a show can be opened from, and therefore returned to. */
private val LIBRARY_PARENTS = listOf(
    MobileDestination.LIBRARY,
    MobileDestination.MOVIES,
    MobileDestination.SERIES,
    MobileDestination.HOME,
)

/** Movies and Series are one screen locked to one half; Library is that screen with both tabs. */
private fun MobileDestination.fixedTab(): LibraryTab? = when (this) {
    MobileDestination.MOVIES -> LibraryTab.MOVIES
    MobileDestination.SERIES -> LibraryTab.SERIES
    else -> null
}

private fun detailRoute(parentRoute: String, tab: LibraryTab, itemId: Long) =
    "$parentRoute/${tab.name}/$itemId"

private const val ARG_TAB = "tab"
private const val ARG_ITEM_ID = "itemId"

/**
 * The full screen player. A route of its own, outside the tabs, because it is the one destination
 * that hides the shell — the bars would be covering the picture, and the back stack is what returns
 * the user to whatever they were looking at.
 */
const val PLAYER_ROUTE = "player"

/**
 * Adding a playlist, or restoring a backup. Like the player it hides the shell: the bars would offer
 * tabs that lead to empty screens, and on a first run there is nothing behind it to go back to.
 */
const val SETUP_ROUTE = "setup"
