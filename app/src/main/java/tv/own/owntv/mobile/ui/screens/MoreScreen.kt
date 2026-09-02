package tv.own.owntv.mobile.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.SharedFlow
import tv.own.owntv.mobile.BuildConfig
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.nav.MobileDestination

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

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        item {
            MoreRow(R.string.common_nav_downloads, Icons.Filled.Download) {
                onNavigate(MobileDestination.DOWNLOADS)
            }
            MoreRow(R.string.common_nav_settings, Icons.Filled.Settings) {
                onNavigate(MobileDestination.SETTINGS)
            }
            HorizontalDivider()
            MoreRow(R.string.profiles_title, Icons.Filled.People)
            MoreRow(R.string.content_category_favorites, Icons.Filled.Favorite)
            MoreRow(R.string.content_category_history, Icons.Filled.History)
            HorizontalDivider()
            // Both land on the same flow, which asks again which of the two it is — but a user who
            // came here to restore a backup should not have to find it behind "add a playlist".
            MoreRow(R.string.setup_add_playlist, Icons.Filled.PlaylistAdd, onAddSource)
            MoreRow(R.string.setup_restore_backup, Icons.Filled.Restore, onAddSource)
            MoreRow(R.string.settings_sync_now, Icons.Filled.Sync)
            MoreRow(R.string.settings_about, Icons.Filled.Info)

            // Dev-only, and English-only by the same rule the harness itself lives under: R8 removes
            // both rows and both screens from a published build.
            if (BuildConfig.DEV_TOOLS) {
                HorizontalDivider()
                MobileListRow(
                    title = "Theme gallery",
                    leading = { Icon(Icons.Filled.Build, contentDescription = null) },
                    onClick = { onDevRoute(DevRoute.GALLERY) },
                )
                MobileListRow(
                    title = "Dev harness",
                    leading = { Icon(Icons.Filled.Build, contentDescription = null) },
                    onClick = { onDevRoute(DevRoute.HARNESS) },
                )
            }
        }
    }
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
