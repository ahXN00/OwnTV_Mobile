package tv.own.owntv.mobile.ui.screens.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.model.ContentMenu
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.ContentMenuSheet
import tv.own.owntv.mobile.ui.components.SheetAction

/** The row a long press was made on. */
data class SearchTarget(val type: MediaType, val id: Long, val title: String)

/**
 * The long-press menu for a search result.
 *
 * A short menu on purpose: everything a result can do to itself, and nothing that needs the list it
 * would normally sit in — reordering a folder from a search result has no folder to reorder. The
 * keys are the ones the rest of the app uses, so the order the user arranged still applies.
 */
@Composable
fun SearchMenu(
    target: SearchTarget,
    isFavorite: Boolean,
    vm: SearchViewModel,
    onDismiss: () -> Unit,
) {
    val actions = buildList {
        add(
            SheetAction(
                key = "favourite",
                label = stringResource(
                    if (isFavorite) R.string.content_remove_favourite else R.string.content_add_favourite,
                ),
                icon = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                group = 0,
                onClick = { vm.toggleFavorite(target.type, target.id) },
            ),
        )
        if (target.type != MediaType.LIVE) {
            add(
                SheetAction(
                    key = "download",
                    label = stringResource(
                        if (target.type == MediaType.MOVIE) R.string.content_download
                        else R.string.content_download_all_episodes,
                    ),
                    icon = Icons.Filled.Download,
                    group = 1,
                    onClick = { vm.download(target.type, target.id) },
                ),
            )
        }
        add(
            SheetAction(
                key = "hide",
                label = stringResource(
                    if (target.type == MediaType.LIVE) R.string.content_hide_channel else R.string.common_hide,
                ),
                icon = Icons.Filled.VisibilityOff,
                destructive = true,
                group = 2,
                onClick = { vm.hide(target.type, target.id) },
            ),
        )
    }

    ContentMenuSheet(
        menu = when (target.type) {
            MediaType.LIVE -> ContentMenu.LIVE
            MediaType.MOVIE -> ContentMenu.MOVIE
            else -> ContentMenu.SERIES
        },
        title = target.title,
        actions = actions,
        onDismiss = onDismiss,
    )
}
