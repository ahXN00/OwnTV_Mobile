package tv.own.owntv.mobile.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.model.ContentMenu
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.ContentMenuSheet
import tv.own.owntv.mobile.ui.components.FilterChipRow
import tv.own.owntv.mobile.ui.components.MobileButton
import tv.own.owntv.mobile.ui.components.MobileButtonStyle
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.SheetAction
import tv.own.owntv.mobile.ui.player.formatTimestamp
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * A film or a show, opened.
 *
 * One screen for both: a film's Play button is the whole of it, a show grows a season strip and its
 * episodes underneath. Everything scrolls as one list, because on a phone a fixed header would leave
 * about four rows of episodes visible.
 */
@Composable
fun DetailScreen(
    tab: LibraryTab,
    itemId: Long,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DetailViewModel = koinViewModel(),
) {
    LaunchedEffect(tab, itemId) { vm.open(tab, itemId) }

    val movie by vm.movie.collectAsStateWithLifecycle()
    val show by vm.show.collectAsStateWithLifecycle()
    val episodes by vm.episodes.collectAsStateWithLifecycle()
    val seasons by vm.seasons.collectAsStateWithLifecycle()
    val season by vm.season.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val favorite by vm.isFavorite.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val episodeProgress by vm.episodeProgress.collectAsStateWithLifecycle()

    val title = movie?.name ?: show?.name.orEmpty()
    val plot = movie?.plot ?: show?.plot
    val poster = movie?.posterUrl ?: show?.posterUrl
    val backdrop = movie?.backdropUrl ?: show?.backdropUrl
    val year = movie?.year ?: show?.year
    val rating = movie?.rating ?: show?.rating
    // Null until the show has loaded; then the last-watched season, or its first.
    val currentSeason = season ?: seasons.firstOrNull()
    val shown = episodes.filter { it.seasonNumber == currentSeason }
    val resumeMs = progress?.takeIf { it.durationMs > 1L }?.positionMs ?: 0L

    var menuFor by remember { mutableStateOf<EpisodeEntity?>(null) }
    val listState = rememberLazyListState()

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                AsyncImage(
                    model = backdrop ?: poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.padding(MobileDimens.ScreenPaddingH)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
                    modifier = Modifier.padding(vertical = MobileDimens.GapSmall),
                ) {
                    year?.takeIf { it > 0 }?.let { Chip(it.toString()) }
                    rating?.takeIf { it > 0 }?.let {
                        Chip(stringResource(R.string.content_rating, it.toFloat()))
                    }
                    movie?.durationSecs?.takeIf { it > 0 }?.let {
                        Chip(formatTimestamp(it * 1000L))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
                ) {
                    MobileButton(
                        text = if (resumeMs > 0) {
                            stringResource(R.string.content_resume_at, formatTimestamp(resumeMs))
                        } else {
                            stringResource(R.string.content_play)
                        },
                        onClick = { vm.play(resumeMs, onPlay) },
                    )
                    if (resumeMs > 0) {
                        MobileButton(
                            text = stringResource(R.string.content_play),
                            onClick = { vm.play(0L, onPlay) },
                            style = MobileButtonStyle.SECONDARY,
                        )
                    }
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(
                                if (favorite) R.string.content_remove_favourite
                                else R.string.content_add_favourite,
                            ),
                        )
                    }
                    IconButton(onClick = { vm.download() }) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = stringResource(R.string.content_download),
                        )
                    }
                }
                if (!plot.isNullOrBlank()) {
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MobileDimens.GapMedium),
                    )
                }
            }
        }

        if (tab == LibraryTab.SERIES) {
            item {
                if (seasons.size > 1) {
                    FilterChipRow(
                        labels = seasons.map { stringResource(R.string.content_season, it) },
                        selectedIndex = seasons.indexOf(currentSeason),
                        onSelect = { index -> seasons.getOrNull(index)?.let { vm.selectSeason(it) } },
                    )
                }
                if (loading) {
                    Box(
                        Modifier.fillMaxWidth().padding(MobileDimens.GapLarge),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                } else if (shown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.content_no_episodes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(MobileDimens.GapLarge),
                    )
                }
            }
            items(shown, key = { it.id }) { episode ->
                val watched = episodeProgress[episode.id]
                EpisodeRow(
                    episode = episode,
                    positionMs = watched?.positionMs ?: 0L,
                    durationMs = watched?.durationMs ?: 0L,
                    onClick = { vm.playEpisode(episode.id, watched?.positionMs ?: 0L, onPlay) },
                    onLongClick = { menuFor = episode },
                )
                HorizontalDivider()
            }
        }
    }

    menuFor?.let { episode ->
        EpisodeMenu(
            episode = episode,
            watched = episodeProgress[episode.id].isFinished(),
            vm = vm,
            onPlay = onPlay,
            onDismiss = { menuFor = null },
        )
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeEntity,
    positionMs: Long,
    durationMs: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column {
        MobileListRow(
            title = if (episode.name.isBlank()) {
                stringResource(R.string.content_season_episode, episode.seasonNumber, episode.episodeNumber)
            } else {
                stringResource(
                    R.string.content_season_episode_title,
                    episode.seasonNumber,
                    episode.episodeNumber,
                    episode.name,
                )
            },
            subtitle = episode.plot?.takeIf { it.isNotBlank() },
            onClick = onClick,
            onLongClick = onLongClick,
        )
        // Only an episode actually started has a line, and a finished one is full rather than reset.
        if (positionMs > 0 && durationMs > 1) {
            LinearProgressIndicator(
                progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MobileDimens.ScreenPaddingH),
            )
        }
    }
}

/** The long-press menu for one episode. */
@Composable
private fun EpisodeMenu(
    episode: EpisodeEntity,
    watched: Boolean,
    vm: DetailViewModel,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val actions = listOf(
        SheetAction(
            key = "mark_watched",
            label = stringResource(
                if (watched) R.string.content_mark_unwatched else R.string.content_mark_watched,
            ),
            icon = if (watched) Icons.Filled.RadioButtonUnchecked else Icons.Filled.CheckCircle,
            onClick = { vm.setEpisodeWatched(episode, !watched) },
        ),
        SheetAction(
            key = "download",
            label = stringResource(R.string.content_download),
            icon = Icons.Filled.Download,
            group = 1,
            onClick = { vm.download(episode) },
        ),
        SheetAction(
            key = "play_external",
            label = stringResource(R.string.content_play_external_short),
            icon = Icons.Filled.OpenInNew,
            group = 1,
            onClick = { vm.playExternal(episode) {} },
        ),
        SheetAction(
            key = "play",
            label = stringResource(R.string.content_play),
            icon = Icons.Filled.PlayArrow,
            group = 1,
            onClick = { vm.playEpisode(episode.id, 0L, onPlay) },
        ),
    )
    ContentMenuSheet(
        menu = ContentMenu.EPISODE,
        title = episode.name.ifBlank {
            stringResource(R.string.content_season_episode, episode.seasonNumber, episode.episodeNumber)
        },
        actions = actions,
        onDismiss = onDismiss,
    )
}

@Composable
private fun Chip(label: String) {
    AssistChip(onClick = { }, label = { Text(label) })
}

/** A finished item carries the 1 ms / 1 ms marker, or simply a position at the very end. */
private fun tv.own.owntv.core.database.entity.PlaybackProgressEntity?.isFinished(): Boolean =
    this != null && durationMs > 0 && positionMs >= (durationMs * 0.95f).toLong()
