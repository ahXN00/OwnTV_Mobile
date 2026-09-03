package tv.own.owntv.mobile.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import tv.own.owntv.mobile.ui.screens.settings.SettingsViewModel

/**
 * The one view model behind all nine settings pages. Everything it takes is core's already — the
 * settings themselves, the DAOs the Data and App pages read, and the shared HTTP client the proxy
 * test borrows so it inherits the app's own TLS rather than inventing a second stack.
 */
val settingsModule = module {
    viewModel {
        SettingsViewModel(
            context = androidContext(),
            settings = get(),
            sourceDao = get(),
            sourceRepository = get(),
            profileDao = get(),
            historyDao = get(),
            progressDao = get(),
            epgSourceStore = get(),
            catalogSync = get(),
            epgSync = get(),
            categoryDao = get(),
            channelDao = get(),
            customize = get(),
            okHttpClient = get(),
            vodEngineStore = get(),
            playbackPrefs = get(),
            metadataProvider = get(),
            metadataBudget = get(),
        )
    }
}
