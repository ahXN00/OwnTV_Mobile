package tv.own.owntv.mobile.ui.shell

import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import tv.own.owntv.core.metadata.MetadataBudget
import tv.own.owntv.mobile.ui.nav.MobileDestination
import tv.own.owntv.mobile.ui.nav.MobileDestination.Companion.visible
import tv.own.owntv.mobile.ui.nav.MobileNavHost
import tv.own.owntv.mobile.ui.nav.PLAYER_ROUTE
import tv.own.owntv.mobile.ui.nav.SEARCH_ROUTE
import tv.own.owntv.mobile.ui.nav.SETUP_ROUTE
import tv.own.owntv.mobile.ui.setup.SetupFlow
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.mobile.ui.player.MiniPlayer
import tv.own.owntv.mobile.ui.screens.library.VodTuner
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.mobile.ui.screens.settings.settingsPageTitleRes

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
    val needsSetup by shellViewModel.needsSetup.collectAsStateWithLifecycle()

    // A brand new install has no playlist, so there is nothing for the tabs to show: the setup flow
    // IS the app until one exists. No cancel — there is nowhere to cancel to.
    if (needsSetup == true) {
        SetupFlow(onDone = { }, modifier = modifier)
        return
    }

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
    val fullscreen = currentRoute == PLAYER_ROUTE || currentRoute == SETUP_ROUTE
    val tuner: LiveTuner = koinInject()
    val vodTuner: VodTuner = koinInject()
    val channel by tuner.channel.collectAsStateWithLifecycle()
    val nowNext by tuner.nowNext.collectAsStateWithLifecycle()
    val film by vodTuner.playing.collectAsStateWithLifecycle()
    // Only ever ONE view of the picture at a time: the engine renders into a single surface, and a
    // second one attaching would take it away from the first. So no mini player on a screen that is
    // already showing the stream.
    val showingStream = fullscreen || currentRoute?.startsWith("${MobileDestination.LIVE.route}/") == true
    val showMini = (channel != null || film != null) && !showingStream

    val settingsTitle = settingsPageTitleRes(currentRoute)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Say once per launch that the day's share of the shared metadata service is gone, rather than
    // letting posters and plots quietly stop appearing. It can happen on any screen, so it belongs
    // here; `remember` (not rememberSaveable) is exactly the once-per-launch scope wanted.
    val metadataBudget: MetadataBudget = koinInject()
    val budgetRefusedAt by metadataBudget.refusedAt.collectAsStateWithLifecycle()
    var budgetNoticeShown by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(budgetRefusedAt) {
        if (budgetRefusedAt > 0L && !budgetNoticeShown) {
            budgetNoticeShown = true
            Toast.makeText(
                context,
                tv.own.owntv.mobile.R.string.settings_metadata_limit_reached,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!fullscreen) TopAppBar(
                title = {
                    Text(
                        // Search belongs to no tab, so it names itself rather than inheriting Home's.
                        // A settings page names itself too, and that name is what "back" leaves.
                        text = stringResource(
                            settingsTitle
                                ?: if (currentRoute == SEARCH_ROUTE) tv.own.owntv.mobile.R.string.search_title
                                else current?.labelRes ?: MobileDestination.HOME.labelRes,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    // A settings page is reached from a list, not from a tab, so the bar carries the
                    // way out. Every other screen has its tab still selected underneath it.
                    if (settingsTitle != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(tv.own.owntv.mobile.R.string.common_back),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(SEARCH_ROUTE) { launchSingleTop = true }
                        },
                    ) {
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
                        // A channel wins when there is one, because the two tuners cannot both be
                        // playing and the live one is what the other stops before it starts.
                        val live = channel
                        MiniPlayer(
                            player = tuner.player,
                            title = live?.name ?: film?.title.orEmpty(),
                            subtitle = if (live != null) nowNext?.now?.title else film?.subtitle,
                            artworkUrl = live?.displayLogoUrl ?: film?.posterUrl,
                            onExpand = { navController.navigate(PLAYER_ROUTE) },
                            onStop = { if (live != null) tuner.stop() else vodTuner.stop() },
                        )
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
