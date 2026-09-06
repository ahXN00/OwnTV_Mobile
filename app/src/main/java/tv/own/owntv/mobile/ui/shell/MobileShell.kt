package tv.own.owntv.mobile.ui.shell

import tv.own.owntv.mobile.ui.components.MobileIcons
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
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
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.ui.nav.MobileDestination
import tv.own.owntv.mobile.ui.nav.MobileDestination.Companion.visible
import tv.own.owntv.mobile.ui.nav.MobileNavHost
import tv.own.owntv.mobile.playback.PipController
import tv.own.owntv.mobile.ui.nav.PLAYER_ROUTE
import tv.own.owntv.mobile.ui.nav.isChannelRoute
import tv.own.owntv.mobile.ui.nav.liveChannelRoute
import tv.own.owntv.mobile.ui.nav.SEARCH_ROUTE
import tv.own.owntv.mobile.ui.nav.SEARCH_ROUTE_PATTERN
import tv.own.owntv.mobile.ui.nav.SETUP_ROUTE
import tv.own.owntv.core.profile.profileGateRequired
import tv.own.owntv.core.profile.shellMayCompose
import tv.own.owntv.mobile.ui.profiles.ProfileGate
import tv.own.owntv.mobile.ui.profiles.ProfileGateSession
import tv.own.owntv.mobile.ui.profiles.ProfilesViewModel
import tv.own.owntv.mobile.ui.setup.SetupFlow
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.mobile.ui.player.FloatingMiniPlayer
import tv.own.owntv.mobile.ui.player.FloatingWindowMenu
import tv.own.owntv.mobile.ui.player.MiniPlayer
import tv.own.owntv.mobile.ui.player.SleepTimerSheet
import tv.own.owntv.mobile.ui.screens.library.VodTuner
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.mobile.ui.screens.settings.settingsPageTitleRes
import tv.own.owntv.mobile.ui.theme.GlassNest
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.MobileNavShape
import tv.own.owntv.mobile.ui.theme.MobilePageShape
import tv.own.owntv.mobile.ui.theme.MobileTopBarShape
import tv.own.owntv.mobile.ui.theme.glassSurface

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

    // Who is watching. The chooser is shown INSTEAD of the app rather than over it, so there is no
    // back gesture out of it and nothing of the locked profile is on screen behind it. Both the
    // profile list and the active id have to have arrived before anything is drawn: an active id
    // that lands first, while Room is still deciding whether that profile is PIN-locked, would be
    // enough to walk straight into it.
    val profilesViewModel: ProfilesViewModel = koinViewModel()
    val gateSession: ProfileGateSession = koinViewModel()
    val profiles by profilesViewModel.profiles.collectAsStateWithLifecycle()
    val activeProfileId by profilesViewModel.activeProfileId.collectAsStateWithLifecycle()
    LaunchedEffect(activeProfileId) {
        gateSession.invalidateIfNotProfile(activeProfileId.takeIf { it >= 0L })
    }
    // Nothing at all until Room answers — a frame of blank is the price of never showing the wrong
    // person's library.
    val loadedProfiles = profiles ?: return
    if (!shellMayCompose(
            profiles = loadedProfiles,
            activeProfileId = activeProfileId,
            authenticatedProfileId = gateSession.unlockedProfileId,
            gateRequired = profileGateRequired(loadedProfiles),
        )
    ) {
        // An empty list is a database still being restored, not a chooser with nothing in it.
        if (loadedProfiles.isNotEmpty()) {
            ProfileGate(
                profiles = loadedProfiles,
                onEntered = { gateSession.unlock(it.id) },
                modifier = modifier,
                vm = profilesViewModel,
            )
        }
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
    // A channel screen sits under whichever tab it was opened from, so the tab it is under is not
    // what says the picture is on screen — the route being a channel's is.
    val showingStream = fullscreen || isChannelRoute(currentRoute)
    val showMini = (channel != null || film != null) && !showingStream
    // Floating window, bar above the tabs, or neither — the user's choice, and the only thing that
    // changes is where the same stream is drawn.
    val settings: SettingsRepository = koinInject()
    val chosenMiniStyle by settings.miniPlayerStyle
        .collectAsStateWithLifecycle(SettingsRepository.MiniPlayerStyle.FLOATING)
    // Sound only has no picture, and a floating window with nothing in it is a smudge over the list the
    // user went back to. It docks instead, as a bar — which is also the shape that has room for a title
    // and the transport buttons, the only things left to show.
    val audioOnly by tuner.player.audioOnly.collectAsStateWithLifecycle()
    val audioOnlyMedia by tuner.player.audioOnlyMedia.collectAsStateWithLifecycle()
    // OFF is not offered in Settings any more, but an older install may still have it stored.
    val miniStyle = if (audioOnly || audioOnlyMedia || chosenMiniStyle == SettingsRepository.MiniPlayerStyle.OFF) {
        SettingsRepository.MiniPlayerStyle.DOCKED
    } else {
        chosenMiniStyle
    }

    // The playback notification was tapped, from a shade that may well outlive the activity that was
    // showing the player. Put it back.
    val pip: PipController = koinInject()
    val openPlayerRequested by pip.openPlayerRequested.collectAsStateWithLifecycle()
    LaunchedEffect(openPlayerRequested) {
        if (openPlayerRequested) {
            pip.openPlayerRequested.value = false
            if (channel != null || film != null) navController.navigate(PLAYER_ROUTE)
        }
    }

    // "Start on" — Home, the last channel watched, the Favorites folder, or one chosen channel. Once
    // per launch, and only once the database has said there is something to open: on a first run the
    // setup flow is the app, and a channel resolved against an empty database is not a missing
    // channel. A channel opens its own screen rather than the full screen player, because a phone is
    // picked up in places where sound arriving unannounced is not welcome.
    val context = LocalContext.current
    val startupLive: StartupLiveSelection = koinInject()
    val startupUnavailable = stringResource(tv.own.owntv.mobile.R.string.settings_startup_channel_unavailable)
    var startupHandled by remember { mutableStateOf(false) }
    LaunchedEffect(needsSetup) {
        if (needsSetup != false || startupHandled) return@LaunchedEffect
        startupHandled = true
        when (val target = shellViewModel.resolveStartup()) {
            StartupTarget.Home -> Unit
            StartupTarget.Favorites -> {
                startupLive.requestFavorites()
                navController.navigateToTab(MobileDestination.LIVE)
            }
            is StartupTarget.Channel -> navController.navigate(liveChannelRoute(target.id))
            StartupTarget.ChannelUnavailable ->
                Toast.makeText(context, startupUnavailable, Toast.LENGTH_LONG).show()
        }
    }

    val favorite by tuner.isFavorite.collectAsStateWithLifecycle()
    var windowMenu by remember { mutableStateOf(false) }
    var sleepSheet by remember { mutableStateOf(false) }

    val settingsTitle = settingsPageTitleRes(currentRoute)

    // Downloads is a tab on a rail but a row in More on a phone, where no tab is selected to name it
    // — without this the bar would call it "Home", which is where it is not.
    val offBarTitle = MobileDestination.entries.firstOrNull { it.route == currentRoute }?.labelRes

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // One behaviour serves every screen, so the offset the last one scrolled it to travels with the
    // user: leave Live TV half way down its list and Home, Guide and More all open with no bar at
    // all. Each arrival starts with the bar down.
    LaunchedEffect(currentRoute) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    // Say once per launch that the day's share of the shared metadata service is gone, rather than
    // letting posters and plots quietly stop appearing. It can happen on any screen, so it belongs
    // here; `remember` (not rememberSaveable) is exactly the once-per-launch scope wanted.
    val metadataBudget: MetadataBudget = koinInject()
    val budgetRefusedAt by metadataBudget.refusedAt.collectAsStateWithLifecycle()
    var budgetNoticeShown by remember { mutableStateOf(false) }
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
        // The wallpaper is drawn by the backdrop root underneath; a Scaffold that painted its own
        // background would cover it and leave the glass with nothing to be transparent to.
        containerColor = Color.Transparent,
        // The player owns every pixel, camera strip included: keeping the bars' and the cutout's room
        // free there would leave a band of wallpaper down the side of the picture.
        contentWindowInsets = if (fullscreen) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // The bar is a pane standing off the screen, not a lid on top of it: the status bar's
            // room is held by the wrapper so the wallpaper runs above the bar and stays clear when
            // the bar scrolls away, and the bar itself is inset and rounded.
            if (!fullscreen) Box(
                Modifier
                    .statusBarsPadding()
                    // The Scaffold hands the sides' room to the content slot only, so sideways —
                    // where the camera strip and the gesture bar are down the edges — the bar started
                    // further left than the rail and the page beneath it. It takes **the Scaffold's
                    // own** horizontal insets, not the wider safe-drawing ones, so the three left
                    // edges land on the same pixel rather than merely near each other.
                    .windowInsetsPadding(
                        ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
                    )
                    .padding(
                        start = MobileDimens.ShellInset,
                        end = MobileDimens.ShellInset,
                        bottom = MobileDimens.ShellGap,
                    ),
            ) {
                TopAppBar(
                    // Transparent container plus the glass modifier, rather than a colour: the bar has
                    // to let the wallpaper through it, and a container colour cannot.
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .glassSurface(GlassSurface.TOPBAR, MobileTopBarShape)
                        .clip(MobileTopBarShape),
                    title = {
                        Text(
                            // Search belongs to no tab, so it names itself rather than inheriting Home's.
                            // A settings page names itself too, and that name is what "back" leaves.
                            text = stringResource(
                                settingsTitle
                                    ?: if (currentRoute == SEARCH_ROUTE_PATTERN) tv.own.owntv.mobile.R.string.search_title
                                    else current?.labelRes ?: offBarTitle ?: MobileDestination.HOME.labelRes,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        // Anything that was opened on top of a tab — a film, a settings page, search —
                        // carries the way out in the bar. A tab itself does not: its own button in the
                        // bottom bar or the rail is already where it is, and there is nothing above it
                        // to leave. That covers the settings root on a rail, which is a tab there.
                        if (destinations.none { it.route == currentRoute }) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = MobileIcons.ArrowBack,
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
                                imageVector = MobileIcons.Search,
                                contentDescription = stringResource(tv.own.owntv.mobile.R.string.common_nav_search),
                            )
                        }
                        // Cast is Phase 5's, when there is a session to hand over. The slot is here so
                        // the bar's layout is the final one and nothing shifts when it starts working.
                        IconButton(onClick = { }, enabled = false) {
                            Icon(
                                imageVector = MobileIcons.Cast,
                                contentDescription = stringResource(tv.own.owntv.mobile.R.string.common_cast),
                            )
                        }
                        IconButton(onClick = { navController.navigateToTab(MobileDestination.MORE) }) {
                            Icon(
                                imageVector = MobileIcons.Person,
                                contentDescription = stringResource(tv.own.owntv.mobile.R.string.profiles_title),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            if (!fullscreen) {
                // The mini player shares the bottom bar slot, above the tabs, so it is docked in both
                // layouts — a rail screen has no bottom bar of its own and would otherwise lose it.
                // Both are islands: inset from the sides, clear of the gesture bar, with the page
                // ending above them rather than running underneath.
                Column(
                    // The gap above the island is the page's own bottom inset, so it is not counted
                    // twice; this only holds the island clear of the gesture bar.
                    Modifier.navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(MobileDimens.ShellGap),
                ) {
                    if (showMini && miniStyle == SettingsRepository.MiniPlayerStyle.DOCKED) {
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
                            modifier = Modifier.padding(horizontal = MobileDimens.ShellInset),
                        )
                    }
                    if (!useRail) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            modifier = Modifier
                                .padding(horizontal = MobileDimens.ShellInset)
                                .height(MobileDimens.NavIslandHeight)
                                .glassSurface(GlassSurface.SIDEBAR, MobileNavShape)
                                .clip(MobileNavShape),
                        ) {
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
        // The player owns the whole display, so it gets none of the shell's geometry: an inset,
        // rounded pane around a video would be a frame nobody asked for.
        val pageShape = if (fullscreen) RectangleShape else MobilePageShape
        Row(Modifier.padding(insets)) {
            if (useRail && !fullscreen) {
                NavigationRail(
                    containerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .padding(start = MobileDimens.ShellInset, bottom = MobileDimens.ShellInset)
                        .glassSurface(GlassSurface.SIDEBAR, MobileNavShape)
                        .clip(MobileNavShape),
                ) {
                    // Eight destinations do not fit down the short side of a phone held sideways, and
                    // an unscrollable rail simply loses the last of them — which is where Settings is.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        // Against a rail the page keeps the smaller gap; against the screen edge it
                        // keeps the full inset, so the wallpaper frames the whole shell evenly.
                        start = if (fullscreen) 0.dp else if (useRail) MobileDimens.ShellGap else MobileDimens.ShellInset,
                        end = if (fullscreen) 0.dp else MobileDimens.ShellInset,
                        bottom = if (fullscreen) 0.dp else MobileDimens.ShellInset,
                    )
                    .glassSurface(GlassSurface.PANELS, pageShape)
                    .clip(pageShape),
            ) {
                // Every screen in the app is standing on this page panel, so a panel of its own
                // draws as the layer behind one instead of frosting what is already frosted.
                GlassNest(GlassSurface.PANELS) {
                    MobileNavHost(
                        navController = navController,
                        scrollToTop = shellViewModel.scrollToTop,
                        onNavigate = { navController.navigateToTab(it) },
                    )
                }
                // Low over the page, under the mini player: a sync running while the user browses is
                // news, but it is never what they came to the screen for.
                if (!fullscreen) {
                    SyncStatusPill(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                    )
                }
                // Over the content rather than beside it, because that is what a floating window is.
                if (showMini && miniStyle == SettingsRepository.MiniPlayerStyle.FLOATING) {
                    val live = channel
                    FloatingMiniPlayer(
                        player = tuner.player,
                        title = live?.name ?: film?.title.orEmpty(),
                        isLive = live != null,
                        artworkUrl = live?.displayLogoUrl ?: film?.posterUrl,
                        onExpand = { navController.navigate(PLAYER_ROUTE) },
                        onStop = { if (live != null) tuner.stop() else vodTuner.stop() },
                        onMenu = { windowMenu = true },
                    )
                }
            }
        }
    }

    if (windowMenu) {
        FloatingWindowMenu(
            isFavorite = favorite,
            onToggleFavorite = tuner::toggleFavorite,
            onAudioOnly = { tuner.setAudioOnly(true) },
            onSleepTimer = { sleepSheet = true },
            onExpand = { navController.navigate(PLAYER_ROUTE) },
            onStop = { if (channel != null) tuner.stop() else vodTuner.stop() },
            onDismiss = { windowMenu = false },
        )
    }
    if (sleepSheet) {
        SleepTimerSheet(
            programmeEndMs = nowNext?.now?.stopMs,
            onDismiss = { sleepSheet = false },
        )
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
