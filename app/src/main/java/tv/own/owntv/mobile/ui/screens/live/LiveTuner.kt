package tv.own.owntv.mobile.ui.screens.live

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.FavoriteDao
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.HistoryDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.database.entity.FavoriteEntity
import tv.own.owntv.core.database.entity.WatchHistoryEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.live.EpgNowNext
import tv.own.owntv.core.live.LiveArchiveUrls
import tv.own.owntv.core.live.LiveEpgReader
import tv.own.owntv.core.live.LiveTimeshift
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.player.AudioOnlyStore
import tv.own.owntv.core.player.enginePinKey
import tv.own.owntv.core.repository.ActiveProfileSources
import tv.own.owntv.core.repository.activeProfileSources
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.stalker.StreamUrlResolver
import tv.own.owntv.mobile.playback.DataSaverGate
import tv.own.owntv.mobile.playback.PlaybackService
import tv.own.owntv.player.LiveProgramme
import tv.own.owntv.player.MpvPlaybackEngine
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.PlaybackSession

/**
 * What is playing, and everything a screen needs to ask about it.
 *
 * It outlives every screen deliberately. The channel screen, the fullscreen player and the docked
 * mini player are three views of **one** stream, and a user who leaves the channel screen while a
 * match is on has not asked for it to stop — so the tuning state cannot belong to a view model that
 * dies with its route. Only [stop] ends playback.
 *
 * Live rewind is core's [LiveTimeshift], the same class the television uses: this app supplies the
 * archive URL and the play call, exactly as `LiveViewModel` does there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveTuner(
    private val context: Context,
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val historyDao: HistoryDao,
    private val profileDao: ProfileDao,
    private val favoriteDao: FavoriteDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val customize: CustomizationStore,
    private val streamUrlResolver: StreamUrlResolver,
    private val epgReader: LiveEpgReader,
    private val archiveUrls: LiveArchiveUrls,
    private val session: PlaybackSession,
    private val dataSaver: DataSaverGate,
    private val audioOnlyStore: AudioOnlyStore,
    val player: OwnTVPlayer,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * The engine as the rest of the system sees it. Published to [session] whenever a stream starts,
     * withdrawn in [stop] — this class is the only thing that knows whether anything is playing at
     * all, so it is the only thing that can answer a call, a headphone unplug or a lockscreen button
     * correctly.
     */
    private val engine by lazy { MpvPlaybackEngine(player) }

    /**
     * Hand the stream to the system: the session takes the lockscreen and the audio focus, the
     * foreground service keeps the process alive once the app leaves the screen. Both are idempotent,
     * so every `play()` call can go through here.
     */
    private fun publishToSystem() {
        session.attach(engine)
        PlaybackService.start(context)
    }

    private val ctx: StateFlow<ActiveProfileSources> = activeProfileSources(settings, sourceDao)
        .stateIn(scope, SharingStarted.Eagerly, ActiveProfileSources(-1L, emptyList()))

    private val custom: StateFlow<SectionCustomizations> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0) flowOf(SectionCustomizations())
            else customize.observe(c.profileId, MediaType.LIVE)
        }
        .stateIn(scope, SharingStarted.Eagerly, SectionCustomizations())

    private val _channel = MutableStateFlow<ChannelEntity?>(null)

    /** The channel on screen, with the user's own name for it. */
    val channel: StateFlow<ChannelEntity?> = _channel

    private val _nowNext = MutableStateFlow<EpgNowNext?>(null)
    val nowNext: StateFlow<EpgNowNext?> = _nowNext

    private val _replaying = MutableStateFlow(false)

    /**
     * True only while a *chosen* archive programme is playing — the one thing on this tuner that has
     * an end and therefore a seek bar.
     *
     * The player cannot be asked this. A live stream's duration is whatever the provider's rolling
     * window happens to report, which for plenty of them is a plausible-looking twenty-five hours, so
     * deciding live-ness from the duration classed real channels as recordings and hid the entire
     * live panel. This tuner is the thing that knows: it started the stream, and it knew which kind
     * it was asking for.
     *
     * A rewind into the archive from the live edge is deliberately **not** a replay — that is still
     * the channel, just behind, and it keeps the live bar and the way back to now.
     */
    val replaying: StateFlow<Boolean> = _replaying

    private val _timelineProgrammes = MutableStateFlow<List<LiveProgramme>>(emptyList())

    /**
     * The playing channel's guide window, for the player's live timeline: these become the programme
     * boundary ticks on the bar and the name the scrub bubble reads out. Loaded once per channel —
     * scrubbing must never re-query the guide, the whole window is already here.
     */
    val timelineProgrammes: StateFlow<List<LiveProgramme>> = _timelineProgrammes

    /** Whether the channel playing is a favourite — the floating window's menu, which has no list
     *  row behind it to ask. */
    val isFavorite: StateFlow<Boolean> = _channel
        .flatMapLatest { channel ->
            val pid = ctx.value.profileId
            if (channel == null || pid < 0) flowOf(false)
            else favoriteDao.isFavorite(pid, MediaType.LIVE, channel.id)
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleFavorite() {
        val channel = _channel.value ?: return
        val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return
        scope.launch {
            if (isFavorite.value) favoriteDao.remove(pid, MediaType.LIVE, channel.id)
            else favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = MediaType.LIVE, itemId = channel.id))
        }
    }

    private val _siblings = MutableStateFlow<List<ChannelEntity>>(emptyList())

    /** The other channels of the same folder — the Channels tab, and the swipe-up overlay. */
    val siblings: StateFlow<List<ChannelEntity>> = _siblings

    private val timeshift = LiveTimeshift(
        scope = scope,
        playback = object : LiveTimeshift.Playback {
            override val positionMs: Long get() = player.position.value
            override val hasError: Boolean get() = player.error.value != null
            override val hasActiveStream: Boolean get() = player.hasActiveStream
        },
        loadArchive = ::loadArchiveStream,
        onLiveEdge = { goToLive() },
    )

    /** Seconds behind the live edge; null at the edge — what the red bar and the pill read. */
    val offsetSec: StateFlow<Int?> = timeshift.offsetSec

    /** The wall-clock instant actually on screen while an archive plays. */
    val watchingWallMs: StateFlow<Long?> = timeshift.watchingWallMs

    private var loadedId: Long? = null

    /** Open [channelId]: read the row, start it, and fill the guide and the channel list around it. */
    fun tune(channelId: Long) {
        if (loadedId == channelId && player.hasActiveStream) return
        loadedId = channelId
        scope.launch {
            val channel = withContext(Dispatchers.IO) { channelDao.getById(channelId) } ?: return@launch
            start(channel)
            loadSiblings(channel)
        }
    }

    /** Switch to another channel without leaving the screen — the Channels tab and the overlay. */
    fun switchTo(channel: ChannelEntity) {
        if (channel.id == loadedId) return
        loadedId = channel.id
        scope.launch { start(channel) }
    }

    /**
     * The next (+1) or previous (−1) channel of the same folder, wrapping at both ends — Channel +/−
     * from the Picture-in-Picture window, where there is no room for a list.
     */
    fun step(delta: Int) {
        val list = _siblings.value
        if (list.size < 2) return
        val index = list.indexOfFirst { it.id == loadedId }
        if (index < 0) return
        switchTo(list[(index + delta).mod(list.size)])
    }

    private suspend fun start(channel: ChannelEntity) {
        // Before anything is claimed to be tuned: refusing has to leave the screen as it was, and the
        // channel forgotten, so tapping the same row again on Wi-Fi opens it.
        if (!dataSaver.allowsStreaming()) {
            loadedId = null
            return
        }
        timeshift.clear() // a new channel is never still rewound into the old one's archive
        _replaying.value = false
        // A renamed channel keeps its new name on this screen too — the row the user tapped had it.
        val named = custom.value.itemNames[CustomizeKeys.channel(channel)]?.let { channel.copy(name = it) } ?: channel
        _channel.value = named
        _nowNext.value = null
        _timelineProgrammes.value = emptyList()

        val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return
        if (!AdultCategoryClassifier.allows(pid, channel.categoryId, profileDao, categoryDao)) return

        val source = withContext(Dispatchers.IO) { sourceDao.getById(channel.sourceId) }
        // Stalker portals mint a play URL per tune; the stored "URL" is a portal command until then.
        val url = if (streamUrlResolver.needsResolve(source)) {
            runCatching { streamUrlResolver.resolve(source!!, channel.streamUrl) }.getOrNull() ?: return
        } else {
            channel.streamUrl
        }
        player.play(
            url = url,
            title = named.name,
            logoUrl = named.displayLogoUrl,
            isLive = true,
            userAgent = source?.userAgent,
            httpHeaders = channel.httpHeaders,
        )
        publishToSystem()
        applyAudioOnlyDefault(channel)
        recordHistory(pid, channel.id)
        _nowNext.value = epgReader.nowNext(channel, custom.value, settings.epgOffsetMinutes.first())
        _timelineProgrammes.value = catchupProgrammes()
            .map { LiveProgramme(it.startMs, it.stopMs, it.title) }
    }

    /**
     * Start this channel without a picture when the user has already said so — either for this
     * channel in particular, or for mobile data in general.
     *
     * Read once, after the stream opens: dropping the video track is something the engine does to a
     * stream it already has, and asking before there is one would have nothing to act on.
     */
    private suspend fun applyAudioOnlyDefault(channel: ChannelEntity) {
        val remembered = settings.audioPerChannelNow() && audioOnlyStore.isAudioOnly(audioOnlyKey(channel))
        val onData = settings.audioOnMobileDataNow() && dataSaver.isMetered()
        // Both ways, every time. Turning the picture off is a decision about *this* channel, and the
        // engine keeps the flag across a retune — so without the else, one tap on Sound only silently
        // became every channel afterwards, looking for all the world like a setting that remembered.
        if (remembered || onData) player.enterAudioOnly() else player.exitAudioOnly()
    }

    /**
     * Turn the picture off or back on, and remember the choice for this channel when the user asked
     * for it to be remembered. The player alone would forget it the moment the channel changed.
     */
    fun setAudioOnly(audioOnly: Boolean) {
        if (audioOnly) player.enterAudioOnly() else player.exitAudioOnly()
        val channel = _channel.value ?: return
        scope.launch {
            if (settings.audioPerChannelNow()) audioOnlyStore.set(audioOnlyKey(channel), audioOnly)
        }
    }

    /** The stable per-item key, with the stream URL as the fallback the engine stores also use. */
    private fun audioOnlyKey(channel: ChannelEntity): String =
        enginePinKey(channel.sourceId, MediaType.LIVE.name, channel.remoteId) ?: channel.streamUrl

    private suspend fun loadSiblings(channel: ChannelEntity) {
        val c = ctx.value
        if (c.profileId < 0) return
        val category = channel.categoryId?.let { withContext(Dispatchers.IO) { categoryDao.getById(it) } }
        val list = withContext(Dispatchers.IO) {
            if (category != null) {
                channelDao.snapshotByCategoryManual(
                    categoryId = category.id,
                    profileId = c.profileId,
                    contextKey = CustomizeKeys.category(category),
                    limit = SIBLING_LIMIT,
                )
            } else {
                // No category of its own: fall back to the profile's whole list, in provider order.
                channelDao.snapshotAll(c.liveSourceIds.ifEmpty { listOf(-1L) }, SIBLING_LIMIT)
            }
        }
        val cust = custom.value
        _siblings.value = list
            .filter { CustomizeKeys.channel(it) !in cust.hiddenItems }
            .map { ch -> cust.itemNames[CustomizeKeys.channel(ch)]?.let { ch.copy(name = it) } ?: ch }
    }

    /** Already-aired programmes this channel's archive still holds, newest first. */
    suspend fun catchupProgrammes(): List<EpgProgrammeEntity> {
        val channel = _channel.value ?: return emptyList()
        return epgReader.catchupProgrammes(
            channel,
            custom.value,
            settings.epgOffsetMinutes.first(),
            ctx.value.sourceIds,
        )
    }

    /**
     * Replay a past programme from the archive. Seekable, so it plays as VOD rather than as live.
     *
     * [on] is the channel it aired on, for the Guide, where a programme is picked without tuning its
     * channel first — starting the live stream only to abandon it a second later would cost the user
     * a connection and the provider a session. Omitted, it is the channel already playing.
     */
    fun playCatchup(programme: EpgProgrammeEntity, on: ChannelEntity? = null) {
        val channel = on ?: _channel.value ?: return
        if (channel.id != loadedId) {
            loadedId = channel.id
            _channel.value = channel
            _nowNext.value = null
            _timelineProgrammes.value = emptyList()
        }
        scope.launch {
            if (!dataSaver.allowsStreaming()) return@launch
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            if (!AdultCategoryClassifier.allows(pid, channel.categoryId, profileDao, categoryDao)) return@launch
            val url = archiveUrls.forProgramme(channel, programme) ?: return@launch
            val source = withContext(Dispatchers.IO) { sourceDao.getById(channel.sourceId) }
            // isArchive: providers cut archive segments mid-GOP, and the engine needs to tolerate it.
            _replaying.value = true
            player.play(
                url = url,
                title = channel.name,
                subtitle = programme.title,
                logoUrl = channel.displayLogoUrl,
                isLive = false,
                isArchive = true,
                userAgent = source?.userAgent,
                httpHeaders = channel.httpHeaders,
            )
            publishToSystem()
            recordHistory(pid, channel.id)
            // The clock over a replay says yesterday 13:00, not now — same as on the television.
            timeshift.followArchiveFrom(programme.startMs)
        }
    }

    /**
     * "Go back to…": start the archive [offsetSec] seconds behind live in one jump.
     *
     * Not a replay — this is still the channel, just behind, so the live bar and the way back to now
     * stay. That is why it goes through the timeshift rather than through [playCatchup].
     */
    fun jumpBackTo(offsetSec: Int) {
        val ch = _channel.value?.takeIf { it.catchup } ?: return
        _replaying.value = false
        timeshift.beginAt(ch, offsetSec)
    }

    /** Offsets worth offering in the catch-up sheet, nearest first; empty without an archive. */
    fun jumpOptions(): List<Int> = _channel.value?.let { timeshift.jumpOptions(it) } ?: emptyList()

    /**
     * Tune the channel carrying provider number [number] — the numeric entry in the channel sheet.
     *
     * The current playlist's own channels first, then the profile's other Live playlists, because a
     * number is the provider's and two providers routinely disagree about who is channel 101. Two
     * visible channels with the same number and no way to choose between them is [DirectTune.Ambiguous]
     * rather than a silent guess.
     */
    suspend fun tuneByNumber(number: Int): DirectTune {
        val sourceIds = ctx.value.liveSourceIds.ifEmpty { return DirectTune.NotFound }
        val playing = _channel.value
        return runCatching {
            val hidden = custom.value.hiddenItems
            val ordered = if (playing == null) sourceIds else {
                listOf(playing.sourceId) + sourceIds.filter { it != playing.sourceId }
            }
            // Stage by source, so the playing playlist's own 101 wins over another playlist's 101
            // instead of the two of them cancelling each other out as an ambiguity.
            for (sourceId in ordered) {
                val hits = withContext(Dispatchers.IO) { channelDao.findByNumber(listOf(sourceId), number) }
                    .filter { CustomizeKeys.channel(it) !in hidden }
                when (hits.size) {
                    0 -> continue
                    1 -> {
                        switchTo(hits.first())
                        return DirectTune.Found(hits.first().name)
                    }
                    else -> return DirectTune.Ambiguous(hits.size)
                }
            }
            DirectTune.NotFound
        }.getOrElse { DirectTune.Failed }
    }

    /** Drag back into the archive (+) or toward live (−), in seconds. */
    fun scrubLive(deltaSec: Int) {
        val ch = _channel.value ?: return
        timeshift.scrub(ch, deltaSec)
    }

    /** How deep this channel's archive goes, in seconds — the length of the rewind bar. */
    fun archiveWindowSec(): Int = _channel.value?.let { timeshift.windowSec(it) } ?: 0

    /** Back to the real-time edge, off the archive stream. */
    fun goToLive() {
        timeshift.clear()
        _channel.value?.let { ch -> scope.launch { start(ch) } }
    }

    /**
     * Turn one point in the archive into a playing stream — the "URL out" half of [LiveTimeshift].
     * False when no archive URL can be built, or the user reached live while it was being resolved.
     */
    private suspend fun loadArchiveStream(ch: ChannelEntity, startMs: Long, offsetSec: Int): Boolean {
        val tz = withContext(Dispatchers.IO) { settings.resolveCatchupTimeZone() }
        val (url, sourceUa) = withContext(Dispatchers.IO) {
            val source = sourceDao.getById(ch.sourceId) ?: return@withContext null
            archiveUrls.forTimeshift(ch, source, startMs, offsetSec, tz)?.let { it to source.userAgent }
        } ?: return false
        if (timeshift.offsetSec.value == null) return false // user jumped back to live meanwhile
        player.play(
            url = url,
            title = ch.name,
            logoUrl = ch.displayLogoUrl,
            isArchive = true,
            userAgent = sourceUa,
            httpHeaders = ch.httpHeaders,
            rewindStartMs = startMs,
        )
        publishToSystem()
        return true
    }

    /** Stop playing altogether — the mini player's swipe-down, and nothing else. */
    fun stop() {
        timeshift.clear()
        loadedId = null
        _channel.value = null
        _nowNext.value = null
        _timelineProgrammes.value = emptyList()
        // Withdraw first: a session left published after the sound stops keeps answering the
        // lockscreen and the headphone button for a stream that no longer exists.
        session.attach(null)
        PlaybackService.stop(context)
        player.stop()
    }

    private suspend fun recordHistory(profileId: Long, channelId: Long) {
        runCatching {
            withContext(Dispatchers.IO) {
                historyDao.record(
                    WatchHistoryEntity(profileId = profileId, mediaType = MediaType.LIVE, itemId = channelId),
                )
            }
        }
    }

    private companion object {
        const val SIBLING_LIMIT = 2_000
    }
}

/**
 * What typing a channel number produced. Every outcome is something the user is told: a number that
 * matches nothing must not look like a tap that was simply ignored.
 */
sealed interface DirectTune {
    data class Found(val name: String) : DirectTune
    data object NotFound : DirectTune
    data class Ambiguous(val count: Int) : DirectTune
    data object Failed : DirectTune
}
