package tv.own.owntv.mobile.ui.screens.library

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.HistoryDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.ProgressDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.PlaybackProgressEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.database.entity.WatchHistoryEntity
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.player.ExternalPlayerLauncher
import tv.own.owntv.core.player.enginePinKey
import tv.own.owntv.core.database.dao.resolveExistingProfileId
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.stalker.ReconnectUrlProvider
import tv.own.owntv.core.stalker.StreamUrlResolver
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.playback.DataSaverGate
import tv.own.owntv.mobile.playback.PlaybackService
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.player.MpvPlaybackEngine
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.PlaybackSession

/** A film or an episode, playing. [mediaType] and [itemId] are what the resume position is written for. */
data class VodPlayback(
    val mediaType: MediaType,
    val itemId: Long,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
)

/**
 * The VOD half of what is playing — [LiveTuner]'s twin, and for the same reason: a film outlives the
 * screen that started it, so the full screen player and the mini player are two views of one stream.
 *
 * There is one engine and one surface, so the two tuners cannot both be playing. This one holds the
 * live tuner and stops it before it starts, and clears itself when the live tuner starts something —
 * one direction only, which is what keeps the pair from chasing each other.
 *
 * Resume positions are written on a timer as well as at the end, because a phone's way of leaving a
 * film is to be taken away by a phone call, not to press Stop.
 */
class VodTuner(
    private val context: Context,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
    private val profileDao: ProfileDao,
    private val sourceDao: SourceDao,
    private val historyDao: HistoryDao,
    private val progressDao: ProgressDao,
    private val settings: SettingsRepository,
    private val streamUrlResolver: StreamUrlResolver,
    private val externalPlayerLauncher: ExternalPlayerLauncher,
    private val session: PlaybackSession,
    private val liveTuner: LiveTuner,
    private val dataSaver: DataSaverGate,
    val player: OwnTVPlayer,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine by lazy { MpvPlaybackEngine(player) }

    private val _playing = MutableStateFlow<VodPlayback?>(null)

    /** The film or episode on screen, or null when nothing of ours is playing. */
    val playing: StateFlow<VodPlayback?> = _playing

    /** The profile the current stream was started for — a mid-film profile switch must not write its
     *  resume position into the new profile's list. */
    private var playingProfileId = -1L

    init {
        scope.launch {
            liveTuner.channel.collect { if (it != null) clearPlaying() }
        }
        scope.launch {
            while (true) {
                delay(PROGRESS_INTERVAL_MS)
                saveProgress()
            }
        }
    }

    /**
     * Play a film. False means nothing opened here — it went to an external player, or the item could
     * not be resolved — so the caller must not navigate to the player screen.
     */
    suspend fun playMovie(movieId: Long, startPositionMs: Long = 0): Boolean {
        val movie = withContext(Dispatchers.IO) { movieDao.getById(movieId) } ?: return false
        val pid = currentProfileId() ?: return false
        if (!AdultCategoryClassifier.allows(pid, movie.categoryId, profileDao, categoryDao)) return false
        val source = withContext(Dispatchers.IO) { sourceDao.getById(movie.sourceId) }

        // #115 — a protected item cannot go to an external player: no intent extra carries a licence
        // URL, so the other app would open it and fail on the first segment.
        if (settings.externalPlayerMovies.first() && movie.drmConfig == null) {
            return handOver(movie.name, source, movie.streamUrl, movie.httpHeaders, pid, MediaType.MOVIE, movie.id)
        }

        if (!dataSaver.allowsStreaming()) return false
        val url = resolve(source, movie.streamUrl) ?: return false
        saveProgress()
        liveTuner.stop()
        player.play(
            url = url,
            title = movie.name,
            year = movie.year?.toString(),
            isLive = false,
            startPositionMs = startPositionMs,
            userAgent = source?.userAgent,
            httpHeaders = movie.httpHeaders,
            drmConfig = movie.drmConfig,
            contentKey = enginePinKey(movie.sourceId, "MOVIE", movie.remoteId),
            reconnectProvider = reconnectFor(source, movie.streamUrl),
        )
        began(pid, VodPlayback(MediaType.MOVIE, movie.id, movie.name, posterUrl = movie.posterUrl))
        return true
    }

    /** [playMovie] for one episode of [show]. */
    suspend fun playEpisode(episodeId: Long, startPositionMs: Long = 0): Boolean {
        val episode = withContext(Dispatchers.IO) { seriesDao.getEpisodeById(episodeId) } ?: return false
        val show = withContext(Dispatchers.IO) { seriesDao.getSeriesById(episode.seriesId) } ?: return false
        val pid = currentProfileId() ?: return false
        if (!AdultCategoryClassifier.allows(pid, show.categoryId, profileDao, categoryDao)) return false
        val source = withContext(Dispatchers.IO) { sourceDao.getById(show.sourceId) }
        val title = show.name
        val subtitle = episodeLabel(episode)

        if (settings.externalPlayerSeries.first() && episode.drmConfig == null) {
            return handOver(title, source, episode.streamUrl, episode.httpHeaders, pid, MediaType.EPISODE, episode.id)
        }

        if (!dataSaver.allowsStreaming()) return false
        val url = resolve(source, episode.streamUrl) ?: return false
        saveProgress()
        liveTuner.stop()
        player.play(
            url = url,
            title = title,
            subtitle = subtitle,
            isLive = false,
            startPositionMs = startPositionMs,
            userAgent = source?.userAgent,
            httpHeaders = episode.httpHeaders,
            drmConfig = episode.drmConfig,
            contentKey = enginePinKey(show.sourceId, "EPISODE", episode.remoteId),
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            reconnectProvider = reconnectFor(source, episode.streamUrl),
        )
        began(pid, VodPlayback(MediaType.EPISODE, episode.id, title, subtitle, show.posterUrl))
        return true
    }

    /**
     * Play a file that is already on this phone.
     *
     * Offline is the whole point of a download, so nothing here asks the provider anything: no URL to
     * resolve, no headers, no reconnect. The resume position is still the film's own, so one started
     * over the network carries on from where it stopped.
     */
    suspend fun playDownload(
        mediaType: MediaType,
        itemId: Long,
        filePath: String,
        title: String,
        posterUrl: String?,
    ): Boolean {
        val pid = currentProfileId() ?: return false
        if (!downloadAllowed(mediaType, itemId, pid)) return false
        if (settings.externalPlayerFor(mediaType).first()) {
            externalPlayerLauncher.launch(filePath, title)
            return false
        }
        saveProgress()
        liveTuner.stop()
        val resume = withContext(Dispatchers.IO) { progressDao.get(pid, mediaType, itemId) }
        player.play(
            url = filePath,
            title = title,
            isLive = false,
            startPositionMs = resume?.positionMs ?: 0L,
        )
        began(pid, VodPlayback(mediaType, itemId, title, posterUrl = posterUrl))
        return true
    }

    /**
     * A kids profile must not reach an adult title just because its file is already on disk. A
     * download whose playlist has since been deleted cannot be classified at all, so a kids profile
     * is refused it — the same call the television makes.
     */
    private suspend fun downloadAllowed(mediaType: MediaType, itemId: Long, profileId: Long): Boolean {
        val item = withContext(Dispatchers.IO) {
            when (mediaType) {
                MediaType.MOVIE -> movieDao.getById(itemId)?.categoryId to true
                else -> seriesDao.getEpisodeById(itemId)
                    ?.let { seriesDao.getSeriesById(it.seriesId) }
                    ?.let { it.categoryId to true } ?: (null to false)
            }
        }
        if (mediaType == MediaType.MOVIE && item.first == null && !item.second) return false
        return AdultCategoryClassifier.allows(profileId, item.first, profileDao, categoryDao)
    }

    /** Hand the item to VLC, MX Player or whatever else is installed — the long-press action, and the
     *  "External player" setting's route. History is still recorded; a resume position cannot be. */
    suspend fun playExternal(movie: MovieEntity): Boolean {
        val pid = currentProfileId() ?: return false
        if (!AdultCategoryClassifier.allows(pid, movie.categoryId, profileDao, categoryDao)) return false
        val source = withContext(Dispatchers.IO) { sourceDao.getById(movie.sourceId) }
        return handOver(movie.name, source, movie.streamUrl, movie.httpHeaders, pid, MediaType.MOVIE, movie.id)
    }

    /** [playExternal] for an episode of [show]. */
    suspend fun playExternal(show: SeriesEntity, episode: EpisodeEntity): Boolean {
        val pid = currentProfileId() ?: return false
        if (!AdultCategoryClassifier.allows(pid, show.categoryId, profileDao, categoryDao)) return false
        val source = withContext(Dispatchers.IO) { sourceDao.getById(show.sourceId) }
        return handOver(show.name, source, episode.streamUrl, episode.httpHeaders, pid, MediaType.EPISODE, episode.id)
    }

    /** Stop playing altogether — the mini player's close button, and nothing else. */
    fun stop() {
        scope.launch {
            saveProgress()
            clearPlaying()
            session.attach(null)
            PlaybackService.stop(context)
            player.stop()
        }
    }

    /** Write the resume position now — on the timer, when another item starts, and when the user stops. */
    suspend fun saveProgress() {
        val current = _playing.value ?: return
        val position = player.position.value
        val duration = player.duration.value
        if (position <= 0 || duration <= 0) return
        if (currentProfileId() != playingProfileId) return
        runCatching {
            withContext(Dispatchers.IO) {
                progressDao.save(
                    PlaybackProgressEntity(
                        profileId = playingProfileId,
                        mediaType = current.mediaType,
                        itemId = current.itemId,
                        positionMs = position,
                        durationMs = duration,
                    ),
                )
            }
        }
    }

    private suspend fun began(profileId: Long, what: VodPlayback) {
        session.attach(engine)
        PlaybackService.start(context)
        playingProfileId = profileId
        _playing.value = what
        runCatching {
            withContext(Dispatchers.IO) {
                historyDao.record(
                    WatchHistoryEntity(profileId = profileId, mediaType = what.mediaType, itemId = what.itemId),
                )
            }
        }
    }

    private fun clearPlaying() {
        _playing.value = null
        playingProfileId = -1L
    }

    private suspend fun handOver(
        title: String,
        source: SourceEntity?,
        streamUrl: String,
        httpHeaders: String?,
        profileId: Long,
        mediaType: MediaType,
        itemId: Long,
    ): Boolean {
        val url = resolve(source, streamUrl) ?: return false
        externalPlayerLauncher.launch(
            url = url,
            title = title,
            userAgent = source?.userAgent,
            httpHeaders = httpHeaders,
        )
        runCatching {
            withContext(Dispatchers.IO) {
                historyDao.record(WatchHistoryEntity(profileId = profileId, mediaType = mediaType, itemId = itemId))
            }
        }
        return false
    }

    /** Stalker mints a play URL per open; the stored "URL" is a portal command until then. */
    private suspend fun resolve(source: SourceEntity?, streamUrl: String): String? =
        if (streamUrlResolver.needsResolve(source)) {
            runCatching { streamUrlResolver.resolve(source!!, streamUrl, vod = true) }.getOrNull()
        } else {
            streamUrl
        }

    /** A Stalker link dies before a long film ends; give the player a way to mint a fresh one. Null
     *  for M3U and Xtream, which also clears whatever the previous item left on the player. */
    private fun reconnectFor(source: SourceEntity?, streamUrl: String): ReconnectUrlProvider? =
        if (streamUrlResolver.needsResolve(source)) {
            ReconnectUrlProvider {
                runCatching { streamUrlResolver.resolve(source!!, streamUrl, vod = true) }.getOrNull()
            }
        } else {
            null
        }

    private suspend fun currentProfileId(): Long? {
        val preferred = settings.activeProfileId.first()
        return if (preferred >= 0) profileDao.resolveExistingProfileId(preferred) else null
    }

    /** "S2 · E4 Title" — core's wording, so the player's second line reads as it does on the TV. */
    private fun episodeLabel(episode: EpisodeEntity): String =
        if (episode.name.isBlank()) {
            context.getString(R.string.content_season_episode, episode.seasonNumber, episode.episodeNumber)
        } else {
            context.getString(
                R.string.content_season_episode_title,
                episode.seasonNumber,
                episode.episodeNumber,
                episode.name,
            )
        }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 5_000L
    }
}
