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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.RequiresApi

class MusicService : Service() {


    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    private val binder = MusicBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var mediaSession: MediaSessionCompat
    private val CHANNEL_ID   = "nixin_music_channel"
    private val NOTIFICATION_ID = 1
    var onSongChanged : ((Song) -> Unit)? = null
    companion object {
        const val ACTION_PLAY_PAUSE = "com.firstapp.nixin_music.PLAY_PAUSE"
        const val ACTION_NEXT       = "com.firstapp.nixin_music.NEXT"
        const val ACTION_PREV       = "com.firstapp.nixin_music.PREV"
        const val ACTION_STOP       = "com.firstapp.nixin_music.STOP"
        var isShuffleOn = false
        var isRepeatOn = false
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        setupMediaSession()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (isPlaying()) pauseSong() else resumeSong()
            ACTION_NEXT -> MainActivity.let {
                when {
                    isShuffleOn -> {
                        it.currentIndex = (it.songs.indices).random()
                    }
                    it.currentIndex < it.songs.size - 1 -> {
                        it.currentIndex++
                    }
                    else -> return@let
                }
                playSong(it.songs[it.currentIndex].path)
                updateNotification(it.songs[it.currentIndex])
                onSongChanged?.invoke(it.songs[it.currentIndex])
            }

            ACTION_PREV -> MainActivity.let {
                when {
                    isShuffleOn -> {
                        it.currentIndex = (it.songs.indices).random()
                    }
                    it.currentIndex > 0 -> {
                        it.currentIndex--
                    }
                    else -> return@let
                }
                playSong(it.songs[it.currentIndex].path)
                updateNotification(it.songs[it.currentIndex])
                onSongChanged?.invoke(it.songs[it.currentIndex])
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        mediaSession.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocus(): Boolean {

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->

            when (focusChange) {

                AudioManager.AUDIOFOCUS_LOSS -> {
                    pauseSong()
                    audioFocusRequest?.let {
                        audioManager.abandonAudioFocusRequest(it)
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    pauseSong()
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    resumeSong()
                }
            }
        }

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "NixinMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onPlay()     { resumeSong() }
                override fun onPause()    { pauseSong()  }
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onSkipToNext() {
                    MainActivity.let {
                        if (it.currentIndex < it.songs.size - 1) {
                            it.currentIndex++
                            playSong(it.songs[it.currentIndex].path)
                            updateNotification(it.songs[it.currentIndex])
                        }
                    }
                }
                @RequiresApi(Build.VERSION_CODES.O)
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


    @RequiresApi(Build.VERSION_CODES.O)
    fun playSong(path: String) {
        val preferences = getSharedPreferences("music_state", MODE_PRIVATE)
        preferences.edit()
            .putInt("last_index", MainActivity.currentIndex)
            .apply()
        if (!requestAudioFocus()) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener {
                MainActivity.let { main ->
                    when {
                        isRepeatOn -> {
                            val next = main.songs[main.currentIndex]
                            playSong(main.songs[main.currentIndex].path)
                            updateNotification(main.songs[main.currentIndex])
                            onSongChanged?.invoke(next)
                        }
                        isShuffleOn -> {
                            main.currentIndex = (main.songs.indices).random()
                            val next = main.songs[main.currentIndex]
                            playSong(next.path)
                            updateNotification(next)
                            onSongChanged?.invoke(next)
                        }
                        main.currentIndex < main.songs.size - 1 -> {
                            main.currentIndex++
                            val next = main.songs[main.currentIndex]
                            playSong(next.path)
                            updateNotification(next)
                            onSongChanged?.invoke(next)
                        }
                    }
                }
            }
        }
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        val current = MainActivity.songs.getOrNull(MainActivity.currentIndex)
        if (current != null) updateNotification(current)
    }

    fun pauseSong() {
        mediaPlayer?.pause()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        val current = MainActivity.songs.getOrNull(MainActivity.currentIndex)
        if (current != null) updateNotification(current)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun resumeSong() {

        if (! requestAudioFocus()) return

        mediaPlayer?.start()

        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)

        val current = MainActivity.songs.getOrNull(MainActivity.currentIndex)

        if (current != null) {
            updateNotification(current)
        }
    }

    fun seekTo(position: Int)      { mediaPlayer?.seekTo(position) }
    fun seekForward10()            { mediaPlayer?.let { seekTo(it.currentPosition + 10_000) } }
    fun seekBack10()               { mediaPlayer?.let { seekTo(it.currentPosition - 10_000) } }
    fun isPlaying()                = mediaPlayer?.isPlaying ?: false
    fun getCurrentPosition()       = mediaPlayer?.currentPosition ?: 0
    fun getDuration()              = mediaPlayer?.duration ?: 0


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


    private fun updateNotification(song: Song) {
        val notification = buildNotification(song)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(song: Song): Notification {


        val openPlayerIntent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(PlayerActivity.EXTRA_INDEX, MainActivity.currentIndex)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openPlayerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val prevPending = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val playPausePending = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val nextPending = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val playPauseIcon = if (isPlaying())
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play


        val albumArt = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)


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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, "Play/Pause", playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nixin Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current song with playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}