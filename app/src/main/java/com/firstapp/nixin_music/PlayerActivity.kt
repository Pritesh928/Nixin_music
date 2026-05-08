package com.firstapp.nixin_music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.firstapp.nixin_music.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val handler = Handler(Looper.getMainLooper())

    // Reference shared state from MainActivity companion
    private val songs get() = MainActivity.songs
    private var currentIndex
        get() = MainActivity.currentIndex
        set(v) { MainActivity.currentIndex = v }
    private val musicService get() = MainActivity.musicService

    // ── SEEKBAR LOOP ──────────────────────────────────────────────────────────
    private val updateSeekBar = object : Runnable {
        override fun run() {
            musicService?.let { service ->
                val pos = service.getCurrentPosition()
                binding.playerSeekBar.progress = pos
                binding.txtElapsed.text = formatTime(pos)
                // sync play/pause icon
                if (service.isPlaying()) {
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    // ── SERVICE CONNECTION ────────────────────────────────────────────────────
    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            MainActivity.musicService = binder.getService()
            isBound = true
            setupSeekBar()           // set max once service is ready
        }
        override fun onServiceDisconnected(name: ComponentName?) { isBound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FULL SCREEN
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore index passed from MainActivity
        currentIndex = intent.getIntExtra(EXTRA_INDEX, currentIndex)

        updateUI()

        // ── CONTROLS ──────────────────────────────────────────────────────────

        // Back / close
        binding.btnBack.setOnClickListener { finish() }

        // Play / Pause
        binding.btnPlayPause.setOnClickListener {
            musicService?.let { service ->
                if (service.isPlaying()) service.pauseSong() else service.resumeSong()
            }
        }

        // Previous
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) { currentIndex--; playSong(currentIndex) }
        }

        // Next
        binding.btnNext.setOnClickListener {
            if (currentIndex < songs.size - 1) { currentIndex++; playSong(currentIndex) }
        }

        // Shuffle (visual toggle only — wire shuffle logic as needed)
        var shuffleOn = false
        binding.btnShuffle.setOnClickListener {
            shuffleOn = !shuffleOn
            binding.btnShuffle.alpha = if (shuffleOn) 1f else 0.4f
        }

        // Repeat (visual toggle only — wire repeat logic as needed)
        var repeatOn = false
        binding.btnRepeat.setOnClickListener {
            repeatOn = !repeatOn
            binding.btnRepeat.alpha = if (repeatOn) 1f else 0.4f
        }

        // SeekBar drag
        binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                    binding.txtElapsed.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        handler.post(updateSeekBar)
    }

    // ── UPDATE UI WITH CURRENT SONG ───────────────────────────────────────────
    private fun updateUI() {
        if (songs.isEmpty()) return
        val song = songs[currentIndex]
        binding.txtPlayerSongTitle.text = song.title
        binding.txtPlayerArtist.text = song.artist
        binding.txtPlaylistName.text = "My Music"
        // Album art: use Glide/Picasso here if you have artwork URIs
        // Glide.with(this).load(song.albumArtUri).into(binding.imgPlayerAlbumArt)
    }

    // ── PLAY + REFRESH UI ─────────────────────────────────────────────────────
    private fun playSong(index: Int) {
        if (songs.isEmpty()) return
        val song = songs[index]
        musicService?.playSong(song.path)
        updateUI()
        setupSeekBar()
    }

    // ── SEEKBAR MAX ───────────────────────────────────────────────────────────
    private fun setupSeekBar() {
        val duration = musicService?.getDuration() ?: 0
        binding.playerSeekBar.max = duration
        binding.txtDuration.text = formatTime(duration)
    }

    // ── TIME FORMATTER ────────────────────────────────────────────────────────
    private fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }

    // ── SERVICE LIFECYCLE ─────────────────────────────────────────────────────
    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) { unbindService(serviceConnection); isBound = false }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
    }

    companion object {
        const val EXTRA_INDEX = "extra_song_index"
    }
}