package tv.own.owntv.mobile.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.HistoryDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.ProgressDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.CategoryEntity
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.ProfileEntity
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.epg.EpgSource
import tv.own.owntv.core.epg.EpgSourceStore
import tv.own.owntv.core.metadata.MetadataBudget
import tv.own.owntv.core.metadata.MetadataBudgetStatus
import tv.own.owntv.core.metadata.MetadataConfig
import tv.own.owntv.core.metadata.MetadataProvider
import tv.own.owntv.core.metadata.profileAllowsAdultMetadata
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.player.PlaybackPrefsStore
import tv.own.owntv.core.player.VodEngineStore
import tv.own.owntv.core.repository.SourceRepository
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.settings.StartupChannelRef
import tv.own.owntv.core.settings.StartupMode
import tv.own.owntv.core.storage.StorageAccess
import tv.own.owntv.core.sync.work.CatalogSyncScheduler
import tv.own.owntv.core.sync.work.CatalogSyncState
import tv.own.owntv.core.sync.work.EpgSyncScheduler

/**
 * The one view model behind every settings page.
 *
 * It deliberately exposes [settings] itself rather than mirroring two hundred preferences as
 * properties: each row collects the flow it displays and calls the setter it owns, so adding a row
 * costs one line here instead of three. Everything a row cannot do on its own — a resync, a deleted
 * source, a cleared history — is a function below, because those need a scope that outlives the row.
 */
class SettingsViewModel(
    private val context: Context,
    val settings: SettingsRepository,
    private val sourceDao: SourceDao,
    private val sourceRepository: SourceRepository,
    private val profileDao: ProfileDao,
    private val historyDao: HistoryDao,
    private val progressDao: ProgressDao,
    private val epgSourceStore: EpgSourceStore,
    private val catalogSync: CatalogSyncScheduler,
    private val epgSync: EpgSyncScheduler,
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
    private val customize: CustomizationStore,
    private val okHttpClient: OkHttpClient,
    private val vodEngineStore: VodEngineStore,
    private val playbackPrefs: PlaybackPrefsStore,
    private val metadataProvider: MetadataProvider,
    private val metadataBudget: MetadataBudget,
) : ViewModel() {

    /** Run a setter on a scope that survives the row being scrolled off the screen. */
    fun edit(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settings.block() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sources: StateFlow<List<SourceEntity>> = settings.activeProfileId
        .flatMapLatest { profileId ->
            if (profileId < 0) sourceDao.observeAll() else sourceDao.observeForProfile(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profiles: StateFlow<List<ProfileEntity>> = profileDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val epgSources: StateFlow<List<EpgSource>> = epgSourceStore.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncState(sourceId: Long): Flow<CatalogSyncState> = catalogSync.observeSync(sourceId)

    /**
     * Fetch the catalogue again. [removeMissing] is the destructive half of the TV app's two resync
     * choices: it lets the run prune titles the provider has stopped listing, and it is never the
     * default.
     */
    fun resync(source: SourceEntity, removeMissing: Boolean = false) {
        catalogSync.enqueueSync(source.id, reason = "manual", forcePrune = removeMissing)
    }

    fun deleteSource(source: SourceEntity) {
        viewModelScope.launch { sourceRepository.deleteSource(source) }
    }

    /** Add an XMLTV feed and fetch it straight away — an unsynced guide source shows nothing. */
    fun addEpg(name: String, url: String, userAgent: String?) {
        viewModelScope.launch {
            val added = epgSourceStore.add(name, url, userAgent?.takeIf { it.isNotBlank() })
            epgSync.enqueueSync(added.id, reason = "manual")
        }
    }

    fun syncEpg(source: EpgSource) {
        epgSync.enqueueSync(source.id, reason = "manual")
    }

    fun removeEpg(source: EpgSource) {
        viewModelScope.launch { epgSourceStore.remove(source.id) }
    }

    /** Clear what the user has watched. Null clears everything; a type clears just that section. */
    fun clearHistory(type: MediaType? = null) {
        viewModelScope.launch {
            val profileId = settings.activeProfileId.first()
            if (profileId < 0) return@launch
            if (type == null) {
                historyDao.clear(profileId)
                progressDao.clearProfile(profileId)
            } else {
                historyDao.clearType(profileId, type)
                // Continue-watching comes from the resume table, not from history, and an episode's
                // progress is stored under EPISODE. Live has no resume position to clear.
                when (type) {
                    MediaType.MOVIE -> progressDao.clearProfileType(profileId, MediaType.MOVIE)
                    MediaType.SERIES -> progressDao.clearProfileType(profileId, MediaType.EPISODE)
                    else -> Unit
                }
            }
        }
    }

    /**
     * Every folder a section has, hidden ones included — the customize page is where a hidden folder
     * is brought back, so it cannot browse the same filtered list the section itself does.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun categories(type: MediaType): Flow<List<CategoryEntity>> = sources.flatMapLatest { list ->
        if (list.isEmpty()) flowOf(emptyList()) else categoryDao.observe(list.map { it.id }, type)
    }

    fun customizations(type: MediaType): Flow<SectionCustomizations> = settings.activeProfileId
        .flatMapLatest { pid ->
            if (pid < 0) flowOf(SectionCustomizations()) else customize.observe(pid, type)
        }

    /** Run one of [CustomizationStore]'s edits against whoever is watching. */
    fun customizeEdit(type: MediaType, block: suspend CustomizationStore.(Long, MediaType) -> Unit) {
        viewModelScope.launch {
            val pid = settings.activeProfileId.first()
            if (pid >= 0) customize.block(pid, type)
        }
    }

    /** The volumes a download can be written to without asking for a file permission. */
    suspend fun downloadVolumes(): List<StorageAccess.StorageRoot> =
        withContext(Dispatchers.IO) { StorageAccess.appRoots(context) }

    // --- Per-item playback choices the player remembered, and the rows that forget them ---

    /**
     * How many movies and episodes are pinned to a specific engine, and how many individual items
     * have a remembered zoom, volume or A/V-sync offset.
     *
     * Counted separately because they are forgotten separately: wanting every film back at the
     * default aspect is not a request to lose the levels set on the quiet ones.
     */
    val vodEnginePinCount: StateFlow<Int> =
        combine(vodEngineStore.mpvUrls, vodEngineStore.exoUrls) { mpv, exo -> mpv.size + exo.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val savedZoomCount: StateFlow<Int> = playbackPrefs.observeZoomCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val savedVolumeCount: StateFlow<Int> = playbackPrefs.observeVolumeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val savedAudioDelayCount: StateFlow<Int> = playbackPrefs.observeAudioDelayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun clearVodEnginePins() { viewModelScope.launch { vodEngineStore.clearAll() } }

    fun clearSavedZoom() { viewModelScope.launch { playbackPrefs.clearZoom() } }

    fun clearSavedVolume() { viewModelScope.launch { playbackPrefs.clearVolume() } }

    fun clearSavedAudioDelay() { viewModelScope.launch { playbackPrefs.clearAudioDelay() } }

    // --- Per-playlist overrides of the global Live TV settings ---

    /** `null` follows the global "Live TV player". */
    fun setSourceLiveEngine(sourceId: Long, preference: String?) {
        viewModelScope.launch { sourceDao.updateLiveEnginePreference(sourceId, preference) }
    }

    /** `null` mode follows the global "Live latency"; [customSecs] only matters for Custom. */
    fun setSourceLiveLatency(sourceId: Long, mode: String?, customSecs: Int) {
        viewModelScope.launch { sourceDao.updateLiveLatency(sourceId, mode, customSecs) }
    }

    /** `-1` follows the global "Pre-buffer". */
    fun setSourcePreroll(sourceId: Long, secs: Int) {
        viewModelScope.launch { sourceDao.updateLivePreroll(sourceId, secs) }
    }

    // --- Metadata: which tier is answering, what is left of the allowance, and a lookup to prove it ---

    val metadataTier: StateFlow<MetadataConfig.Tier> = settings.metadataConfigFlow
        .map { it.tier }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetadataConfig.Tier.DEFAULT_WORKER)

    /**
     * What is left of this install's allowance. Only meaningful on the shared default Worker — an own
     * key or a self-hosted server is the user's own resource and is never metered, so the page hides
     * this on those two rather than showing a limit that does not exist.
     */
    private val _metadataBudgetStatus = MutableStateFlow<MetadataBudgetStatus?>(null)
    val metadataBudgetStatus: StateFlow<MetadataBudgetStatus?> = _metadataBudgetStatus.asStateFlow()

    fun refreshMetadataBudget() {
        viewModelScope.launch {
            _metadataBudgetStatus.value = runCatching { metadataBudget.status() }.getOrNull()
        }
    }

    sealed interface MetadataTestState {
        data object Idle : MetadataTestState
        data object Testing : MetadataTestState
        data class Ok(val title: String, val year: Int?, val tmdbId: Int) : MetadataTestState
        data class Fail(val failure: MetadataFailure) : MetadataTestState
    }

    sealed interface MetadataFailure {
        data object EmptyTitle : MetadataFailure
        data object ServerUnavailable : MetadataFailure
        data class NoMatch(val query: String) : MetadataFailure
        data class Unknown(val rawMessage: String?) : MetadataFailure
    }

    private val _metadataTest = MutableStateFlow<MetadataTestState>(MetadataTestState.Idle)
    val metadataTest: StateFlow<MetadataTestState> = _metadataTest.asStateFlow()

    /**
     * Look one title up through whichever tier is configured. This is the only way to find out that a
     * key or a server address is wrong without waiting for posters to quietly stop appearing.
     */
    fun testMetadataLookup(title: String) {
        if (_metadataTest.value == MetadataTestState.Testing) return
        val q = title.trim()
        if (q.isEmpty()) {
            _metadataTest.value = MetadataTestState.Fail(MetadataFailure.EmptyTitle)
            return
        }
        _metadataTest.value = MetadataTestState.Testing
        viewModelScope.launch {
            val profileId = settings.activeProfileId.first()
            val includeAdult = profileAllowsAdultMetadata(profileDao.getById(profileId)?.isKids)
            val result = runCatching { metadataProvider.searchMovie(q, includeAdult = includeAdult) }
            _metadataTest.value = result.fold(
                onSuccess = { hits ->
                    val top = hits?.firstOrNull()
                    when {
                        hits == null -> MetadataTestState.Fail(MetadataFailure.ServerUnavailable)
                        top == null -> MetadataTestState.Fail(MetadataFailure.NoMatch(q))
                        else -> MetadataTestState.Ok(top.title, top.year, top.tmdbId)
                    }
                },
                onFailure = {
                    MetadataTestState.Fail(MetadataFailure.Unknown(it.message?.takeIf { m -> m.isNotBlank() }))
                },
            )
        }
    }

    // --- Proxy and DNS, saved as a form and testable before it is saved ---

    fun saveProxy(enabled: Boolean, host: String, port: Int, username: String, password: String) {
        viewModelScope.launch { settings.saveProxy(enabled, host, port, username, password) }
    }

    fun saveDns(enabled: Boolean, host: String, port: Int, dohUrl: String) {
        viewModelScope.launch { settings.saveDns(enabled, host, port, dohUrl) }
    }

    private val _proxyTest = MutableStateFlow<NetworkTestState>(NetworkTestState.Idle)
    val proxyTest: StateFlow<NetworkTestState> = _proxyTest.asStateFlow()

    private val _dnsTest = MutableStateFlow<NetworkTestState>(NetworkTestState.Idle)
    val dnsTest: StateFlow<NetworkTestState> = _dnsTest.asStateFlow()

    /**
     * Try the typed proxy, not the saved one — the point is to find out whether it works *before*
     * committing it, so a wrong port cannot take the whole app offline.
     */
    fun testProxy(host: String, port: Int, username: String, password: String) {
        if (_proxyTest.value == NetworkTestState.Testing) return
        _proxyTest.value = NetworkTestState.Testing
        viewModelScope.launch {
            _proxyTest.value = probeProxy(okHttpClient, host, port, username, password)
        }
    }

    fun testDns(host: String, port: Int, dohUrl: String) {
        if (_dnsTest.value == NetworkTestState.Testing) return
        _dnsTest.value = NetworkTestState.Testing
        viewModelScope.launch { _dnsTest.value = probeDns(host, port, dohUrl) }
    }

    // --- App startup, which is stored per profile ---

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val startupMode: StateFlow<StartupMode> = settings.activeProfileId
        .flatMapLatest { pid -> if (pid < 0) flowOf(StartupMode.HOME) else settings.startupMode(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartupMode.HOME)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val startupChannel: StateFlow<StartupChannelRef?> = settings.activeProfileId
        .flatMapLatest { pid -> if (pid < 0) flowOf(null) else settings.startupChannel(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setStartupMode(mode: StartupMode) {
        viewModelScope.launch {
            val pid = settings.activeProfileId.first()
            if (pid >= 0) settings.setStartupMode(pid, mode)
        }
    }

    fun setStartupChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            val pid = settings.activeProfileId.first()
            if (pid < 0) return@launch
            settings.setSpecificStartupChannel(
                pid,
                StartupChannelRef(channel.sourceId, channel.remoteId, channel.name, channel.id),
            )
        }
    }

    /** Names matching [query], bounded — the startup picker is a search box, not the whole playlist. */
    suspend fun searchChannels(query: String): List<ChannelEntity> {
        val ids = sources.value.filter { it.syncLive }.map { it.id }
        if (ids.isEmpty()) return emptyList()
        return channelDao.searchList(query.trim(), ids, limit = 60)
    }
}
