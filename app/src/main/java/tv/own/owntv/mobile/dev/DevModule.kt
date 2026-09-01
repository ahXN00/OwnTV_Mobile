package tv.own.owntv.mobile.dev

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Bindings for the Plan 3 Phase 3 harness. Deleted whole once Plan 4's real screens replace it. */
val devModule = module {
    viewModelOf(::DevHarnessViewModel)
}
