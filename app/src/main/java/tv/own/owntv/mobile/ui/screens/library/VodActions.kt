package tv.own.owntv.mobile.ui.screens.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.core.model.ContentMenu
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.ContentMenuSheet
import tv.own.owntv.mobile.ui.components.SheetAction

/**
 * The long-press menu for a film or a show.
 *
 * The keys are the TV app's, because the order the user arranged in Settings is saved against exactly
 * those strings — a menu rearranged on the television comes out rearranged here.
 */
@Composable
fun VodMenu(
    item: VodItem,
    tab: LibraryTab,
    selected: LiveKey,
    isFavorite: Boolean,
    vm: LibraryViewModel,
    onDismiss: () -> Unit,
) {
    val movie = tab == LibraryTab.MOVIES
    val watched = movie && vm.isWatched(item.id)

    val actions = buildList {
        add(
            SheetAction(
                key = "favourite",
                label = stringResource(
                    if (isFavorite) R.string.content_remove_favourite else R.string.content_add_favourite,
                ),
                icon = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                group = 0,
                onClick = { vm.toggleFavorite(item.id) },
            ),
        )
        if (movie) {
            add(
                SheetAction(
                    key = "mark_watched",
                    label = stringResource(
                        if (watched) R.string.content_mark_unwatched else R.string.content_mark_watched,
                    ),
                    icon = if (watched) Icons.Filled.RadioButtonUnchecked else Icons.Filled.CheckCircle,
                    group = 0,
                    onClick = { vm.setWatched(item.id, !watched) },
                ),
            )
            add(
                SheetAction(
                    key = "play_external",
                    label = stringResource(R.string.content_play_external_short),
                    icon = Icons.Filled.OpenInNew,
                    group = 1,
                    onClick = { vm.playExternal(item.id) },
                ),
            )
        }
        add(
            SheetAction(
                key = "download",
                label = stringResource(
                    if (movie) R.string.content_download else R.string.content_download_all_episodes,
                ),
                icon = Icons.Filled.Download,
                group = 1,
                onClick = { vm.download(item.id) },
            ),
        )
        add(
            SheetAction(
                key = "hide",
                label = stringResource(R.string.common_hide),
                icon = Icons.Filled.VisibilityOff,
                destructive = true,
                group = 2,
                onClick = { vm.hide(item.id) },
            ),
        )
        if (selected == LiveKey.History) {
            add(
                SheetAction(
                    key = "remove_history",
                    label = stringResource(R.string.content_remove_history),
                    destructive = true,
                    group = 2,
                    onClick = { vm.removeFromHistory(item.id) },
                ),
            )
        }
    }

    ContentMenuSheet(
        menu = if (movie) ContentMenu.MOVIE else ContentMenu.SERIES,
        title = item.name,
        actions = actions,
        onDismiss = onDismiss,
    )
}
