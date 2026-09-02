package tv.own.owntv.mobile.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.settings.PlaylistRefresh
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.setup.SourceImporter
import tv.own.owntv.core.sync.SyncScopeChoice
import tv.own.owntv.mobile.R
import java.io.File

/**
 * Adding a playlist, or restoring a backup, on the phone.
 *
 * The sequence itself — add, sync, finalize, and undo it all when it fails — is core's
 * [SourceImporter], shared with the television. What is this app's own is the job the import runs in
 * and the profile it attaches to: on a phone that is already set up there is an active profile to
 * add to, and only a first run creates one.
 */
class SetupViewModel(
    private val importer: SourceImporter,
    private val profileDao: ProfileDao,
    private val settings: SettingsRepository,
    private val context: Context,
) : ViewModel() {

    val state = importer.state
    val progress = importer.progress

    private var importJob: Job? = null

    fun startXtream(
        name: String,
        server: String,
        username: String,
        password: String,
        userAgent: String,
        autoRefresh: PlaylistRefresh,
        live: SyncScopeChoice,
        movies: SyncScopeChoice,
        series: SyncScopeChoice,
        preferHls: Boolean,
    ) = runImport {
        importer.xtream(
            name = name, server = server, username = username, password = password,
            userAgent = userAgent, autoRefresh = autoRefresh,
            live = live, movies = movies, series = series, preferHls = preferHls,
        )
    }

    fun startM3u(name: String, url: String, userAgent: String, autoRefresh: PlaylistRefresh) =
        runImport { importer.m3u(name = name, url = url, userAgent = userAgent, autoRefresh = autoRefresh) }

    fun startStalker(
        name: String,
        portalUrl: String,
        mac: String,
        serialNumber: String,
        deviceId: String,
        deviceId2: String,
        signature: String,
        userAgent: String,
        autoRefresh: PlaylistRefresh,
        live: SyncScopeChoice,
        movies: SyncScopeChoice,
        series: SyncScopeChoice,
    ) = runImport {
        importer.stalker(
            name = name, portalUrl = portalUrl, mac = mac, serialNumber = serialNumber,
            deviceId = deviceId, deviceId2 = deviceId2, signature = signature, userAgent = userAgent,
            autoRefresh = autoRefresh, live = live, movies = movies, series = series,
        )
    }

    /** Restore everything from a backup file; an encrypted one asks for its password first. */
    fun importBackup(file: File) {
        importJob?.cancel()
        importJob = viewModelScope.launch { importer.importBackup(file) }
    }

    fun restoreWithPassword(file: File, password: String?) {
        importJob?.cancel()
        importJob = viewModelScope.launch { importer.restoreWithPassword(file, password) }
    }

    /** Ends the flow: the profile the content landed on becomes the active one. */
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            importer.finish()
            onDone()
        }
    }

    fun reset() = importer.reset()

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        importer.reset()
    }

    private fun runImport(block: suspend () -> Unit) {
        importJob?.cancel()
        val job = viewModelScope.launch {
            attachToProfile()
            block()
        }
        importJob = job
        job.invokeOnCompletion { if (importJob == job) importJob = null }
    }

    /**
     * Which profile the new playlist belongs to. An app that has been used before already has one,
     * and adding a second playlist must not quietly create a second profile to hang it on; only a
     * genuinely empty install makes one, named the way the wizard would name it.
     */
    private suspend fun attachToProfile() {
        val existing = settings.activeProfileIdNow().takeIf { it >= 0 }
            ?: profileDao.getAllOnce().firstOrNull()?.id
        if (existing != null) {
            importer.useProfile(existing)
        } else {
            importer.createProfile(
                name = context.getString(R.string.setup_default_profile),
                avatarId = 0,
                isKids = false,
                pin = null,
            )
        }
    }
}
