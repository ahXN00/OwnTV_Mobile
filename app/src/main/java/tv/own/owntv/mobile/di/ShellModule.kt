package tv.own.owntv.mobile.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import tv.own.owntv.mobile.ui.shell.ShellViewModel

/** The app shell's own bindings. `NavVisibility` comes from core's `dataModule`. */
val shellModule = module {
    viewModelOf(::ShellViewModel)
}
