package tv.own.owntv.mobile.ui.screens.downloads

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.DownloadDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.entity.DownloadEntity
import tv.own.owntv.core.download.DownloadManager
import tv.own.owntv.core.download.DownloadStorageInfo
import tv.own.owntv.core.model.DownloadStatus
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.storage.StorageAccess
import tv.own.owntv.mobile.ui.screens.library.VodTuner
import java.io.File

/**
 * What has been saved for offline watching, and how the queue is getting on.
 *
 * The queue itself is core's — the same worker that feeds the television — so this only reads it,
 * filters out what the profile is not allowed to see, and offers the four buttons a download can
 * need. The one thing computed here is the transfer rate: core records bytes, not speed, so the
 * screen measures it from two readings a second apart.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModel(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
    private val profileDao: ProfileDao,
    private val customize: CustomizationStore,
    private val settings: SettingsRepository,
    private val downloadManager: DownloadManager,
    private val vodTuner: VodTuner,
) : ViewModel() {

    /**
     * The profile's downloads, minus rows whose film or show the user has hidden — hidden is hidden
     * everywhere, Downloads included. The file stays on disk and the row comes back on unhide.
     */
    val downloads: StateFlow<List<DownloadEntity>> = settings.activeProfileId
        .flatMapLatest { pid ->
            if (pid < 0) {
                flowOf(emptyList())
            } else {
                combine(
                    downloadDao.observeForProfile(pid),
                    customize.observe(pid, MediaType.MOVIE),
                    customize.observe(pid, MediaType.SERIES),
                    profileDao.observeById(pid),
                ) { list, custMovie, custSeries, profile ->
                    if (custMovie.hiddenItems.isEmpty() && custSeries.hiddenItems.isEmpty() &&
                        profile?.isKids != true
                    ) {
                        list
                    } else {
                        list.filterNot { isHidden(it, custMovie, custSeries, profile?.isKids == true) }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private suspend fun isHidden(
        d: DownloadEntity,
        custMovie: SectionCustomizations,
        custSeries: SectionCustomizations,
        isKidsProfile: Boolean,
    ): Boolean = when (d.mediaType) {
        MediaType.MOVIE -> movieDao.getById(d.itemId)?.let { movie ->
            CustomizeKeys.movie(movie) in custMovie.hiddenItems ||
                (isKidsProfile && AdultCategoryClassifier.isAdult(movie.categoryId?.let { categoryDao.getById(it)?.name }))
        } ?: isKidsProfile
        MediaType.EPISODE -> seriesDao.getEpisodeById(d.itemId)
            ?.let { ep -> seriesDao.getSeriesById(ep.seriesId) }
            ?.let { series ->
                CustomizeKeys.series(series) in custSeries.hiddenItems ||
                    (isKidsProfile && AdultCategoryClassifier.isAdult(series.categoryId?.let { categoryDao.getById(it)?.name }))
            } ?: isKidsProfile
        else -> false
    }

    /** Free and total space on whichever volume the downloads are being written to. */
    val storage: StateFlow<DownloadStorageInfo?> = downloads
        .mapLatest { downloadManager.storageInfo() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * How fast the queue is moving, in megabits per second, from the growth of the running rows.
     * Zero whenever nothing is running, and after a pause, so a stale figure never lingers.
     */
    val speedMbps: StateFlow<Double> = flow {
        var lastBytes = -1L
        var lastAt = 0L
        while (true) {
            val running = downloads.value.filter { it.status == DownloadStatus.RUNNING }
            val bytes = running.sumOf { it.downloadedBytes }
            val now = System.currentTimeMillis()
            emit(
                if (running.isEmpty() || lastBytes < 0 || now <= lastAt) 0.0
                else (bytes - lastBytes).coerceAtLeast(0L) * 8.0 / (now - lastAt) / 1_000.0,
            )
            lastBytes = if (running.isEmpty()) -1L else bytes
            lastAt = now
            delay(SPEED_SAMPLE_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /**
     * The volumes downloads can go to: this app's own folder on internal storage, and one on every
     * memory card or stick that is plugged in. Not a folder picker — a phone gives an app a folder
     * per volume without asking for any permission, and asking for more than that is not something
     * this app does.
     */
    val volumes: List<StorageAccess.StorageRoot> = StorageAccess.appRoots(context)

    val downloadRoot: StateFlow<String> = settings.downloadRoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setDownloadRoot(path: String) {
        viewModelScope.launch { settings.setDownloadRoot(path) }
    }

    private val _playing = MutableStateFlow(false)
    /** True once a download has actually started playing, so the screen knows to open the player. */
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    fun play(download: DownloadEntity) {
        val path = download.filePath ?: return
        viewModelScope.launch {
            _playing.value = vodTuner.playDownload(
                mediaType = download.mediaType,
                itemId = download.itemId,
                filePath = path,
                title = download.title,
                posterUrl = download.posterUrl,
            )
        }
    }

    fun playerOpened() {
        _playing.value = false
    }

    /**
     * Copy a finished download to wherever the user picked in the system's own save dialog. The app's
     * own folder is invisible to a file manager, so this is how a saved film leaves the app.
     */
    fun saveCopy(download: DownloadEntity, target: Uri) {
        val path = download.filePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(target)?.use { out ->
                    File(path).inputStream().use { it.copyTo(out) }
                }
            }
        }
    }

    fun retry(download: DownloadEntity) = downloadManager.retry(download)
    fun pause(download: DownloadEntity) = downloadManager.pause(download)
    fun resume(download: DownloadEntity) = downloadManager.resume(download)
    fun delete(download: DownloadEntity) = downloadManager.delete(download)

    private companion object {
        const val SPEED_SAMPLE_MS = 1_000L
    }
}
