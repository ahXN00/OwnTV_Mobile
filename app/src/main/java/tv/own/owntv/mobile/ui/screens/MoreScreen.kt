package tv.own.owntv.mobile.ui.screens

import tv.own.owntv.mobile.ui.components.MobileIcons
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.sync.work.CatalogSyncScheduler
import tv.own.owntv.mobile.BuildConfig
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileGroup
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.nav.MobileDestination
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The hub behind the More tab: everything that does not deserve a tab of its own.
 *
 * Downloads and Settings are also rail destinations on a tablet, so they are reachable two ways
 * there and one way on a phone — the same screen either way. Nothing here is built yet; the rows
 * that have a destination navigate, and the rest land in their own phase.
 */
@Composable
fun MoreScreen(
    scrollToTop: SharedFlow<String>,
    onNavigate: (MobileDestination) -> Unit,
    onDevRoute: (DevRoute) -> Unit,
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    listState.ObeyScrollToTop(route = MobileDestination.MORE.route, scrollToTop = scrollToTop)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MobileDimens.PagePaddingH,
            end = MobileDimens.PagePaddingH,
            top = MobileDimens.ScreenPaddingV,
            bottom = MobileDimens.GapLarge,
        ),
    ) {
        // Three blocks, not one list with rules across it: where you go, whose account it is, and
        // what you do to the library. The rounded pane is the grouping, the way a settings page
        // groups its rows.
        item {
            MobileGroup {
                MoreRow(R.string.common_nav_downloads, MobileIcons.Download) {
                    onNavigate(MobileDestination.DOWNLOADS)
                }
                MoreRow(R.string.common_nav_settings, MobileIcons.Settings) {
                    onNavigate(MobileDestination.SETTINGS)
                }
            }
            MobileGroup {
                MoreRow(R.string.profiles_title, MobileIcons.People)
                MoreRow(R.string.content_category_favorites, MobileIcons.Favorite)
                MoreRow(R.string.content_category_history, MobileIcons.History)
            }
            MobileGroup {
                // Both land on the same flow, which asks again which of the two it is — but a user
                // who came here to restore a backup should not have to find it behind "add a
                // playlist".
                MoreRow(R.string.setup_add_playlist, MobileIcons.PlaylistAdd, onAddSource)
                MoreRow(R.string.setup_restore_backup, MobileIcons.Restore, onAddSource)
                MoreRow(R.string.settings_sync_now, MobileIcons.Sync)
                MoreRow(R.string.settings_about, MobileIcons.Info)
            }

            // Dev-only, and English-only by the same rule the harness itself lives under: R8 removes
            // both rows and both screens from a published build.
            if (BuildConfig.DEV_TOOLS) {
                MobileGroup {
                    MobileListRow(
                        title = "Theme gallery",
                        leading = { Icon(MobileIcons.Build, contentDescription = null) },
                        onClick = { onDevRoute(DevRoute.GALLERY) },
                    )
                    MobileListRow(
                        title = "Dev harness",
                        leading = { Icon(MobileIcons.Build, contentDescription = null) },
                        onClick = { onDevRoute(DevRoute.HARNESS) },
                    )
                    RebuildTrendingRow()
                }
            }
        }
    }
}

/**
 * The television's "Rebuild Now Trending", on the phone.
 *
 * Trending is read-only on mobile by Plan 4's own rule — the row shows what core last stored, and the
 * refresh is scheduled a few days apart. That timer is the problem when a trending bug is being
 * chased: reproducing it means waiting days, or driving the television. This forces the download for
 * every playlist on the active profile, right now, ignoring the timer.
 *
 * Deliberately unthrottled beyond its own two-second chip, for the reason the television's is:
 * `BuildConfig.DEV_TOOLS` is false in every published APK, so R8 deletes this function and the row
 * that calls it, and a maintainer chasing a bug needs to press it as often as the bug requires.
 *
 * English-only, like the two rows above it and by the same exception — it never reaches a user. The
 * chip is the one exception: core already has that word translated, so it costs nothing to use it.
 */
@Composable
private fun RebuildTrendingRow(
    settings: SettingsRepository = koinInject(),
    sourceDao: SourceDao = koinInject(),
    scheduler: CatalogSyncScheduler = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    MobileListRow(
        title = "Rebuild Now Trending",
        subtitle = if (running) stringResource(R.string.settings_rebuilding) else null,
        leading = { Icon(MobileIcons.Build, contentDescription = null) },
        onClick = {
            if (running) return@MobileListRow
            running = true
            scope.launch {
                val profileId = settings.activeProfileId.first()
                if (profileId >= 0) {
                    sourceDao.sourceIdsForProfile(profileId)
                        .forEach { scheduler.enqueueTrendingRefresh(it, force = true) }
                }
                // The work is a background job, so there is nothing to await — the chip is there to
                // say the press landed, not to report the result. Watch the row itself for that.
                delay(2_500)
                running = false
            }
        },
    )
}

/** The two Plan 3 / Phase 1 scaffolding screens, reachable only in a dev build. */
enum class DevRoute(val route: String) {
    GALLERY("dev_gallery"),
    HARNESS("dev_harness"),
}

@Composable
private fun MoreRow(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {},
) {
    MobileListRow(
        title = stringResource(labelRes),
        leading = { Icon(imageVector = icon, contentDescription = null) },
        onClick = onClick,
    )
}
