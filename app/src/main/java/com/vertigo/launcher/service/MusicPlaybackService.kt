package com.vertigo.launcher.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * MusicPlaybackService — Foreground service that publishes a MediaSession
 * and media-style notification so V-Launcher's local music appears in the
 * system notification shade / Dynamic Island / lock-screen controls.
 *
 * Communication pattern:
 *   HomeViewModel  ──(startService intent)──▶  this service  (update UI)
 *   this service   ──(LocalBroadcast)──▶  HomeViewModel     (user tapped button)
 */
class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "v_launcher_music"
        const val NOTIFICATION_ID = 9081

        /* ---------- actions sent TO this service ---------- */
        const val ACTION_PLAY   = "com.vertigo.launcher.music.PLAY"
        const val ACTION_UPDATE = "com.vertigo.launcher.music.UPDATE"
        const val ACTION_STOP   = "com.vertigo.launcher.music.STOP"
        // Notification-button re-entry actions (PendingIntent → onStartCommand)
        const val ACTION_BTN_PLAY_PAUSE = "com.vertigo.launcher.music.BTN_PP"
        const val ACTION_BTN_NEXT       = "com.vertigo.launcher.music.BTN_NX"
        const val ACTION_BTN_PREV       = "com.vertigo.launcher.music.BTN_PV"

        /* ---------- extras ---------- */
        const val EXTRA_TITLE      = "title"
        const val EXTRA_ARTIST     = "artist"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_POSITION   = "position"
        const val EXTRA_DURATION   = "duration"

        /* ---------- broadcasts FROM this service → ViewModel ---------- */
        const val BROADCAST_PLAY_PAUSE = "com.vertigo.launcher.music.CMD_PP"
        const val BROADCAST_NEXT       = "com.vertigo.launcher.music.CMD_NX"
        const val BROADCAST_PREV       = "com.vertigo.launcher.music.CMD_PV"
        const val BROADCAST_SEEK       = "com.vertigo.launcher.music.CMD_SK"
        const val EXTRA_SEEK_POSITION  = "seek_pos"
    }

    private var mediaSession: MediaSession? = null
    private var currentTitle  = "V-Launcher Music"
    private var currentArtist = ""
    private var isPlaying = false
    private var started = false

    /* ───────────────────────── lifecycle ───────────────────────── */

    override fun onCreate() {
        super.onCreate()
        createChannel()
        initSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_PLAY -> {
                readExtras(intent)
                isPlaying = true
                syncSession(intent)
                if (!started) { goForeground(); started = true } else updateNotif()
            }

            ACTION_UPDATE -> {
                readExtras(intent)
                syncSession(intent)
                if (started) updateNotif()
            }

            ACTION_STOP -> {
                isPlaying = false
                mediaSession?.isActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                started = false
                stopSelf()
            }

            /* notification button re-entry */
            ACTION_BTN_PLAY_PAUSE -> broadcast(BROADCAST_PLAY_PAUSE)
            ACTION_BTN_NEXT       -> broadcast(BROADCAST_NEXT)
            ACTION_BTN_PREV       -> broadcast(BROADCAST_PREV)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ───────────────────────── helpers ───────────────────────── */

    private fun readExtras(intent: Intent) {
        currentTitle  = intent.getStringExtra(EXTRA_TITLE)  ?: currentTitle
        currentArtist = intent.getStringExtra(EXTRA_ARTIST) ?: currentArtist
        isPlaying     = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying)
    }

    private fun broadcast(action: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(action))
    }

    /* ───────────────────────── channel ───────────────────────── */

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Music Playback",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "V-Launcher music playback controls"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    /* ───────────────────────── MediaSession ───────────────────────── */

    private fun initSession() {
        mediaSession = MediaSession(this, "VLauncherMusic").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay()           { broadcast(BROADCAST_PLAY_PAUSE) }
                override fun onPause()          { broadcast(BROADCAST_PLAY_PAUSE) }
                override fun onSkipToNext()     { broadcast(BROADCAST_NEXT) }
                override fun onSkipToPrevious() { broadcast(BROADCAST_PREV) }
                override fun onStop()           { broadcast(BROADCAST_PLAY_PAUSE) }
                override fun onSeekTo(pos: Long) {
                    LocalBroadcastManager.getInstance(this@MusicPlaybackService)
                        .sendBroadcast(Intent(BROADCAST_SEEK).putExtra(EXTRA_SEEK_POSITION, pos))
                }
            })
            isActive = true
        }
    }

    private fun syncSession(intent: Intent) {
        val pos = intent.getLongExtra(EXTRA_POSITION, 0L)
        val dur = intent.getLongExtra(EXTRA_DURATION, 0L)

        mediaSession?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, dur)
                .build()
        )
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP
                )
                .setState(
                    if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    pos, if (isPlaying) 1.0f else 0f
                )
                .build()
        )
        mediaSession?.isActive = isPlaying
    }

    /* ───────────────────────── notification ───────────────────────── */

    private fun goForeground() {
        val n = buildNotif()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun updateNotif() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotif())
    }

    private fun svcPI(action: String, code: Int): PendingIntent =
        PendingIntent.getService(this, code,
            Intent(this, MusicPlaybackService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun buildNotif(): Notification {
        val ppIcon = if (isPlaying) android.R.drawable.ic_media_pause
                     else android.R.drawable.ic_media_play
        val ppLabel = if (isPlaying) "Pause" else "Play"

        val contentPI = PendingIntent.getActivity(this, 3,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setContentIntent(contentPI)
            .setOngoing(isPlaying)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(Notification.Action.Builder(
                android.R.drawable.ic_media_previous, "Prev", svcPI(ACTION_BTN_PREV, 0)).build())
            .addAction(Notification.Action.Builder(
                ppIcon, ppLabel, svcPI(ACTION_BTN_PLAY_PAUSE, 1)).build())
            .addAction(Notification.Action.Builder(
                android.R.drawable.ic_media_next, "Next", svcPI(ACTION_BTN_NEXT, 2)).build())
            .setStyle(Notification.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))

        // Set rich album art placeholder so status bar media controls treat it with high priority
        try {
            val largeIcon = android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_play)
            builder.setLargeIcon(largeIcon)
            builder.setColorized(true)
            builder.setColor(0xFF00F0FF.toInt())
        } catch (e: Exception) {
            // Fallback
        }

        return builder.build()
    }
}
