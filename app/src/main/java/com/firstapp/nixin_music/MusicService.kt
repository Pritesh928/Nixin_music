package com.firstapp.nixin_music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.app.Service

class MusicService : Service() {

    // ── BINDER ────────────────────────────────────────────────────────────────
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    private val binder = MusicBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    // ── MEDIA PLAYER ──────────────────────────────────────────────────────────
    private var mediaPlayer: MediaPlayer? = null

    // ── MEDIA SESSION (powers the notification controls) ──────────────────────
    private lateinit var mediaSession: MediaSessionCompat

    // ── NOTIFICATION ──────────────────────────────────────────────────────────
    private val CHANNEL_ID   = "nixin_music_channel"
    private val NOTIFICATION_ID = 1

    // Actions sent by notification buttons
    companion object {
        const val ACTION_PLAY_PAUSE = "com.firstapp.nixin_music.PLAY_PAUSE"
        const val ACTION_NEXT       = "com.firstapp.nixin_music.NEXT"
        const val ACTION_PREV       = "com.firstapp.nixin_music.PREV"
        const val ACTION_STOP       = "com.firstapp.nixin_music.STOP"
    }

    // ── LIFECYCLE ─────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle button taps from the notification
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (isPlaying()) pauseSong() else resumeSong()
            ACTION_NEXT       -> MainActivity.let {
                if (it.currentIndex < it.songs.size - 1) {
                    it.currentIndex++
                    playSong(it.songs[it.currentIndex].path)
                    updateNotification(it.songs[it.currentIndex])
                }
            }
            ACTION_PREV       -> MainActivity.let {
                if (it.currentIndex > 0) {
                    it.currentIndex--
                    playSong(it.songs[it.currentIndex].path)
                    updateNotification(it.songs[it.currentIndex])
                }
            }
            ACTION_STOP       -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
    }

    // ── MEDIA SESSION SETUP ───────────────────────────────────────────────────
    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "NixinMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay()     { resumeSong() }
                override fun onPause()    { pauseSong()  }
                override fun onSkipToNext() {
                    MainActivity.let {
                        if (it.currentIndex < it.songs.size - 1) {
                            it.currentIndex++
                            playSong(it.songs[it.currentIndex].path)
                            updateNotification(it.songs[it.currentIndex])
                        }
                    }
                }
                override fun onSkipToPrevious() {
                    MainActivity.let {
                        if (it.currentIndex > 0) {
                            it.currentIndex--
                            playSong(it.songs[it.currentIndex].path)
                            updateNotification(it.songs[it.currentIndex])
                        }
                    }
                }
            })
            isActive = true
        }
    }

    // ── PLAY / PAUSE / RESUME ─────────────────────────────────────────────────
    fun playSong(path: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener {
                // Auto-advance to next song
                MainActivity.let { main ->
                    if (main.currentIndex < main.songs.size - 1) {
                        main.currentIndex++
                        val next = main.songs[main.currentIndex]
                        playSong(next.path)
                        updateNotification(next)
                    }
                }
            }
        }
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)

        // Show / refresh notification with current song info
        val current = MainActivity.songs.getOrNull(MainActivity.currentIndex)
        if (current != null) updateNotification(current)
    }

    fun pauseSong() {
        mediaPlayer?.pause()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        val current = MainActivity.songs.getOrNull(MainActivity.currentIndex)
        if (current != null) updateNotification(current)
    }

    fun resumeSong() {
        mediaPlayer?.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        val current = MainActivity.songs.getOrNull(MainActivity.currentIndex)
        if (current != null) updateNotification(current)
    }

    fun seekTo(position: Int)      { mediaPlayer?.seekTo(position) }
    fun seekForward10()            { mediaPlayer?.let { seekTo(it.currentPosition + 10_000) } }
    fun seekBack10()               { mediaPlayer?.let { seekTo(it.currentPosition - 10_000) } }
    fun isPlaying()                = mediaPlayer?.isPlaying ?: false
    fun getCurrentPosition()       = mediaPlayer?.currentPosition ?: 0
    fun getDuration()              = mediaPlayer?.duration ?: 0

    // ── PLAYBACK STATE (keeps lock-screen controls in sync) ───────────────────
    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, getCurrentPosition().toLong(), 1f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    // ── BUILD & SHOW NOTIFICATION ─────────────────────────────────────────────
    private fun updateNotification(song: Song) {
        val notification = buildNotification(song)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(song: Song): Notification {

        // Tapping the notification opens PlayerActivity
        val openPlayerIntent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(PlayerActivity.EXTRA_INDEX, MainActivity.currentIndex)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openPlayerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification button: Previous
        val prevPending = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification button: Play / Pause
        val playPausePending = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification button: Next
        val nextPending = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause icon switches based on state
        val playPauseIcon = if (isPlaying())
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

        // Album art placeholder (swap with real art via MediaMetadataRetriever if needed)
        val albumArt = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        // Update MediaSession metadata so lock screen shows correct info
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration().toLong())
                .build()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(albumArt)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // shows on lock screen
            .setOnlyAlertOnce(true)                               // no sound/vibrate on update
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, "Play/Pause", playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)   // show all 3 in collapsed view
            )
            .build()
    }

    // ── NOTIFICATION CHANNEL (required Android 8+) ────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nixin Music Playback",
                NotificationManager.IMPORTANCE_LOW      // LOW = no sound, no heads-up
            ).apply {
                description = "Shows current song with playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}