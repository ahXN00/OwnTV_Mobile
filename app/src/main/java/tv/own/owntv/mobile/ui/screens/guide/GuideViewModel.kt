package tv.own.owntv.mobile.ui.screens.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.customize.applyCustomizationsWithCustoms
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.ContentOrderDao
import tv.own.owntv.core.database.dao.CustomCategoryDao
import tv.own.owntv.core.database.dao.FavoriteDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.database.entity.FavoriteEntity
import tv.own.owntv.core.live.GuideReader
import tv.own.owntv.core.live.GuideSlot
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.core.live.livePagingSource
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.repository.ActiveProfileSources
import tv.own.owntv.core.repository.activeProfileSources
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.ui.screens.live.LiveCategory
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/** The stretch of time the guide is showing, on the clock the user reads. */
data class GuideWindow(val start: Long, val end: Long)

/**
 * The Guide, for a screen you hold.
 *
 * The data is the television's, down to the query: the same channels in the same order, hidden the
 * same way, shifted by the same per-channel offsets. What differs is how much of it is asked for at
 * once. A television grid loads a rolling window of every channel because a remote can fling through
 * hundreds of rows; a phone shows one day at a time and reads a row's programmes as that row comes
 * into view, so opening the Guide costs one query rather than the lineup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuideViewModel(
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val customCategoryDao: CustomCategoryDao,
    private val contentOrderDao: ContentOrderDao,
    private val favoriteDao: FavoriteDao,
    private val profileDao: ProfileDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val customize: CustomizationStore,
    private val guide: GuideReader,
    private val tuner: LiveTuner,
) : ViewModel() {

    private val ctx: StateFlow<ActiveProfileSources> = activeProfileSources(settings, sourceDao)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveProfileSources(-1L, emptyList()))

    private val custom: StateFlow<SectionCustomizations> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0) flowOf(SectionCustomizations())
            else customize.observe(c.profileId, MediaType.LIVE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SectionCustomizations())

    private val epgOffset: StateFlow<Int> = settings.epgOffsetMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val sortMode: StateFlow<SettingsRepository.SortMode> = settings.sortLive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.SortMode.PLAYLIST)

    private val liveCategories = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0) flowOf(emptyList())
            else categoryDao.observe(c.liveSourceIds, MediaType.LIVE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Categories this profile does not see — hidden ones, and every adult one for a kids profile. */
    private val hiddenCategoryIds: StateFlow<Set<Long>> =
        combine(liveCategories, custom, ctx.flatMapLatest { profileDao.observeById(it.profileId) }) { cats, cust, profile ->
            AdultCategoryClassifier.hiddenCategoryIds(cats, cust.hiddenCategories, profile?.isKids == true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val folderContextKeys: StateFlow<Map<Long, String>> = liveCategories
        .map { cats -> cats.associateBy({ it.id }, { CustomizeKeys.category(it) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val orderedContexts: StateFlow<Set<String>> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0) flowOf(emptyList())
            else contentOrderDao.observeContextKeys(c.profileId, MediaType.LIVE)
        }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** All channels, favourites, then the profile's folders — the Guide filters the same lists Live
     *  does, minus History and Catch-up, which are about what was watched rather than what is on. */
    val categories: StateFlow<List<LiveCategory>> = combine(
        liveCategories,
        custom,
        sortMode,
        ctx.flatMapLatest { profileDao.observeById(it.profileId) },
    ) { cats, cust, sort, profile ->
        val kids = profile?.isKids == true
        val visibleCats = if (kids) cats.filterNot { AdultCategoryClassifier.isAdult(it.name) } else cats
        val visibleCustoms =
            if (kids) cust.customCategories.filterNot { AdultCategoryClassifier.isAdult(it.name) }
            else cust.customCategories
        val folders = visibleCats.applyCustomizationsWithCustoms(
            cust,
            visibleCustoms,
            alphaRest = sort == SettingsRepository.SortMode.ALPHA,
        )
        buildList {
            add(LiveCategory(LiveKey.All, builtIn = LiveCategory.BuiltIn.ALL))
            add(LiveCategory(LiveKey.Favorites, builtIn = LiveCategory.BuiltIn.FAVORITES))
            folders.forEach { e ->
                add(
                    LiveCategory(
                        key = e.categoryId?.let { LiveKey.Folder(it) } ?: LiveKey.Custom(e.customId!!),
                        title = e.displayName,
                    ),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selected = MutableStateFlow<LiveKey>(LiveKey.All)
    val selected: StateFlow<LiveKey> = _selected

    fun select(key: LiveKey) { _selected.value = key }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun setQuery(q: String) { _query.value = q }

    /** Which day is on screen: 0 is today, 1 tomorrow, and so on to the end of what the guide holds. */
    private val _day = MutableStateFlow(0)
    val day: StateFlow<Int> = _day

    fun selectDay(offset: Int) {
        if (_day.value == offset) return
        _day.value = offset
        rowCache.clear()
    }

    /**
     * The window on screen. Today starts at the last half hour rather than at midnight — a guide
     * that opens on this morning's programmes is a guide the user has to scroll before it is useful.
     */
    val window: StateFlow<GuideWindow> = _day
        .map { offset ->
            val cal = Calendar.getInstance()
            if (offset == 0) {
                cal.set(Calendar.MINUTE, if (cal.get(Calendar.MINUTE) < 30) 0 else 30)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            } else {
                cal.add(Calendar.DAY_OF_YEAR, offset)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            GuideWindow(start, cal.timeInMillis)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GuideWindow(0, 0))

    /** Grid, on-now list or one channel's schedule. Null until the user picks: the screen then
     *  decides from its own width, which is the only thing that knows whether a grid fits. */
    val viewMode: StateFlow<SettingsRepository.GuideView?> = settings.guideView
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setViewMode(view: SettingsRepository.GuideView) {
        viewModelScope.launch { settings.setGuideView(view) }
    }

    val densityPct: StateFlow<Int> = settings.guideDensityPct
        .stateIn(viewModelScope, SharingStarted.Eagerly, 100)

    fun setDensityPct(pct: Int) {
        viewModelScope.launch { settings.setGuideDensityPct(pct) }
    }

    val favoriteIds: StateFlow<Set<Long>> = ctx
        .flatMapLatest { favoriteDao.observeFavoriteIds(it.profileId, MediaType.LIVE) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private data class Args(val key: LiveKey, val ctx: ActiveProfileSources, val cust: SectionCustomizations, val hidden: Set<Long>, val query: String)

    /** The rows of the guide: the profile's channels, in its own order, filtered by chip and search. */
    val channels: Flow<PagingData<ChannelEntity>> =
        combine(_selected, ctx, custom, hiddenCategoryIds, _query.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(), ::Args)
            .combine(orderedContexts) { args, _ -> args }
            .flatMapLatest { (key, c, cust, hidden, q) ->
                if (c.profileId < 0) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE / 2)) {
                        livePagingSource(
                            key = key,
                            profileId = c.profileId,
                            sourceIds = c.liveSourceIds,
                            query = q,
                            sort = sortMode.value,
                            channelDao = channelDao,
                            customCategoryDao = customCategoryDao,
                            contextKey = { folderContextKeys.value[it] },
                            hasManualOrder = { it in orderedContexts.value },
                        )
                    }.flow.map { paging -> paging.applyCustomizations(key, cust, hidden) }
                }
            }
            .cachedIn(viewModelScope)

    /** Hidden channels and folders drop out and renames apply — the same rule as the Live list. */
    private fun PagingData<ChannelEntity>.applyCustomizations(
        key: LiveKey,
        cust: SectionCustomizations,
        hidden: Set<Long>,
    ): PagingData<ChannelEntity> {
        if (cust.hiddenItems.isEmpty() && cust.itemNames.isEmpty() && hidden.isEmpty() && cust.movedFromOrigin.isEmpty()) return this
        return filter { ch ->
            CustomizeKeys.channel(ch) !in cust.hiddenItems &&
                (ch.categoryId == null || ch.categoryId !in hidden) &&
                (cust.movedFromOrigin[CustomizeKeys.channel(ch)]?.let { origin ->
                    key !is LiveKey.Folder || origin != folderContextKeys.value[key.id]
                } ?: true)
        }.map { ch -> cust.itemNames[CustomizeKeys.channel(ch)]?.let { ch.copy(name = it) } ?: ch }
    }

    // One day of one channel, kept while that day is on screen so scrolling back up is instant.
    private val rowCache = ConcurrentHashMap<Long, List<EpgProgrammeEntity>>()
    private var sourceIds: List<Long> = emptyList()

    /** Read straight from the cache, so a row scrolled back into view draws without a blank frame. */
    fun cachedRow(channelId: Long): List<EpgProgrammeEntity>? = rowCache[channelId]

    /** This channel's programmes for the selected day, read as the row comes into view. */
    suspend fun row(channel: ChannelEntity): List<EpgProgrammeEntity> {
        rowCache[channel.id]?.let { return it }
        val w = window.value
        if (sourceIds.isEmpty()) sourceIds = guide.guideSourceIds()
        val rows = guide.row(channel, custom.value, epgOffset.value, sourceIds, w.start, w.end)
        rowCache[channel.id] = rows
        return rows
    }

    private val _onNow = MutableStateFlow<Map<Long, GuideSlot>>(emptyMap())

    /** What is on now, and next, for the rows the "On now" list can see. */
    val onNow: StateFlow<Map<Long, GuideSlot>> = _onNow

    /** Fill [onNow] for the rows on screen, in one query — channels already answered are skipped. */
    fun loadOnNow(visible: List<ChannelEntity>) {
        val missing = visible.filter { it.id !in _onNow.value }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            if (sourceIds.isEmpty()) sourceIds = guide.guideSourceIds()
            val found = guide.onNow(
                channels = missing,
                cust = custom.value,
                globalShiftMinutes = epgOffset.value,
                sourceIds = sourceIds,
                atMs = System.currentTimeMillis(),
                lookAheadMs = ON_NOW_LOOK_AHEAD_MS,
            )
            // Channels with no guide are recorded as empty, or every scroll re-asks for them.
            _onNow.value = _onNow.value + missing.associate { it.id to (found[it.id] ?: GuideSlot(null, null)) }
        }
    }

    /** The synopsis, fetched when a programme is opened — the list queries leave it out. */
    suspend fun description(programmeId: Long): String? = guide.description(programmeId)

    /** Whether this programme can be replayed: an archive channel, already aired, still in window. */
    fun canCatchup(channel: ChannelEntity, programme: EpgProgrammeEntity): Boolean {
        if (!channel.catchup) return false
        val now = System.currentTimeMillis()
        if (programme.startMs > now) return false
        val days = channel.catchupDays.takeIf { it > 0 } ?: tv.own.owntv.core.live.DEFAULT_CATCHUP_DAYS
        return programme.startMs >= now - days * DAY_MS
    }

    /** Watch the channel itself — the same tuner the Live screen and the mini player are showing. */
    fun watch(channel: ChannelEntity) = tuner.switchTo(channel)

    /** Replay a programme from the provider's archive. */
    fun playCatchup(channel: ChannelEntity, programme: EpgProgrammeEntity) =
        tuner.playCatchup(programme, channel)

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            if (channel.id in favoriteIds.value) favoriteDao.remove(pid, MediaType.LIVE, channel.id)
            else favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = MediaType.LIVE, itemId = channel.id))
        }
    }

    private companion object {
        const val PAGE_SIZE = 40
        const val SEARCH_DEBOUNCE_MS = 300L
        const val ON_NOW_LOOK_AHEAD_MS = 6L * 60 * 60 * 1000
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
