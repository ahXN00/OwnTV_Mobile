package tv.own.owntv.mobile.ui.screens.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.CategoryPickerSheet
import tv.own.owntv.mobile.ui.components.FilterChipRow
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.mobileGroupPlate
import tv.own.owntv.mobile.ui.screens.ObeyScrollToTop
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * Live TV: a strip of categories, a list of channels, and a long-press menu on each one.
 *
 * The TV app shows three panels at once — rail, list, preview — because it has the width for it and
 * a remote that moves between them. A phone has neither, so the same three things become one
 * scrolling list, a chip strip above it, and a screen you open by tapping a channel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    scrollToTop: SharedFlow<String>,
    onOpenChannel: (channelId: Long, openCatchup: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    vm: LiveViewModel = koinViewModel(),
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val channels = vm.channels.collectAsLazyPagingItems()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val providers by vm.providerNames.collectAsStateWithLifecycle()
    val showNumbers by vm.showChannelNumbers.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    listState.ObeyScrollToTop(route = "live", scrollToTop = scrollToTop)

    var menuFor by remember { mutableStateOf<ChannelEntity?>(null) }
    var categoryPicker by remember { mutableStateOf(false) }

    // The guide is read for what is actually on screen. Watching the visible range rather than each
    // row means one batched query per scroll settle instead of one per row appearing.
    LaunchedEffect(listState, channels) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { indices ->
                vm.loadNowPlaying(indices.mapNotNull { channels.peek(it) })
            }
    }

    // Changing category scrolls back to the top: the position of the old list means nothing in the new one.
    LaunchedEffect(selected) { listState.scrollToItem(0) }

    Column(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth()) {
            FilterChipRow(
                labels = categories.map { it.label() },
                selectedIndex = categories.indexOfFirst { it.key == selected },
                onSelect = { index -> categories.getOrNull(index)?.let { vm.select(it.key) } },
                modifier = Modifier.padding(end = MobileDimens.TouchTarget),
            )
            IconButton(
                onClick = { categoryPicker = true },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Filled.Search, stringResource(R.string.content_search_categories))
            }
        }
        if (categoryPicker) {
            CategoryPickerSheet(
                labels = categories.map { it.label() },
                selectedIndex = categories.indexOfFirst { it.key == selected },
                onSelect = { index -> categories.getOrNull(index)?.let { vm.select(it.key) } },
                onDismiss = { categoryPicker = false },
            )
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (channels.itemCount == 0) {
                EmptyChannels()
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().mobileGroupPlate()) {
                    items(count = channels.itemCount, key = channels.itemKey { it.id }) { index ->
                        val channel = channels[index]
                        if (channel != null) {
                            ChannelRow(
                                channel = channel,
                                number = channel.number?.takeIf { showNumbers && it > 0 },
                                nowPlaying = nowPlaying[channel.id],
                                providerName = providers[channel.sourceId],
                                isFavorite = channel.id in favorites,
                                onClick = { onOpenChannel(channel.id, false) },
                                onLongClick = { menuFor = channel },
                            )
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { channel ->
        ChannelMenu(
            channel = channel,
            selected = selected,
            isFavorite = channel.id in favorites,
            originName = categories.firstOrNull { it.key == selected }?.label().orEmpty(),
            vm = vm,
            onOpenCatchup = { onOpenChannel(channel.id, true) },
            onDismiss = { menuFor = null },
        )
    }
}

/** The chip's text: a translated label for the four built-in lists, the stored name otherwise. */
@Composable
private fun LiveCategory.label(): String = when (builtIn) {
    LiveCategory.BuiltIn.ALL -> stringResource(R.string.content_category_all_channels)
    LiveCategory.BuiltIn.FAVORITES -> stringResource(R.string.content_category_favorites)
    LiveCategory.BuiltIn.HISTORY -> stringResource(R.string.content_category_history)
    LiveCategory.BuiltIn.CATCHUP -> stringResource(R.string.content_catchup)
    null -> title.orEmpty()
}

@Composable
private fun EmptyChannels() {
    Box(Modifier.fillMaxSize().padding(MobileDimens.GapLarge), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.content_no_channels_here),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * One channel: its number, its logo, its name, and what is on it now.
 *
 * The second line is deliberately shared between the programme title and the playlist name — a
 * phone row has one line to spare, and which of the two is worth showing depends on whether the
 * user has a guide and more than one playlist.
 */
@Composable
private fun ChannelRow(
    channel: ChannelEntity,
    number: Int?,
    nowPlaying: String?,
    providerName: String?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val separator = stringResource(R.string.content_epg_bits_separator)
    val subtitle = listOfNotNull(nowPlaying, providerName).takeIf { it.isNotEmpty() }?.joinToString(separator)

    MobileListRow(
        title = channel.name,
        subtitle = subtitle,
        leading = { ChannelLogo(channel, number) },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapTiny)) {
                if (channel.catchup) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = stringResource(R.string.content_catchup),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(TRAILING_ICON),
                    )
                }
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.content_category_favorites),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(TRAILING_ICON),
                    )
                }
            }
        },
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/** The logo, with the channel number under it when the Channel numbers setting is on. */
@Composable
private fun ChannelLogo(channel: ChannelEntity, number: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val logo = channel.displayLogoUrl
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(LOGO_SIZE),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(LOGO_SIZE),
            )
        }
        if (number != null) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Rows the pager has not loaded yet come back null; [peek] avoids asking it to load them. */
private fun LazyPagingItems<ChannelEntity>.peek(index: Int): ChannelEntity? =
    itemSnapshotList.getOrNull(index)

private val LOGO_SIZE = 32.dp
private val TRAILING_ICON = 18.dp
