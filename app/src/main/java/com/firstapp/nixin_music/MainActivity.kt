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
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstapp.nixin_music.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())


    private val updateLoop = object : Runnable {
        override fun run() {
            val service = musicService
            if (service != null) {
                if (service.isPlaying()) {
                    binding.miniPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    binding.miniPlayPause.setImageResource(android.R.drawable.ic_media_play)
                }


                val duration = service.getDuration()
                if (duration > 0) {
                    val progress = service.getCurrentPosition().toFloat() / duration
                    val parentWidth = binding.miniProgressFill.parent as? View
                    binding.miniProgressFill.layoutParams =
                        binding.miniProgressFill.layoutParams.also {
                            it.width = ((parentWidth?.width ?: 0) * progress).toInt()
                        }
                    binding.miniProgressFill.requestLayout()
                }
            }
            handler.postDelayed(this, 500)
        }
    }


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) { isBound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // request notification bar permission on Android 13+ phones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerSongs.layoutManager = LinearLayoutManager(this)

        checkStoragePermission()


        binding.miniPlayer.setOnClickListener { openPlayer() }

        binding.miniPlayPause.setOnClickListener {
            musicService?.let { s ->
                if (s.isPlaying()) s.pauseSong() else s.resumeSong()
            }
        }


        binding.miniPrev.setOnClickListener {
            if (currentIndex > 0) { currentIndex--; playSong(currentIndex) }
        }


        binding.miniNext.setOnClickListener {
            if (currentIndex < songs.size - 1) { currentIndex++; playSong(currentIndex) }
        }


        binding.btnShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                currentIndex = (songs.indices).random()
                playSong(currentIndex)
                openPlayer()
            }
        }

        handler.post(updateLoop)
        findViewById<ImageButton>(R.id.searchpage).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageButton>(R.id.downloads).setOnClickListener {
            startActivity(Intent(this, DownloadActivity::class.java))
        }
        findViewById<ImageButton>(R.id.library).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }


    private fun openPlayer() {
        if (songs.isEmpty()) return
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_INDEX, currentIndex)
        )
    }


    private fun loadSongs() {
        songs.clear()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = contentResolver.query(
            uri, null,
            MediaStore.Audio.Media.IS_MUSIC + " != 0",
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val title  = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val path   = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                songs.add(Song(title, artist ?: "Unknown Artist", path))
            }
        }


        binding.songCountBadge.text = "${songs.size} songs"

        val adapter = SongAdapter(songs) { position ->
            currentIndex = position
            playSong(position)
            openPlayer()
        }
        binding.recyclerSongs.adapter = adapter
    }


    private fun playSong(index: Int) {
        if (songs.isEmpty()) return
        val song = songs[index]

        binding.miniPlayer.visibility = View.VISIBLE
        binding.miniSongTitle.text    = song.title
        binding.miniArtistName.text   = song.artist

        musicService?.playSong(song.path)
    }


    private fun checkStoragePermission() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 1)
        } else {
            loadSongs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        }
    }


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
        handler.removeCallbacks(updateLoop)
    }

    companion object {
        val songs = mutableListOf<Song>()
        var currentIndex = 0
        var musicService: MusicService? = null
        var isBound = false
    }
}