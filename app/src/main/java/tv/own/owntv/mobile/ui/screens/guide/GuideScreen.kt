package tv.own.owntv.mobile.ui.screens.guide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.FilterChipRow
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.MobileTextField
import tv.own.owntv.mobile.ui.screens.ObeyScrollToTop
import tv.own.owntv.mobile.ui.screens.live.LiveCategory
import tv.own.owntv.mobile.ui.theme.MobileDimens
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * The guide, on a screen you hold.
 *
 * The television draws one thing — a grid — because it has the width for it and a remote to fling
 * across it. A phone gets three, and remembers which one was picked: a list of what is on right now
 * (the only shape that fits a portrait phone), the grid itself for a tablet or a turned phone, and a
 * single channel's schedule read top to bottom.
 */
@Composable
fun GuideScreen(
    scrollToTop: SharedFlow<String>,
    onOpenChannel: (Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: GuideViewModel = koinViewModel(),
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val channels = vm.channels.collectAsLazyPagingItems()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    val window by vm.window.collectAsStateWithLifecycle()
    val storedMode by vm.viewMode.collectAsStateWithLifecycle()
    val density by vm.densityPct.collectAsStateWithLifecycle()

    // Nothing has been chosen yet: a grid needs width, so a portrait phone opens on the "on now" list.
    val wide = LocalConfiguration.current.screenWidthDp >= WIDE_DP
    val mode = storedMode
        ?: if (wide) SettingsRepository.GuideView.GRID else SettingsRepository.GuideView.ON_NOW

    val listState = rememberLazyListState()
    listState.ObeyScrollToTop(route = "guide", scrollToTop = scrollToTop)

    var optionsOpen by remember { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<Pair<ChannelEntity, EpgProgrammeEntity>?>(null) }

    // A different category, day or search is a different list; the old scroll position means nothing.
    LaunchedEffect(selected, day, query, mode) { listState.scrollToItem(0) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MobileTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = stringResource(R.string.content_epg_search_hint),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { optionsOpen = true }) {
                Icon(Icons.Filled.Tune, stringResource(R.string.content_epg_title))
            }
        }
        FilterChipRow(
            labels = categories.map { it.label() },
            selectedIndex = categories.indexOfFirst { it.key == selected },
            onSelect = { index -> categories.getOrNull(index)?.let { vm.select(it.key) } },
        )
        DayStrip(selected = day, onSelect = vm::selectDay)

        if (channels.itemCount == 0) {
            EmptyGuide()
        } else {
            when (mode) {
                SettingsRepository.GuideView.ON_NOW -> OnNowList(
                    vm = vm,
                    channels = channels,
                    listState = listState,
                    favorites = favorites,
                    onOpen = { channel, programme -> sheetFor = channel to programme },
                    onOpenChannel = onOpenChannel,
                )
                SettingsRepository.GuideView.GRID -> GuideGrid(
                    vm = vm,
                    channels = channels,
                    listState = listState,
                    window = window,
                    densityPct = density,
                    onOpen = { channel, programme -> sheetFor = channel to programme },
                    onOpenChannel = onOpenChannel,
                )
                SettingsRepository.GuideView.TIMELINE -> GuideTimeline(
                    vm = vm,
                    channels = channels,
                    listState = listState,
                    onOpen = { channel, programme -> sheetFor = channel to programme },
                )
            }
        }
    }

    if (optionsOpen) {
        GuideOptionsSheet(
            mode = mode,
            densityPct = density,
            onMode = { vm.setViewMode(it); optionsOpen = false },
            onDensity = vm::setDensityPct,
            onDismiss = { optionsOpen = false },
        )
    }
    sheetFor?.let { (channel, programme) ->
        ProgrammeSheet(
            channel = channel,
            programme = programme,
            isFavorite = channel.id in favorites,
            vm = vm,
            onOpenChannel = onOpenChannel,
            onDismiss = { sheetFor = null },
        )
    }
}

/** Today first, then the week the guide usually holds. A day with nothing in it shows as empty. */
@Composable
private fun DayStrip(selected: Int, onSelect: (Int) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val labels = remember(locale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
        val format = SimpleDateFormat(pattern, locale)
        val cal = Calendar.getInstance()
        List(GUIDE_DAYS) { offset ->
            val day = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            format.format(day.time)
        }
    }
    FilterChipRow(labels = labels, selectedIndex = selected, onSelect = onSelect)
}

/**
 * What is on every channel right now, and what follows it.
 *
 * This is the guide a phone can actually read: one channel per row, the programme underneath the
 * name, and a bar showing how much of it is gone. The rows on screen are answered in one query.
 */
@Composable
private fun OnNowList(
    vm: GuideViewModel,
    channels: LazyPagingItems<ChannelEntity>,
    listState: LazyListState,
    favorites: Set<Long>,
    onOpen: (ChannelEntity, EpgProgrammeEntity) -> Unit,
    onOpenChannel: (Long) -> Unit,
) {
    val onNow by vm.onNow.collectAsStateWithLifecycle()

    LaunchedEffect(listState, channels) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { indices ->
                vm.loadOnNow(indices.mapNotNull { channels.itemSnapshotList.getOrNull(it) })
            }
    }

    val times = rememberGuideTimeFormat()
    val separator = stringResource(R.string.content_epg_bits_separator)
    val nextLabel = stringResource(R.string.content_next_up)

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(count = channels.itemCount, key = channels.itemKey { it.id }) { index ->
            val channel = channels[index] ?: return@items
            val slot = onNow[channel.id]
            val now = slot?.now
            MobileListRow(
                title = channel.name,
                subtitle = now?.title ?: stringResource(R.string.content_epg_no_guide),
                leading = { ChannelLogo(channel) },
                trailing = {
                    if (channel.id in favorites) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.content_category_favorites),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(TRAILING_ICON),
                        )
                    }
                },
                // Without a programme there is nothing to open a sheet about — go straight to the channel.
                onClick = { if (now != null) onOpen(channel, now) else onOpenChannel(channel.id) },
                onLongClick = { onOpenChannel(channel.id) },
            )
            if (now != null) {
                NowProgress(now)
                slot.next?.let { next ->
                    Text(
                        text = nextLabel + separator + times.format(Date(next.startMs)) + separator + next.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            start = MobileDimens.ScreenPaddingH,
                            end = MobileDimens.ScreenPaddingH,
                            bottom = MobileDimens.GapSmall,
                        ),
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

/** How much of the current programme has already gone. */
@Composable
private fun NowProgress(programme: EpgProgrammeEntity) {
    val span = (programme.stopMs - programme.startMs).coerceAtLeast(1)
    val done = (System.currentTimeMillis() - programme.startMs).toFloat() / span
    LinearProgressIndicator(
        progress = { done.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapTiny),
    )
}

@Composable
internal fun ChannelLogo(channel: ChannelEntity) {
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
}

/**
 * One programme, opened.
 *
 * The synopsis is fetched here rather than carried by the list: a day of every channel's descriptions
 * is megabytes of text, and only the one that was tapped is ever read.
 */
@Composable
private fun ProgrammeSheet(
    channel: ChannelEntity,
    programme: EpgProgrammeEntity,
    isFavorite: Boolean,
    vm: GuideViewModel,
    onOpenChannel: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var description by remember(programme.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(programme.id) { description = vm.description(programme.id) }

    val times = rememberGuideTimeFormat()

    MobileBottomSheet(onDismissRequest = onDismiss, title = programme.title) {
        Text(
            text = stringResource(
                R.string.content_epg_time_range,
                times.format(Date(programme.startMs)),
                times.format(Date(programme.stopMs)),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
        )
        description?.takeIf { it.isNotBlank() }?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = MobileDimens.ScreenPaddingH,
                    vertical = MobileDimens.GapSmall,
                ),
            )
        }
        HorizontalDivider()
        MobileListRow(
            title = stringResource(R.string.content_epg_watch_channel),
            onClick = { onDismiss(); onOpenChannel(channel.id) },
        )
        if (vm.canCatchup(channel, programme)) {
            MobileListRow(
                title = stringResource(R.string.content_epg_watch_start),
                onClick = { onDismiss(); vm.playCatchup(channel, programme); onOpenChannel(channel.id) },
            )
        }
        MobileListRow(
            title = stringResource(
                if (isFavorite) R.string.content_epg_unfavourite else R.string.content_epg_favourite,
            ),
            leading = {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                )
            },
            onClick = { vm.toggleFavorite(channel); onDismiss() },
        )
    }
}

/** Which of the three shapes the guide takes, and — for the grid — how much time a screen holds. */
@Composable
private fun GuideOptionsSheet(
    mode: SettingsRepository.GuideView,
    densityPct: Int,
    onMode: (SettingsRepository.GuideView) -> Unit,
    onDensity: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.content_epg_title)) {
        SettingsRepository.GuideView.entries.forEach { entry ->
            MobileListRow(
                title = stringResource(entry.labelRes()),
                leading = { RadioButton(selected = entry == mode, onClick = { onMode(entry) }) },
                onClick = { onMode(entry) },
            )
        }
        if (mode == SettingsRepository.GuideView.GRID) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(
                    start = MobileDimens.ScreenPaddingH,
                    top = MobileDimens.GapSmall,
                ),
            )
            Slider(
                value = densityPct.toFloat(),
                onValueChange = { onDensity(it.toInt()) },
                valueRange = MIN_DENSITY.toFloat()..MAX_DENSITY.toFloat(),
                modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
            )
        }
    }
}

@Composable
private fun EmptyGuide() {
    Box(Modifier.fillMaxSize().padding(MobileDimens.GapLarge), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.content_epg_add_playlist),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The chip's text: a translated label for the two built-in lists, the stored name otherwise. */
@Composable
private fun LiveCategory.label(): String = when (builtIn) {
    LiveCategory.BuiltIn.ALL -> stringResource(R.string.content_epg_all_categories)
    LiveCategory.BuiltIn.FAVORITES -> stringResource(R.string.content_category_favorites)
    else -> title.orEmpty()
}

private fun SettingsRepository.GuideView.labelRes() = when (this) {
    SettingsRepository.GuideView.GRID -> R.string.settings_view_grid
    SettingsRepository.GuideView.ON_NOW -> R.string.home_row_on_now
    SettingsRepository.GuideView.TIMELINE -> R.string.settings_guide_width_epg
}

/** Programme clocks follow the phone's own locale, so 20:00 and 8:00 PM are both right somewhere. */
@Composable
internal fun rememberGuideTimeFormat(): DateFormat {
    val locales = LocalConfiguration.current.locales
    return remember(locales) { DateFormat.getTimeInstance(DateFormat.SHORT) }
}

/** Today plus the week most providers publish. */
internal const val GUIDE_DAYS = 7

internal const val MIN_DENSITY = 70
internal const val MAX_DENSITY = 130

private const val WIDE_DP = 600
private val LOGO_SIZE = 32.dp
private val TRAILING_ICON = 18.dp
