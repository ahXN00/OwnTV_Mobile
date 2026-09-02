package tv.own.owntv.mobile.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.own.owntv.core.backup.BackupManager
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.ProfileEntity
import tv.own.owntv.core.repository.SourceRepository
import tv.own.owntv.core.sync.SyncResult
import tv.own.owntv.player.OwnTVPlayer

/**
 * State of the Plan 3 Phase 3 harness. Everything here is throwaway: the point is to prove core's
 * database, sync path and player run unchanged on a phone, not to be the shape of any real screen.
 */
data class DevState(
    val status: String = "idle",
    val profileId: Long? = null,
    val sourceIds: List<Long> = emptyList(),
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val channels: List<ChannelEntity> = emptyList(),
    val busy: Boolean = false,
    val playing: ChannelEntity? = null,
)

/** Channels the list shows. The catalogs run to six figures; this screen is not a catalog browser. */
private const val CHANNEL_LIST_LIMIT = 500

class DevHarnessViewModel(
    private val profileDao: ProfileDao,
    private val sourceDao: SourceDao,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val sourceRepository: SourceRepository,
    private val backupManager: BackupManager,
    val player: OwnTVPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow(DevState())
    val state: StateFlow<DevState> = _state.asStateFlow()

    /** Source rows by id — only so a played channel can carry its playlist's User-Agent, which some
     *  providers reject the stream without. */
    private var sourcesById: Map<Long, tv.own.owntv.core.database.entity.SourceEntity> = emptyMap()

    init {
        // Opening the database IS the first thing this phase has to prove, so do it on the first
        // frame rather than behind a button: if core's Room setup, its migrations or its schema-drift
        // heal can't run on a phone, the app says so before anything is typed.
        viewModelScope.launch { refresh("opened database") }
    }

    /** Reads the profile, its sources and the raw row counts straight out of core's DAOs. */
    private suspend fun refresh(status: String) {
        val profile = withContext(Dispatchers.IO) { profileDao.getAllOnce().firstOrNull() }
        val sources = withContext(Dispatchers.IO) {
            profile?.let { sourceDao.sourceIdsForProfile(it.id) }.orEmpty()
        }
        sourcesById = withContext(Dispatchers.IO) { sourceDao.getAllOnce().associateBy { it.id } }
        val (channels, movies, series) = withContext(Dispatchers.IO) {
            Triple(
                sources.sumOf { channelDao.countForSourceOnce(it) },
                sources.sumOf { movieDao.countForSourceOnce(it) },
                sources.sumOf { seriesDao.countForSourceOnce(it) },
            )
        }
        val list = withContext(Dispatchers.IO) {
            if (sources.isEmpty()) emptyList() else channelDao.allForSources(sources, CHANNEL_LIST_LIMIT)
        }
        _state.update {
            it.copy(
                status = status,
                profileId = profile?.id,
                sourceIds = sources,
                channelCount = channels,
                movieCount = movies,
                seriesCount = series,
                channels = list,
                busy = false,
            )
        }
    }

    /** The app has no profile UI yet, and core hangs every source off a profile. Make one silently. */
    private suspend fun profileId(): Long = _state.value.profileId
        ?: withContext(Dispatchers.IO) {
            profileDao.insert(ProfileEntity(name = "Dev", avatarColor = 0, avatarId = 0))
        }.also { id -> _state.update { it.copy(profileId = id) } }

    fun addM3u(url: String) = importing { profileId ->
        sourceRepository.addM3uSource(profileId = profileId, name = "M3U", url = url.trim())
    }

    fun addXtream(server: String, username: String, password: String) = importing { profileId ->
        sourceRepository.addXtreamSource(
            profileId = profileId,
            name = "Xtream",
            serverUrl = server.trim(),
            username = username.trim(),
            password = password.trim(),
        )
    }

    /** Create the source, then run core's real sync with its real progress callback. */
    private fun importing(create: suspend (Long) -> tv.own.owntv.core.database.entity.SourceEntity) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, status = "starting…") }
        viewModelScope.launch {
            val result = runCatching {
                val source = create(profileId())
                sourceRepository.sync(source) { stage ->
                    _state.update {
                        it.copy(
                            status = "live ${stage.liveProcessed} · movies ${stage.moviesProcessed} · " +
                                "series ${stage.seriesProcessed}",
                        )
                    }
                }
            }
            val status = result.fold(
                onSuccess = { sync ->
                    when (sync) {
                        is SyncResult.Success ->
                            "sync ok" + if (sync.warnings.isEmpty()) "" else " (${sync.warnings.size} warnings)"
                        is SyncResult.Failed -> "sync failed: ${sync.message}"
                        SyncResult.Cancelled -> "sync cancelled"
                    }
                },
                onFailure = { "threw: ${it::class.simpleName}: ${it.message}" },
            )
            refresh(status)
        }
    }

    /**
     * Restores a backup exported by the **TV app** — the other half of Phase 3 step 2, and the first
     * exercise of the cacheDir bridge Plan 4 Phase 11 relies on: the picked document is already a
     * real [java.io.File] copy by the time it gets here, because core's [BackupManager] opens the
     * file more than once (probe, inspect, read) and a one-shot content stream cannot serve that.
     */
    fun restore(file: java.io.File, password: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, status = "restoring…") }
        viewModelScope.launch {
            val pass = password.takeIf { it.isNotBlank() }
            val sealed = withContext(Dispatchers.IO) { backupManager.isSealed(file) }
            val inspection = backupManager.sectionsIn(file, pass)
            val status = inspection.fold(
                onSuccess = { found ->
                    backupManager.import(file, backupPassword = pass).fold(
                        onSuccess = { "restored ${it.items} items · sealed=$sealed · ${found.sections}" },
                        onFailure = { "import failed: ${it::class.simpleName}: ${it.message}" },
                    )
                },
                onFailure = { "inspect failed: ${it::class.simpleName}: ${it.message}" },
            )
            file.delete()
            refresh(status)
        }
    }

    fun reload() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch { refresh("reloaded") }
    }

    fun play(channel: ChannelEntity) {
        _state.update { it.copy(playing = channel) }
        player.play(
            url = channel.streamUrl,
            title = channel.name,
            isLive = true,
            userAgent = sourcesById[channel.sourceId]?.userAgent,
            httpHeaders = channel.httpHeaders,
        )
    }

    fun stop() {
        player.stop()
        _state.update { it.copy(playing = null) }
    }

    override fun onCleared() {
        // stop, not release: the engine is a singleton, and since Plan 4 Phase 3 the real Live TV
        // screen uses the same instance — releasing it here would leave that screen a dead player.
        player.stop()
        super.onCleared()
    }
}
