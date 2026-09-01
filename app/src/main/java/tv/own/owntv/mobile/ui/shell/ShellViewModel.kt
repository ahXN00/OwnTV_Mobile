package tv.own.owntv.mobile.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.own.owntv.core.nav.MainSection
import tv.own.owntv.core.nav.NavVisibility

/** Shell state: which destinations the active playlist offers, and the tap that sends a list home. */
class ShellViewModel(navVisibility: NavVisibility) : ViewModel() {

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
