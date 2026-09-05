package tv.own.owntv.mobile.playback

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The two facts the activity and the player screen have to agree on about Picture-in-Picture.
 *
 * Only the activity can enter PiP, and only the player screen knows whether the picture is what the
 * user is looking at — so the decision needs one small piece of state that outlives neither of them
 * alone. Deliberately two booleans and nothing else: everything else about PiP is already answerable
 * from the player.
 */
class PipController {

    /** The full screen player owns the display. Home pressed now means "keep watching", not "leave". */
    val playerOnScreen = MutableStateFlow(false)

    /** The app is running in the little window — no controls, no gestures, no bars. */
    val inPip = MutableStateFlow(false)

    /**
     * The playback notification was tapped: put the full screen player back on screen.
     *
     * A one-shot, cleared by the shell once it has navigated. It has to travel this way because the
     * two ends cannot reach each other: the notification arrives as an Intent, which only the activity
     * sees, and the player is a navigation destination, which only the shell can reach.
     */
    val openPlayerRequested = MutableStateFlow(false)
}
