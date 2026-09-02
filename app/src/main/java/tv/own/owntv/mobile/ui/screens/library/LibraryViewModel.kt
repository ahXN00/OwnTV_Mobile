package tv.own.owntv.mobile.ui.screens.library

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.content.movieCountFlow
import tv.own.owntv.core.content.moviePagingSource
import tv.own.owntv.core.content.seriesCountFlow
import tv.own.owntv.core.content.seriesPagingSource
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.customize.applyCustomizationsWithCustoms
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.ContentOrderDao
import tv.own.owntv.core.database.dao.CustomCategoryDao
import tv.own.owntv.core.database.dao.FavoriteDao
import tv.own.owntv.core.database.dao.HistoryDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.ProgressDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.FavoriteEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.PlaybackProgressEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.download.DownloadManager
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.core.live.parseLiveKey
import tv.own.owntv.core.live.serialize
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.repository.ActiveProfileSources
import tv.own.owntv.core.repository.activeProfileSources
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.storage.StorageAccess

/** Which half of the library is on screen. */
enum class LibraryTab { MOVIES, SERIES }

/** One chip in the strip. [builtIn] labels are translated; the rest are provider or user names. */
data class VodCategory(val key: LiveKey, val title: String? = null, val builtIn: BuiltIn? = null) {
    enum class BuiltIn { ALL, FAVORITES, HISTORY }
}

/** A poster in the grid. Films and shows differ in almost nothing the grid draws, so they share one. */
data class VodItem(
    val id: Long,
    val name: String,
    val posterUrl: String?,
    val year: Int?,
    val rating: Double?,
)

/**
 * Movies and Series for the phone.
 *
 * One view model for both, because they are the same screen twice: the same rail selection type, the
 * same queries in core's [moviePagingSource] / [seriesPagingSource], the same chips, and the same
 * long-press actions. What differs is a table name.
 *
 * Nothing here queries until a tab is actually collected — a catalogue of 170,000 films must not cost
 * anything at launch, and the count under the chips is the only whole-list question ever asked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
    private val customCategoryDao: CustomCategoryDao,
    private val contentOrderDao: ContentOrderDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val progressDao: ProgressDao,
    private val profileDao: ProfileDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val customize: CustomizationStore,
    private val downloadManager: DownloadManager,
    private val vodTuner: VodTuner,
) : ViewModel() {

    private val _tab = MutableStateFlow(LibraryTab.MOVIES)
    val tab: StateFlow<LibraryTab> = _tab

    private val _selectedMovies = MutableStateFlow<LiveKey>(LiveKey.All)
    private val _selectedSeries = MutableStateFlow<LiveKey>(LiveKey.All)

    /** The chip the current tab is on. */
    val selected: StateFlow<LiveKey> =
        combine(_tab, _selectedMovies, _selectedSeries) { tab, movies, series ->
            if (tab == LibraryTab.MOVIES) movies else series
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveKey.All)

    private val ctx: StateFlow<ActiveProfileSources> = activeProfileSources(settings, sourceDao)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveProfileSources(-1L, emptyList()))

    private val mediaType: StateFlow<MediaType> = _tab
        .map { if (it == LibraryTab.MOVIES) MediaType.MOVIE else MediaType.SERIES }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaType.MOVIE)

    private val custom: StateFlow<SectionCustomizations> = combine(ctx, mediaType) { c, type -> c to type }
        .flatMapLatest { (c, type) ->
            if (c.profileId < 0) flowOf(SectionCustomizations()) else customize.observe(c.profileId, type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SectionCustomizations())

    val sortMode: StateFlow<SettingsRepository.SortMode> = _tab
        .flatMapLatest { if (it == LibraryTab.MOVIES) settings.sortMovies else settings.sortSeries }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.SortMode.PLAYLIST)

    val viewMode: StateFlow<SettingsRepository.VodViewMode> = settings.vodViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.VodViewMode.GRID)

    /** How many posters a row holds. 0 means "decide from the width of the screen". */
    val gridColumns: StateFlow<Int> = settings.vodGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val vodCategories = combine(ctx, mediaType) { c, type -> c to type }
        .flatMapLatest { (c, type) ->
            if (c.profileId < 0) flowOf(emptyList()) else categoryDao.observe(c.sourceIdsFor(type), type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Categories this profile does not see — its hidden ones, plus every adult category on a kids
     *  profile. Resolved to ids so All and History drop those items too, as the TV app does. */
    private val hiddenCategoryIds: StateFlow<Set<Long>> = combine(
        vodCategories,
        custom,
        ctx.flatMapLatest { profileDao.observeById(it.profileId) },
    ) { cats, cust, profile ->
        AdultCategoryClassifier.hiddenCategoryIds(cats, cust.hiddenCategories, profile?.isKids == true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val folderContextKeys: StateFlow<Map<Long, String>> = vodCategories
        .map { cats -> cats.associateBy({ it.id }, { CustomizeKeys.category(it) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Contexts that actually carry a manual order — core's query takes the cheap path for the rest,
     *  and the pager is rebuilt when a folder gains or loses one. */
    private val orderedContexts: StateFlow<Set<String>> = combine(ctx, mediaType) { c, type -> c to type }
        .flatMapLatest { (c, type) ->
            if (c.profileId < 0) flowOf(emptyList()) else contentOrderDao.observeContextKeys(c.profileId, type)
        }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** The chip strip: All, Favorites, History, then the profile's own arranged folders. */
    val categories: StateFlow<List<VodCategory>> = combine(
        vodCategories,
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
            add(VodCategory(LiveKey.All, builtIn = VodCategory.BuiltIn.ALL))
            add(VodCategory(LiveKey.Favorites, builtIn = VodCategory.BuiltIn.FAVORITES))
            add(VodCategory(LiveKey.History, builtIn = VodCategory.BuiltIn.HISTORY))
            folders.forEach { e ->
                add(
                    VodCategory(
                        key = e.categoryId?.let { LiveKey.Folder(it) } ?: LiveKey.Custom(e.customId!!),
                        title = e.displayName,
                    ),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** How many items the current selection holds — the one whole-list question the screen asks. */
    val count: StateFlow<Int> = combine(mediaType, selected, ctx, hiddenCategoryIds, ::Counted)
        .flatMapLatest { (type, key, c, hidden) ->
            when {
                c.profileId < 0 -> flowOf(0)
                type == MediaType.MOVIE ->
                    movieCountFlow(key, c.profileId, c.sourceIdsFor(type), hidden, movieDao, customCategoryDao)
                else ->
                    seriesCountFlow(key, c.profileId, c.sourceIdsFor(type), hidden, seriesDao, customCategoryDao)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private data class Counted(
        val type: MediaType,
        val key: LiveKey,
        val ctx: ActiveProfileSources,
        val hidden: Set<Long>,
    )

    private data class Args(
        val key: LiveKey,
        val ctx: ActiveProfileSources,
        val cust: SectionCustomizations,
        val hidden: Set<Long>,
    )

    /** The films of the current selection. Collected only while the Movies tab is on screen. */
    val movies: Flow<PagingData<VodItem>> =
        combine(_selectedMovies, ctx, custom, hiddenCategoryIds, ::Args)
            .combine(orderedContexts) { args, _ -> args }
            .flatMapLatest { (key, c, cust, hidden) ->
                if (c.profileId < 0) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(pagingConfig()) {
                        moviePagingSource(
                            key = key,
                            profileId = c.profileId,
                            sourceIds = c.sourceIdsFor(MediaType.MOVIE),
                            query = "",
                            sort = sortMode.value,
                            movieDao = movieDao,
                            customCategoryDao = customCategoryDao,
                            contextKey = { folderContextKeys.value[it] },
                            hasManualOrder = { it in orderedContexts.value },
                        )
                    }.flow.map { paging ->
                        paging.customized(key, cust, hidden, { CustomizeKeys.movie(it) }, { it.categoryId })
                            .map { m -> m.asItem(cust) }
                    }
                }
            }
            .cachedIn(viewModelScope)

    /** [movies] for shows. */
    val series: Flow<PagingData<VodItem>> =
        combine(_selectedSeries, ctx, custom, hiddenCategoryIds, ::Args)
            .combine(orderedContexts) { args, _ -> args }
            .flatMapLatest { (key, c, cust, hidden) ->
                if (c.profileId < 0) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(pagingConfig()) {
                        seriesPagingSource(
                            key = key,
                            profileId = c.profileId,
                            sourceIds = c.sourceIdsFor(MediaType.SERIES),
                            query = "",
                            sort = sortMode.value,
                            seriesDao = seriesDao,
                            customCategoryDao = customCategoryDao,
                            contextKey = { folderContextKeys.value[it] },
                            hasManualOrder = { it in orderedContexts.value },
                        )
                    }.flow.map { paging ->
                        paging.customized(key, cust, hidden, { CustomizeKeys.series(it) }, { it.categoryId })
                            .map { s -> s.asItem(cust) }
                    }
                }
            }
            .cachedIn(viewModelScope)

    /**
     * Hidden items and hidden categories drop out, and an item moved into a custom category leaves
     * the folder it came from while staying in All.
     *
     * Applied on each fresh `PagingData` inside the pager chain: Paging forbids transforming one the
     * UI has already collected, so a customization change re-creates the pager instead.
     */
    private fun <T : Any> PagingData<T>.customized(
        key: LiveKey,
        cust: SectionCustomizations,
        hidden: Set<Long>,
        itemKey: (T) -> String,
        categoryOf: (T) -> Long?,
    ): PagingData<T> {
        if (cust.hiddenItems.isEmpty() && hidden.isEmpty() && cust.movedFromOrigin.isEmpty()) return this
        return filter { item ->
            itemKey(item) !in cust.hiddenItems &&
                (categoryOf(item) == null || categoryOf(item) !in hidden) &&
                (cust.movedFromOrigin[itemKey(item)]?.let { origin ->
                    key !is LiveKey.Folder || origin != folderContextKeys.value[key.id]
                } ?: true)
        }
    }

    private fun MovieEntity.asItem(cust: SectionCustomizations) = VodItem(
        id = id,
        name = cust.itemNames[CustomizeKeys.movie(this)] ?: name,
        posterUrl = posterUrl,
        year = year,
        rating = rating,
    )

    private fun SeriesEntity.asItem(cust: SectionCustomizations) = VodItem(
        id = id,
        name = cust.itemNames[CustomizeKeys.series(this)] ?: name,
        posterUrl = posterUrl,
        year = year,
        rating = rating,
    )

    val favoriteIds: StateFlow<Set<Long>> = combine(ctx, mediaType) { c, type -> c to type }
        .flatMapLatest { (c, type) -> favoriteDao.observeFavoriteIds(c.profileId, type) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Resume positions for films, keyed by film id — the bar along the bottom of a poster. Only a
     *  started film has a row, so this is a handful of entries and not the catalogue. */
    val movieProgress: StateFlow<Map<Long, PlaybackProgressEntity>> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0) flowOf(emptyList()) else progressDao.observeMovieProgress(c.profileId)
        }
        .map { list -> list.associateBy { it.itemId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun select(tab: LibraryTab) {
        _tab.value = tab
    }

    fun select(key: LiveKey) {
        val current = if (_tab.value == LibraryTab.MOVIES) _selectedMovies else _selectedSeries
        if (current.value == key) return
        current.value = key
        val movies = _tab.value == LibraryTab.MOVIES
        viewModelScope.launch {
            if (movies) settings.setLastMoviesCategory(key.serialize())
            else settings.setLastSeriesCategory(key.serialize())
        }
    }

    fun setSort(mode: SettingsRepository.SortMode) {
        viewModelScope.launch {
            if (_tab.value == LibraryTab.MOVIES) settings.setSortMovies(mode) else settings.setSortSeries(mode)
        }
    }

    fun setViewMode(mode: SettingsRepository.VodViewMode) {
        viewModelScope.launch { settings.setVodViewMode(mode) }
    }

    /** The pinch gesture, and the only writer of the column count. 0 hands it back to the screen. */
    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settings.setVodGridColumns(columns) }
    }

    init {
        viewModelScope.launch {
            if (settings.rememberCategoryMovies.first()) {
                parseLiveKey(settings.lastMoviesCategory.first())?.let { _selectedMovies.value = it }
            }
            if (settings.rememberCategorySeries.first()) {
                parseLiveKey(settings.lastSeriesCategory.first())?.let { _selectedSeries.value = it }
            }
        }
    }

    // --- The long-press actions ------------------------------------------------------------------

    fun toggleFavorite(itemId: Long) {
        viewModelScope.launch {
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            val type = mediaType.value
            if (itemId in favoriteIds.value) favoriteDao.remove(pid, type, itemId)
            else favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = type, itemId = itemId))
        }
    }

    /** True when the film has been watched to the end (or marked so). */
    fun isWatched(itemId: Long): Boolean = movieProgress.value[itemId]?.let {
        it.durationMs > 0 && it.positionMs >= (it.durationMs * WATCHED_FRACTION).toLong()
    } == true

    /**
     * Mark a film watched without playing it. The 1 ms / 1 ms sentinel is the TV app's: it satisfies
     * the "finished" rule while leaving Play starting from the beginning.
     */
    fun setWatched(movieId: Long, watched: Boolean) {
        viewModelScope.launch {
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            if (watched) {
                progressDao.save(
                    PlaybackProgressEntity(
                        profileId = pid,
                        mediaType = MediaType.MOVIE,
                        itemId = movieId,
                        positionMs = 1L,
                        durationMs = 1L,
                    ),
                )
            } else {
                progressDao.clear(pid, MediaType.MOVIE, movieId)
            }
        }
    }

    /** Hide the item from every list (undone in Settings → Customize). */
    fun hide(itemId: Long) {
        viewModelScope.launch {
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            when (mediaType.value) {
                MediaType.MOVIE -> movieDao.getById(itemId)?.let {
                    customize.setItemHidden(pid, MediaType.MOVIE, CustomizeKeys.movie(it), it.name, true)
                }
                else -> seriesDao.getSeriesById(itemId)?.let {
                    customize.setItemHidden(pid, MediaType.SERIES, CustomizeKeys.series(it), it.name, true)
                }
            }
        }
    }

    fun removeFromHistory(itemId: Long) {
        viewModelScope.launch {
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            historyDao.remove(pid, mediaType.value, itemId)
        }
    }

    fun playExternal(itemId: Long) {
        viewModelScope.launch {
            movieDao.getById(itemId)?.let { vodTuner.playExternal(it) }
        }
    }

    /** Download a film, or every episode of a show — the same layout on disk the TV app writes. */
    fun download(itemId: Long) {
        viewModelScope.launch {
            val pid = ctx.value.profileId.takeIf { it >= 0 } ?: return@launch
            if (mediaType.value == MediaType.MOVIE) {
                val movie = movieDao.getById(itemId) ?: return@launch
                if (!AdultCategoryClassifier.allows(pid, movie.categoryId, profileDao, categoryDao)) return@launch
                downloadManager.enqueue(
                    profileId = pid,
                    mediaType = MediaType.MOVIE,
                    itemId = movie.id,
                    title = movie.name,
                    posterUrl = movie.posterUrl,
                    streamUrl = movie.streamUrl,
                    relativeDir = MOVIES_DIR,
                    fileName = "${StorageAccess.sanitize(movie.name)}." +
                        (movie.containerExt ?: StorageAccess.extOf(movie.streamUrl)),
                )
            } else {
                val show = seriesDao.getSeriesById(itemId) ?: return@launch
                if (!AdultCategoryClassifier.allows(pid, show.categoryId, profileDao, categoryDao)) return@launch
                seriesDao.episodesBySeriesOnce(show.id).forEach { episode ->
                    downloadManager.enqueue(
                        profileId = pid,
                        mediaType = MediaType.EPISODE,
                        itemId = episode.id,
                        title = episode.name.takeIf { it.isNotBlank() } ?: show.name,
                        posterUrl = show.posterUrl,
                        streamUrl = episode.streamUrl,
                        relativeDir = episodeDir(show.name, episode.seasonNumber),
                        fileName = episodeFileName(episode.name, episode.episodeNumber, episode.containerExt, episode.streamUrl),
                    )
                }
            }
        }
    }

    private companion object {
        const val WATCHED_FRACTION = 0.95f

        fun pagingConfig() = PagingConfig(
            pageSize = 60,
            prefetchDistance = 30,
            initialLoadSize = 90,
            maxSize = 300,
        )
    }
}

/** Where an episode's file goes — the TV app's layout, so one library serves both apps. */
internal fun episodeDir(showName: String, seasonNumber: Int): String =
    "Series/${StorageAccess.sanitize(showName)}/Season $seasonNumber"

internal fun episodeFileName(name: String, episodeNumber: Int, containerExt: String?, streamUrl: String): String {
    val stem = StorageAccess.sanitize(name.ifBlank { "episode-$episodeNumber" })
    return "$stem.${containerExt ?: StorageAccess.extOf(streamUrl)}"
}

/** Where films go on disk, matching the TV app so one library serves both. */
internal const val MOVIES_DIR = "Movies"
