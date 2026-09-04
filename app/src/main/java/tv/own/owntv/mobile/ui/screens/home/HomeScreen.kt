package tv.own.owntv.mobile.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.SharedFlow
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.home.GuideSliceState
import tv.own.owntv.core.home.HeroItem
import tv.own.owntv.core.home.HomeFeed
import tv.own.owntv.core.home.TrendingHomeItem
import tv.own.owntv.core.launcher.LauncherContinuationItem
import tv.own.owntv.core.launcher.LauncherWatchNextType
import tv.own.owntv.core.model.HomeLiveRowMode
import tv.own.owntv.core.model.HomeRow
import tv.own.owntv.core.weather.WeatherInfo
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.PosterCard
import tv.own.owntv.mobile.ui.components.SectionHeader
import tv.own.owntv.mobile.ui.screens.ObeyScrollToTop
import tv.own.owntv.mobile.ui.theme.MobileCardShape
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * Home, on a phone.
 *
 * The same rows as the television, in the same order, hidden by the same choices — but read top to
 * bottom instead of across, with each row sliding sideways under the thumb. Everything on it is a
 * shortcut back into something already started, so a tap resumes rather than opens: a film goes
 * straight to the picture, a channel to its own screen.
 */
@Composable
fun HomeScreen(
    scrollToTop: SharedFlow<String>,
    onOpenChannel: (Long) -> Unit,
    onOpenMovie: (Long) -> Unit,
    onOpenSeries: (Long) -> Unit,
    onPlayerOpened: () -> Unit,
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = koinViewModel(),
) {
    val feed by vm.feed.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val fahrenheit by vm.fahrenheit.collectAsStateWithLifecycle()

    // Coming back from a film is exactly when "continue watching" is out of date.
    LaunchedEffect(Unit) { vm.refresh() }

    val listState = rememberLazyListState()
    listState.ObeyScrollToTop(route = MobileHomeRoute, scrollToTop = scrollToTop)

    val state = feed ?: return
    val rows = state.config.visibleOrder.filter { state.hasContent(it) }

    if (rows.isEmpty()) {
        EmptyHome(onAddSource = onAddSource, modifier = modifier)
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MobileDimens.GapLarge),
    ) {
        weather?.let { info ->
            item("weather") { WeatherChip(info = info, fahrenheit = fahrenheit) }
        }
        rows.forEach { row ->
            item(row.name) {
                when (row) {
                    HomeRow.TRENDING -> TrendingRow(state.trendingItems, onOpenMovie, onOpenSeries)
                    HomeRow.HERO -> HeroRow(
                        items = state.heroItems,
                        onOpenChannel = onOpenChannel,
                        onPlayMovie = { id, position -> vm.playMovie(id, position, onPlayerOpened) },
                        onPlayEpisode = { id, position -> vm.playEpisode(id, position, onPlayerOpened) },
                    )
                    HomeRow.RECENT_CHANNELS -> LiveRow(
                        title = stringResource(R.string.home_row_recent_channels),
                        channels = state.recentLive,
                        guide = state.recentGuide,
                        mode = vm.modeOf(row, state),
                        onToggleMode = { vm.toggleLiveMode(row) },
                        onOpenChannel = onOpenChannel,
                    )
                    HomeRow.FAVORITE_CHANNELS -> LiveRow(
                        title = stringResource(R.string.home_row_favorite_channels),
                        channels = state.favoriteLive,
                        guide = state.favoriteGuide,
                        mode = vm.modeOf(row, state),
                        onToggleMode = { vm.toggleLiveMode(row) },
                        onOpenChannel = onOpenChannel,
                    )
                    HomeRow.CONTINUE_MOVIES -> ContinueRow(
                        title = stringResource(R.string.home_row_continue_movies),
                        items = state.continueMovies,
                        onPlay = { item -> vm.playMovie(item.sourceItemId, item.positionMs, onPlayerOpened) },
                    )
                    HomeRow.CONTINUE_SERIES -> ContinueRow(
                        title = stringResource(R.string.home_row_continue_series),
                        items = state.continueSeries,
                        onPlay = { item -> vm.playEpisode(item.targetItemId, item.positionMs, onPlayerOpened) },
                    )
                }
            }
        }
    }
}

/** A row with nothing in it is not drawn at all — an empty heading is worse than one row fewer. */
private fun HomeFeed.hasContent(row: HomeRow): Boolean = when (row) {
    HomeRow.TRENDING -> trendingItems.isNotEmpty()
    HomeRow.HERO -> heroItems.isNotEmpty()
    HomeRow.RECENT_CHANNELS -> recentLive.isNotEmpty()
    HomeRow.FAVORITE_CHANNELS -> favoriteLive.isNotEmpty()
    HomeRow.CONTINUE_MOVIES -> continueMovies.isNotEmpty()
    HomeRow.CONTINUE_SERIES -> continueSeries.isNotEmpty()
}

/**
 * The hero: one card per thing worth carrying on with, each as wide as the screen and snapping into
 * place, so the row reads as a stack of cards rather than a strip of thumbnails.
 */
@Composable
private fun HeroRow(
    items: List<HeroItem>,
    onOpenChannel: (Long) -> Unit,
    onPlayMovie: (Long, Long) -> Unit,
    onPlayEpisode: (Long, Long) -> Unit,
) {
    val cardWidth = LocalConfiguration.current.screenWidthDp.dp - MobileDimens.ScreenPaddingH * 2
    val state = rememberLazyListState()
    Column {
        SectionHeader(title = stringResource(R.string.home_row_keep_watching))
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapMedium),
            contentPadding = PaddingValues(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            items(items.size, key = { items[it].rowKey() }) { index ->
                val item = items[index]
                HeroCard(
                    item = item,
                    modifier = Modifier.width(cardWidth),
                    onPlay = {
                        when (item) {
                            is HeroItem.LiveHero -> onOpenChannel(item.channel.id)
                            is HeroItem.MovieHero -> onPlayMovie(item.movie.id, item.seekToMs)
                            is HeroItem.SeriesHero -> onPlayEpisode(item.episode.id, item.seekToMs)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HeroCard(item: HeroItem, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val artwork = when (item) {
        is HeroItem.MovieHero -> item.movie.backdropUrl ?: item.movie.posterUrl
        is HeroItem.SeriesHero -> item.series.backdropUrl ?: item.series.posterUrl
        is HeroItem.LiveHero -> item.channel.displayLogoUrl
    }
    val title = when (item) {
        is HeroItem.MovieHero -> item.movie.name
        is HeroItem.SeriesHero -> item.series.name
        is HeroItem.LiveHero -> item.channel.name
    }
    val subtitle = when (item) {
        is HeroItem.MovieHero -> item.movie.year?.toString()
        is HeroItem.SeriesHero -> item.item.subtitle
        is HeroItem.LiveHero -> null
    }
    // A channel's logo is a small transparent picture, not a backdrop: it is centred at its own size
    // rather than stretched across the card, which is the one place the three variants really differ.
    val live = item is HeroItem.LiveHero

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(MobileCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onPlay),
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = if (live) ContentScale.Fit else ContentScale.Crop,
                modifier = if (live) {
                    Modifier.align(Alignment.Center).size(HeroLogoSize).padding(MobileDimens.GapMedium)
                } else {
                    Modifier.fillMaxSize()
                },
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))),
                )
                .padding(MobileDimens.GapMedium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live) {
                    Icon(
                        Icons.Filled.LiveTv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(HeroBadgeSize).padding(end = MobileDimens.GapTiny),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.durationMs > 0 && item.positionMs > 0) {
                LinearProgressIndicator(
                    progress = { (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MobileDimens.GapSmall)
                        .height(MobileDimens.PosterProgressHeight),
                )
            }
            Button(onClick = onPlay, modifier = Modifier.padding(top = MobileDimens.GapSmall)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(item.actionLabel()),
                    modifier = Modifier.padding(start = MobileDimens.GapSmall),
                )
            }
        }
    }
}

private fun HeroItem.rowKey(): String = when (this) {
    is HeroItem.MovieHero -> "m${movie.id}"
    is HeroItem.SeriesHero -> "e${episode.id}"
    is HeroItem.LiveHero -> "c${channel.id}"
}

private fun HeroItem.actionLabel(): Int = when {
    this is HeroItem.LiveHero -> R.string.content_action_play
    watchNextType == LauncherWatchNextType.NEXT -> R.string.content_action_next_up
    positionMs > 0 -> R.string.content_action_resume
    else -> R.string.content_action_play
}

/** Trending is read-only here: core's worker decides what is in it, and this only shows it. */
@Composable
private fun TrendingRow(
    items: List<TrendingHomeItem>,
    onOpenMovie: (Long) -> Unit,
    onOpenSeries: (Long) -> Unit,
) {
    Column {
        SectionHeader(title = stringResource(R.string.home_row_now_trending))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
            contentPadding = PaddingValues(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            items(items.size, key = { items[it].stableKey }) { index ->
                when (val item = items[index]) {
                    is TrendingHomeItem.Movie -> PosterCard(
                        title = item.movie.name,
                        imageUrl = item.movie.posterUrl,
                        onClick = { onOpenMovie(item.movie.id) },
                    )
                    is TrendingHomeItem.Series -> PosterCard(
                        title = item.series.name,
                        imageUrl = item.series.posterUrl,
                        onClick = { onOpenSeries(item.series.id) },
                    )
                }
            }
        }
    }
}

/** Films or episodes left half-watched, each with the bar showing how far in. */
@Composable
private fun ContinueRow(
    title: String,
    items: List<LauncherContinuationItem>,
    onPlay: (LauncherContinuationItem) -> Unit,
) {
    Column {
        SectionHeader(title = title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
            contentPadding = PaddingValues(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            items(items.size, key = { items[it].stableKey }) { index ->
                val item = items[index]
                PosterCard(
                    title = item.title,
                    imageUrl = item.posterUrl,
                    subtitle = item.subtitle,
                    progress = if (item.durationMs > 0 && item.positionMs > 0) {
                        (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
                    } else {
                        null
                    },
                    onClick = { onPlay(item) },
                )
            }
        }
    }
}

/**
 * A channel row in either of the two shapes the setting offers: logos to pick from, or what is on
 * each of them right now. The header's action switches between them, and it is stored where the
 * television reads it — switch it here and it is switched there.
 */
@Composable
private fun LiveRow(
    title: String,
    channels: List<ChannelEntity>,
    guide: GuideSliceState,
    mode: HomeLiveRowMode,
    onToggleMode: () -> Unit,
    onOpenChannel: (Long) -> Unit,
) {
    Column {
        SectionHeader(
            title = title,
            actionLabel = stringResource(
                when (mode) {
                    HomeLiveRowMode.CARDS -> R.string.home_row_on_now
                    HomeLiveRowMode.ON_NOW -> R.string.home_row_cards
                },
            ),
            onAction = onToggleMode,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
            contentPadding = PaddingValues(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            items(channels.size, key = { channels[it].id }) { index ->
                val channel = channels[index]
                if (mode == HomeLiveRowMode.ON_NOW) {
                    OnNowCard(
                        channel = channel,
                        guide = guide,
                        onClick = { onOpenChannel(channel.id) },
                    )
                } else {
                    ChannelCard(channel = channel, onClick = { onOpenChannel(channel.id) })
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(channel: ChannelEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(ChannelCardWidth)
            .clip(MobileCardShape)
            .clickable(onClick = onClick)
            .padding(MobileDimens.GapSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChannelLogo(channel, ChannelLogoSize)
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = MobileDimens.GapSmall),
        )
    }
}

@Composable
private fun OnNowCard(channel: ChannelEntity, guide: GuideSliceState, onClick: () -> Unit) {
    val now = guide.programmes[channel.id]?.firstOrNull { guide.now in it.startMs until it.stopMs }
    Row(
        modifier = Modifier
            .width(OnNowCardWidth)
            .clip(MobileCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(MobileDimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelLogo(channel, ChannelLogoSize)
        Column(modifier = Modifier.padding(start = MobileDimens.GapSmall)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = now?.title ?: stringResource(R.string.content_no_epg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (now != null && now.stopMs > now.startMs) {
                LinearProgressIndicator(
                    progress = {
                        ((guide.now - now.startMs).toFloat() / (now.stopMs - now.startMs)).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MobileDimens.GapTiny)
                        .height(MobileDimens.PosterProgressHeight),
                )
            }
        }
    }
}

@Composable
private fun ChannelLogo(channel: ChannelEntity, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(MobileDimens.PosterArtCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val logo = channel.displayLogoUrl
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(MobileDimens.GapTiny),
            )
        } else {
            Icon(
                Icons.Filled.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Temperature, place and sky, in the unit the user chose. */
@Composable
private fun WeatherChip(info: WeatherInfo, fahrenheit: Boolean) {
    val temperature = if (fahrenheit) {
        stringResource(R.string.common_weather_fahrenheit, (info.temperatureC * 9 / 5 + 32).toInt())
    } else {
        stringResource(R.string.common_weather_celsius, info.temperatureC.toInt())
    }
    val text =
        if (info.city.isNotBlank()) stringResource(R.string.common_weather_city, temperature, info.city)
        else temperature
    Row(
        modifier = Modifier.padding(
            horizontal = MobileDimens.ScreenPaddingH,
            vertical = MobileDimens.GapSmall,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = info.conditionIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(WeatherIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = MobileDimens.GapSmall),
        )
    }
}

/**
 * The WMO code, drawn with Material's own icons rather than the television's canvas artwork. Read
 * off the code and not [WeatherInfo.symbolKey], whose keys name the TV's drawings.
 */
private fun WeatherInfo.conditionIcon(): ImageVector = when {
    weatherCode <= 2 && isDay -> Icons.Filled.WbSunny
    weatherCode <= 2 -> Icons.Filled.DarkMode
    weatherCode in 51..57 -> Icons.Filled.Grain
    weatherCode in 61..67 || weatherCode in 80..82 -> Icons.Filled.WaterDrop
    weatherCode in 71..77 || weatherCode in 85..86 -> Icons.Filled.AcUnit
    weatherCode in 95..99 -> Icons.Filled.Bolt
    else -> Icons.Filled.Cloud
}

@Composable
private fun EmptyHome(onAddSource: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(MobileDimens.GapLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.content_epg_add_playlist_first),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddSource, modifier = Modifier.padding(top = MobileDimens.GapMedium)) {
            Text(stringResource(R.string.setup_add_playlist))
        }
    }
}

/** The route this screen answers a "scroll back to the top" tap for. */
private const val MobileHomeRoute = "home"

private val HeroLogoSize = 120.dp
private val HeroBadgeSize = 20.dp
private val ChannelCardWidth = 96.dp
private val ChannelLogoSize = 56.dp
private val OnNowCardWidth = 240.dp
private val WeatherIconSize = 18.dp
