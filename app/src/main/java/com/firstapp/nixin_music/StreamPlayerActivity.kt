package com.firstapp.nixin_music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StreamPlayerActivity : AppCompatActivity() {

    private var streamService: StreamMusicService? = null
    private var isBound = false
    private val handler = Handler(Looper.getMainLooper())

    private var videoList = listOf<VideoItem>()
    private var currentPosition = 0

    companion object {
        var lastVideoList = listOf<VideoItem>()
        var lastPosition = 0
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StreamMusicService.StreamBinder
            streamService = binder.getService()
            isBound = true

            streamService?.onSongEnded = { runOnUiThread { playNextSong() } }

            handler.post(updateSeekBar)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private val updateSeekBar = object : Runnable {
        override fun run() {
            streamService?.let { s ->
                val duration = s.getDuration().takeIf { it > 0 } ?: 0
                val position = s.getCurrentPosition()

                findViewById<SeekBar>(R.id.streamSeekBar).apply {
                    max = duration.toInt()
                    progress = position.toInt()
                }
                findViewById<TextView>(R.id.txtStreamElapsed).text = formatTime(position)
                findViewById<TextView>(R.id.txtStreamDuration).text = formatTime(duration)
                findViewById<ImageButton>(R.id.btnStreamPlayPause)
                    .setImageResource(
                        if (s.isPlaying()) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    )
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_player)

        val videoListJson = intent.getStringExtra("VIDEO_LIST")
        if (videoListJson != null) {
            val type = object : TypeToken<List<VideoItem>>() {}.type
            videoList = Gson().fromJson(videoListJson, type)
            lastVideoList = videoList
        } else {
            videoList = lastVideoList
        }

        currentPosition = intent.getIntExtra("CURRENT_POSITION", lastPosition)
        lastPosition = currentPosition

        val videoId = intent.getStringExtra("VIDEO_ID")
        val title = intent.getStringExtra("SONG_TITLE") ?: ""
        val thumbnail = intent.getStringExtra("THUMBNAIL") ?: ""

        findViewById<TextView>(R.id.txtStreamTitle).text = title
        Glide.with(this).load(thumbnail).placeholder(R.drawable.music)
            .into(findViewById(R.id.imgStreamThumb))

        Intent(this, StreamMusicService::class.java).also {
            startService(it)
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        findViewById<ImageButton>(R.id.btnStreamBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnStreamPlayPause).setOnClickListener {
            streamService?.let { if (it.isPlaying()) it.pause() else it.resume() }
        }
        findViewById<ImageButton>(R.id.btnStreamPrev).setOnClickListener { playPrevSong() }
        findViewById<ImageButton>(R.id.btnStreamNext).setOnClickListener { playNextSong() }
        findViewById<SeekBar>(R.id.streamSeekBar)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) streamService?.seekTo(progress.toLong())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        if (videoId != null) {
            loadSong(videoId, title, thumbnail)
        } else {
            streamService?.let {
                findViewById<TextView>(R.id.txtStreamTitle).text = it.currentTitle
            }
        }
    }

    private fun loadSong(videoId: String, title: String, thumbnail: String) {
        val loadingText = findViewById<TextView>(R.id.txtStreamSource)
        loadingText.text = "Loading..."
        findViewById<ImageButton>(R.id.btnStreamPlayPause).isEnabled = false

        RetrofitClient.api.getStreamUrl(videoId)
            .enqueue(object : retrofit2.Callback<String> {
                override fun onResponse(
                    call: retrofit2.Call<String>,
                    response: retrofit2.Response<String>
                ) {
                    if (response.isSuccessful) {
                        val streamUrl = response.body() ?: return
                        loadingText.text = "YouTube"
                        findViewById<ImageButton>(R.id.btnStreamPlayPause).isEnabled = true

                        // Play through service so it survives activity close
                        streamService?.playSong(streamUrl, title, thumbnail)

                        // Update UI
                        findViewById<TextView>(R.id.txtStreamTitle).text = title
                        Glide.with(this@StreamPlayerActivity)
                            .load(thumbnail).placeholder(R.drawable.music)
                            .into(findViewById(R.id.imgStreamThumb))

                    } else {
                        loadingText.text = "Failed — skipping"
                        playNextSong()
                    }
                }
                override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
                    loadingText.text = "Error — skipping"
                    playNextSong()
                }
            })
    }

    private fun playNextSong() {
        if (videoList.isEmpty()) return
        currentPosition = (currentPosition + 1) % videoList.size
        lastPosition = currentPosition
        val next = videoList[currentPosition]
        Toast.makeText(this, "Next: ${next.title}", Toast.LENGTH_SHORT).show()
        loadSong(next.videoId, next.title, next.thumbnail)
    }

    private fun playPrevSong() {
        if (videoList.isEmpty()) return
        currentPosition = if (currentPosition > 0) currentPosition - 1 else videoList.size - 1
        lastPosition = currentPosition
        val prev = videoList[currentPosition]
        loadSong(prev.videoId, prev.title, prev.thumbnail)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    override fun onStart() {
        super.onStart()
        if (!isBound) {
            Intent(this, StreamMusicService::class.java).also {
                bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updateSeekBar)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}