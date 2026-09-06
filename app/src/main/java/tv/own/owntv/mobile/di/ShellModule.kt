package tv.own.owntv.mobile.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import tv.own.owntv.mobile.ui.screens.ContentActions
import tv.own.owntv.mobile.ui.setup.SetupViewModel
import tv.own.owntv.mobile.ui.shell.ShellViewModel
import tv.own.owntv.mobile.ui.shell.StartupLiveSelection

/** The app shell's own bindings. `NavVisibility` and `SourceImporter` come from core's `dataModule`. */
val shellModule = module {
    // Shared by every screen with a long-press menu, so it is bound where nothing owns it.
    singleOf(::ContentActions)
    // Set by the shell on launch, taken by Live TV when it opens — neither owns the other.
    singleOf(::StartupLiveSelection)
    viewModelOf(::ShellViewModel)
    viewModel { SetupViewModel(get(), get(), get(), androidContext()) }
}
