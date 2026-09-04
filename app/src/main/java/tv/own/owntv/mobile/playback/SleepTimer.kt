package tv.own.owntv.mobile.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stop playing in a while, for a user who is falling asleep to it.
 *
 * A single, because the countdown has to outlive the screen it was set on — the whole point is that
 * the user puts the phone down. Deliberately **not** persisted: a timer that survived a restart
 * would stop a stream someone started hours later for an unrelated reason.
 *
 * The stop is the tuner's, not the player's, so the notification and the mini player go with it
 * rather than being left behind pointing at nothing.
 */
class SleepTimer(private val stopPlayback: () -> Unit) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow<Long?>(null)

    /** Milliseconds left, or null when no timer is running. */
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    /** Start (or replace) the countdown. A non-positive duration stops playback at once. */
    fun start(durationMs: Long) {
        job?.cancel()
        if (durationMs <= 0L) {
            _remainingMs.value = null
            stopPlayback()
            return
        }
        _remainingMs.value = durationMs
        job = scope.launch {
            var left = durationMs
            while (left > 0L) {
                delay(minOf(left, TICK_MS))
                left -= TICK_MS
                _remainingMs.value = left.coerceAtLeast(0L)
            }
            _remainingMs.value = null
            stopPlayback()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
    }

    private companion object {
        /** A second: the label counts down in minutes, and a coarser tick makes the last one lie. */
        const val TICK_MS = 1_000L
    }
}
