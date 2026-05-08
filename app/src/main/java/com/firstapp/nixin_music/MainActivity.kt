package com.firstapp.nixin_music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.view.WindowManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstapp.nixin_music.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    // SEEKBAR UPDATE (drives mini-player progress if needed)
    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (musicService == null) {
                handler.postDelayed(this, 500)
                return
            }
            val service = musicService!!
            if (service.isPlaying()) {
                binding.miniPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                binding.miniPlayPause.setImageResource(android.R.drawable.ic_media_play)
            }
            handler.postDelayed(this, 500)
        }
    }

    // SERVICE CONNECTION
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FULL SCREEN GLASS EFFECT
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerSongs.layoutManager = LinearLayoutManager(this)

        checkPermission()

        // MINI PLAYER: click whole bar → open PlayerActivity
        binding.miniPlayer.setOnClickListener { openPlayer() }

        // MINI PLAYER: play/pause button
        binding.miniPlayPause.setOnClickListener {
            musicService?.let { service ->
                if (service.isPlaying()) service.pauseSong() else service.resumeSong()
            }
        }

        // MINI PLAYER: next button
        binding.miniNext.setOnClickListener {
            if (currentIndex < songs.size - 1) {
                currentIndex++
                playSong(currentIndex)
            }
        }

        handler.post(updateSeekBar)
    }

    // ── OPEN FULL PLAYER ──────────────────────────────────────────────────────
    private fun openPlayer() {
        if (songs.isEmpty()) return
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_INDEX, currentIndex)
        startActivity(intent)
    }

    // ── SONG LIST ─────────────────────────────────────────────────────────────
    private fun loadSongs() {
        songs.clear()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = contentResolver.query(
            uri,
            null,
            MediaStore.Audio.Media.IS_MUSIC + " != 0",
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                songs.add(Song(title, artist ?: "Unknown Artist", path))
            }
        }

        val adapter = SongAdapter(songs) { position ->
            currentIndex = position
            playSong(position)
            openPlayer()          // tap on a row → play + open full player
        }
        binding.recyclerSongs.adapter = adapter
    }

    // ── PLAY SONG ─────────────────────────────────────────────────────────────
    private fun playSong(index: Int) {
        if (songs.isEmpty()) return
        val song = songs[index]

        // Show / update mini-player bar
        binding.miniPlayer.visibility = View.VISIBLE
        binding.miniSongTitle.text = song.title
        binding.miniArtistName.text = song.artist

        musicService?.playSong(song.path)
    }

    // ── PERMISSIONS ───────────────────────────────────────────────────────────
    private fun checkPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        } else {
            loadSongs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        }
    }

    // ── SERVICE LIFECYCLE ─────────────────────────────────────────────────────
    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
            startService(it)
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

    // ── COMPANION (shared state for PlayerActivity) ───────────────────────────
    companion object {
        val songs = mutableListOf<Song>()
        var currentIndex = 0
        var musicService: MusicService? = null
        var isBound = false
    }
}