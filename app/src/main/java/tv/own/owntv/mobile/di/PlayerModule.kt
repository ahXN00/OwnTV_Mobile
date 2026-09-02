package tv.own.owntv.mobile.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import tv.own.owntv.mobile.playback.PipController
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.PlaybackSession
import tv.own.owntv.player.PlayerDiagnostics

/**
 * The libmpv player, bound app-side.
 *
 * `:player-core` deliberately ships no Koin module of its own — it is a plain library, and the app
 * that hosts it decides how many engines it wants and when they are created. The TV app's
 * `playerModule` binds five (full player, live preview, hero preview, session, subtitles); this one
 * binds the two that a mobile app cannot do without, and will grow as the real screens land.
 *
 * Every constructor argument resolves out of core's own `dataModule`, so nothing else has to be
 * declared here. Named arguments because nine consecutive `get()` calls depend silently on parameter
 * ORDER: Koin resolves by type, so two same-typed dependencies could swap without a compile error.
 */
val playerModule = module {
    // Tails own-process logcat for MediaCodec/AudioTrack errors the engine can't expose.
    single { PlayerDiagnostics() }
    // Shared between the activity (which enters PiP) and the player screen (which knows whether the
    // picture is on screen at all).
    single { PipController() }
    // Audio focus and the system media session. Both arguments differ from the television's defaults
    // for the same reason: this is a phone. A call PAUSES the film rather than playing it quietly
    // under the caller, and unplugging headphones stops it instead of switching to the loudspeaker.
    single {
        PlaybackSession(
            context = androidContext(),
            focusPolicy = PlaybackSession.FocusPolicy.PAUSE,
            pauseWhenOutputDisconnects = true,
        )
    }
    single {
        OwnTVPlayer(
            context = androidContext(),
            settings = get(),
            connectivity = get(),
            streamingHttp = get(),
            diagnostics = get(),
            proxyHolder = get(),
            vodEngineStore = get(),
            localeStore = get(),
            playbackPrefs = get(),
        )
    }
}
