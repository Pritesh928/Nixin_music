package com.firstapp.nixin_music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class StreamMusicService : Service() {

    inner class StreamBinder : Binder() {
        fun getService(): StreamMusicService = this@StreamMusicService
    }

    private val binder = StreamBinder()
    private lateinit var player: ExoPlayer

    var currentTitle = ""
    var currentThumbnail = ""
    var onSongEnded: (() -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "stream_channel"
        const val NOTIFICATION_ID = 2
        const val ACTION_PLAY_PAUSE = "stream.PLAY_PAUSE"
        const val ACTION_STOP = "stream.STOP"
        var currentStreamTitle = ""
        var currentStreamThumbnail = ""
        var currentVideoId = ""
        var instance: StreamMusicService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        player = ExoPlayer.Builder(this).build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onSongEnded?.invoke()
                }
                updateNotification()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_STOP -> {
                player.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    fun playSong(streamUrl: String, title: String, thumbnail: String, videoId: String = "") {
        currentTitle = title
        currentThumbnail = thumbnail
        currentStreamTitle = title
        currentStreamThumbnail = thumbnail
        currentVideoId = videoId
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()
        updateNotification()
    }

    fun isPlaying() = player.isPlaying
    fun pause() = player.pause()
    fun resume() = player.play()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun getCurrentPosition() = player.currentPosition
    fun getDuration() = player.duration
    fun stop() = player.stop()

    private fun updateNotification() {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, StreamPlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SONG_TITLE", currentTitle)
                putExtra("THUMBNAIL", currentThumbnail)
                putExtra("FROM_NOTIFICATION", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePending = PendingIntent.getService(
            this, 1,
            Intent(this, StreamMusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this, 2,
            Intent(this, StreamMusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (player.isPlaying)
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText("Streaming from YouTube")
            .setContentIntent(openIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(playPauseIcon, "Play/Pause", playPausePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nixin Stream Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        player.release()
    }
}