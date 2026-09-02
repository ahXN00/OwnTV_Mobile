package tv.own.owntv.mobile.ui.shell

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import tv.own.owntv.mobile.ui.nav.MobileDestination
import tv.own.owntv.mobile.ui.nav.MobileDestination.Companion.visible
import tv.own.owntv.mobile.ui.nav.MobileNavHost
import tv.own.owntv.mobile.ui.nav.PLAYER_ROUTE
import tv.own.owntv.mobile.ui.player.MiniPlayer
import tv.own.owntv.mobile.ui.screens.live.LiveTuner

/**
 * The frame every screen sits in: a top app bar that collapses as you scroll, the navigation itself,
 * and the content.
 *
 * The navigation is a bottom bar on a phone and a rail on anything wider, chosen by width rather
 * than by device type — a phone in landscape and a foldable opened flat are both "wider", and a
 * tablet in split-screen is not. Which destinations appear is core's `NavVisibility` rule, the same
 * one the TV app's sidebar uses, so a channels-only playlist loses Library in both apps at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileShell(
    windowWidthDp: Int,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val shellViewModel: ShellViewModel = koinViewModel()
    val sections by shellViewModel.visibleSections.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 600dp is Material's compact/medium boundary: below it a rail would eat the content.
    val useRail = windowWidthDp >= 600
    val destinations =
        (if (useRail) MobileDestination.rail else MobileDestination.bottomBar).visible(sections)
    // A detail route ("live/42/false") keeps its tab selected and its tab's title: on a phone the
    // channel you opened is still Live TV, and the bottom bar must not go blank while you watch it.
    val current = destinations.firstOrNull { it.route == currentRoute }
        ?: destinations.firstOrNull { currentRoute?.startsWith("${it.route}/") == true }

    // The full screen player is the one destination that owns the whole display: no bars, no rail,
    // and no mini player, because the thing the mini player would be showing is already on screen.
    val fullscreen = currentRoute == PLAYER_ROUTE
    val tuner: LiveTuner = koinInject()
    val playing by tuner.channel.collectAsStateWithLifecycle()
    // Only ever ONE view of the picture at a time: the engine renders into a single surface, and a
    // second one attaching would take it away from the first. So no mini player on a screen that is
    // already showing the stream.
    val showingStream = fullscreen || currentRoute?.startsWith("${MobileDestination.LIVE.route}/") == true
    val showMini = playing != null && !showingStream

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!fullscreen) TopAppBar(
                title = {
                    Text(
                        text = stringResource(current?.labelRes ?: MobileDestination.HOME.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigateToTab(MobileDestination.MORE) }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(tv.own.owntv.mobile.R.string.common_nav_search),
                        )
                    }
                    // Cast is Phase 5's, when there is a session to hand over. The slot is here so
                    // the bar's layout is the final one and nothing shifts when it starts working.
                    IconButton(onClick = { }, enabled = false) {
                        Icon(
                            imageVector = Icons.Filled.Cast,
                            contentDescription = stringResource(tv.own.owntv.mobile.R.string.common_cast),
                        )
                    }
                    IconButton(onClick = { navController.navigateToTab(MobileDestination.MORE) }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(tv.own.owntv.mobile.R.string.profiles_title),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (!fullscreen) {
                // The mini player shares the bottom bar slot, above the tabs, so it is docked in both
                // layouts — a rail screen has no bottom bar of its own and would otherwise lose it.
                Column {
                    if (showMini) {
                        MiniPlayer(tuner = tuner, onExpand = { navController.navigate(PLAYER_ROUTE) })
                    }
                    if (!useRail) {
                        NavigationBar {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = destination == current,
                                    onClick = { navController.onNavClick(destination, current, shellViewModel) },
                                    icon = { NavIcon(destination) },
                                    label = { NavLabel(destination) },
                                    modifier = Modifier.longPressResetsScroll(destination, shellViewModel),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { insets ->
        Row(Modifier.padding(insets)) {
            if (useRail && !fullscreen) {
                NavigationRail {
                    destinations.forEach { destination ->
                        NavigationRailItem(
                            selected = destination == current,
                            onClick = { navController.onNavClick(destination, current, shellViewModel) },
                            icon = { NavIcon(destination) },
                            label = { NavLabel(destination) },
                            modifier = Modifier.longPressResetsScroll(destination, shellViewModel),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxSize()) {
                MobileNavHost(
                    navController = navController,
                    scrollToTop = shellViewModel.scrollToTop,
                    onNavigate = { navController.navigateToTab(it) },
                )
            }
        }
    }
}

@Composable
private fun NavIcon(destination: MobileDestination) {
    Icon(imageVector = destination.icon, contentDescription = null)
}

@Composable
private fun NavLabel(destination: MobileDestination) {
    Text(
        text = stringResource(destination.labelRes),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Tapping the tab you are already on sends its list back to the top, as does a long press. */
private fun NavHostController.onNavClick(
    destination: MobileDestination,
    current: MobileDestination?,
    shell: ShellViewModel,
) {
    if (destination == current) shell.requestScrollToTop(destination.route) else navigateToTab(destination)
}

/**
 * A long press on a nav item scrolls that tab back to the top.
 *
 * It watches the pointer on the *initial* pass and never consumes, because the item's own click
 * handling sits below this modifier and would otherwise swallow the gesture — a plain
 * `combinedClickable` wrapped around a `NavigationBarItem` does nothing at all.
 */
private fun Modifier.longPressResetsScroll(
    destination: MobileDestination,
    shell: ShellViewModel,
): Modifier = pointerInput(destination) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val heldDown = try {
            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation(PointerEventPass.Initial)
            }
            false
        } catch (_: PointerEventTimeoutCancellationException) {
            true
        }
        if (heldDown) shell.requestScrollToTop(destination.route)
    }
}

/**
 * Switch tabs the way a bottom bar is supposed to: one entry per tab on the back stack, each tab's
 * own scroll position and state kept, and back from any tab landing on the start destination rather
 * than walking every tab you visited.
 */
fun NavHostController.navigateToTab(destination: MobileDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
