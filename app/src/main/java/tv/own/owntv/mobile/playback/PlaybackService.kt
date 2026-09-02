package tv.own.owntv.mobile.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import tv.own.owntv.core.i18n.AppLocale
import tv.own.owntv.core.i18n.LocaleStore
import tv.own.owntv.mobile.MainActivity
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.player.MediaMeta
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.PlaybackSession

/**
 * The notification that keeps playing when the app is not on screen.
 *
 * A phone takes the app off screen constantly — the home button, another app, the lock screen — and
 * without a foreground service the system is entitled to kill the process mid-film. This is what makes
 * background audio legal, and what puts the controls on the lock screen: the notification is hung on
 * core's own [PlaybackSession] token, so the transport buttons, the metadata and the seek bar are the
 * ones the session already publishes rather than a second copy that can disagree with it.
 *
 * Started when a stream starts and stopped when playback ends, both from [LiveTuner] — the one object
 * that knows whether anything is playing at all.
 *
 * The platform's own `Notification.MediaStyle` is used rather than the AndroidX one: it takes the
 * `MediaSession.Token` this app already has, while the compat version needs a `MediaSessionCompat`
 * and a dependency on `androidx.media` to convert one.
 */
class PlaybackService : Service() {

    private val player: OwnTVPlayer by inject()
    private val tuner: LiveTuner by inject()
    private val session: PlaybackSession by inject()
    private val localeStore: LocaleStore by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Artwork is fetched over the network, so it is kept against the URL it came from. */
    private var artUrl: String? = null
    private var art: Bitmap? = null
    private var lastChannelName: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Post once, synchronously, before anything can await a network image: a service that has not
        // called startForeground() within a few seconds of being started is killed outright.
        startForeground(build(player.currentMeta.value, player.isPlaying.value))
        combine(player.currentMeta, player.isPlaying) { meta, playing -> meta to playing }
            .distinctUntilChanged()
            .onEach { (meta, playing) ->
                loadArt(meta.logoUrl)
                notify(build(meta, playing))
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> player.togglePlayPause()
            ACTION_STOP -> tuner.stop() // which stops this service in turn
        }
        // Not sticky: a service the system restarts with no stream behind it would post a notification
        // for nothing playing, with no way for the user to make it go away.
        return START_NOT_STICKY
    }

    /** Swiping the app out of Recents is not "keep playing in the background" — it is "close it". */
    override fun onTaskRemoved(rootIntent: Intent?) {
        tuner.stop()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** The app's own language, which is not necessarily the phone's — the same rule core's download
     *  notification follows. */
    private fun localized(): Context = AppLocale.wrap(this, localeStore.currentTag.value)

    private fun build(meta: MediaMeta, playing: Boolean): Notification {
        val ctx = localized()
        ensureChannel(ctx)
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(meta.title ?: ctx.getString(R.string.app_name))
            .setContentText(meta.subtitle.orEmpty())
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                action(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    ctx.getString(R.string.settings_remote_action_play_pause),
                    ACTION_TOGGLE,
                ),
            )
            .addAction(
                action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    ctx.getString(R.string.content_close),
                    ACTION_STOP,
                ),
            )
        art?.let { builder.setLargeIcon(it) }
        // The token is what the lock screen reads: title, position and the seek bar all come from the
        // session, so they cannot drift from what the player is actually doing.
        val style = Notification.MediaStyle().setShowActionsInCompactView(0)
        session.token?.let { style.setMediaSession(it) }
        return builder.setStyle(style).build()
    }

    private fun action(icon: Int, label: String, intentAction: String): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(this, icon),
            label,
            PendingIntent.getService(
                this,
                intentAction.hashCode(),
                Intent(this, PlaybackService::class.java).setAction(intentAction),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

    private fun startForeground(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun notify(notification: Notification) {
        // Refusing POST_NOTIFICATIONS silences the notification but must never take playback with it.
        runCatching {
            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun loadArt(url: String?) {
        if (url == artUrl) return
        artUrl = url
        art = if (url.isNullOrBlank()) {
            null
        } else {
            val result = runCatching {
                SingletonImageLoader.get(this).execute(ImageRequest.Builder(this).data(url).build())
            }.getOrNull()
            (result as? SuccessResult)?.image?.toBitmap()
        }
    }

    private fun ensureChannel(localized: Context) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val name = localized.getString(R.string.settings_playback_group)
        // Re-created when the app's language changes, so the channel is never left in the old one.
        if (name == lastChannelName) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        lastChannelName = name
    }

    companion object {
        private const val CHANNEL_ID = "owntv_playback"
        private const val NOTIFICATION_ID = 4301
        private const val ACTION_TOGGLE = "tv.own.owntv.mobile.TOGGLE"
        private const val ACTION_STOP = "tv.own.owntv.mobile.STOP"

        /** Called when a stream starts. Safe to call again for a stream already playing. */
        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, PlaybackService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, PlaybackService::class.java)) }
        }
    }
}
