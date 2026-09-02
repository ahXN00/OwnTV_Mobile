package tv.own.owntv.mobile.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.nav.MainSection
import tv.own.owntv.core.nav.NavVisibility

/** Shell state: which destinations the active playlist offers, and the tap that sends a list home. */
class ShellViewModel(navVisibility: NavVisibility, sourceDao: SourceDao) : ViewModel() {

    /**
     * Whether the app has nothing to show yet. Null until the database has answered, so the shell
     * does not flash an empty Home on the way to the setup screen on every cold start.
     */
    val needsSetup: StateFlow<Boolean?> = sourceDao.observeAll()
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Core's rule, unchanged — the same set the TV app's rail is built from. */
    val visibleSections: StateFlow<Set<MainSection>> = navVisibility.visibleSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainSection.allBrowse)

    private val _scrollToTop = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Emits a route whose list should jump back to the top — a long press on its nav item. */
    val scrollToTop: SharedFlow<String> = _scrollToTop.asSharedFlow()

    fun requestScrollToTop(route: String) {
        viewModelScope.launch { _scrollToTop.emit(route) }
    }
}
