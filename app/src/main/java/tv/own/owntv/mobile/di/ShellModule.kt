package tv.own.owntv.mobile.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import tv.own.owntv.mobile.ui.setup.SetupViewModel
import tv.own.owntv.mobile.ui.shell.ShellViewModel

/** The app shell's own bindings. `NavVisibility` and `SourceImporter` come from core's `dataModule`. */
val shellModule = module {
    viewModelOf(::ShellViewModel)
    viewModel { SetupViewModel(get(), get(), get(), androidContext()) }
}
