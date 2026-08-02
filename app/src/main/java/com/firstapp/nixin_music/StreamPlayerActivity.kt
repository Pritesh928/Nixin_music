package com.firstapp.nixin_music

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StreamPlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    private var videoList = listOf<VideoItem>()
    private var currentPosition = 0

    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (!::player.isInitialized) {
                handler.postDelayed(this, 500)
                return
            }
            val duration = player.duration.takeIf { it > 0 } ?: 0
            val position = player.currentPosition

            findViewById<SeekBar>(R.id.streamSeekBar).apply {
                max = duration.toInt()
                progress = position.toInt()
            }
            findViewById<TextView>(R.id.txtStreamElapsed).text = formatTime(position)
            findViewById<TextView>(R.id.txtStreamDuration).text = formatTime(duration)
            findViewById<ImageButton>(R.id.btnStreamPlayPause)
                .setImageResource(
                    if (player.isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )
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
        }
        currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)

        val videoId = intent.getStringExtra("VIDEO_ID") ?: run { finish(); return }

        findViewById<ImageButton>(R.id.btnStreamBack).setOnClickListener { finish() }
        handler.post(updateSeekBar)

        loadSong(videoId,
            intent.getStringExtra("SONG_TITLE") ?: "",
            intent.getStringExtra("THUMBNAIL") ?: "")
    }

    private fun loadSong(videoId: String, title: String, thumbnail: String) {
        val loadingText = findViewById<TextView>(R.id.txtStreamSource)
        val playPauseBtn = findViewById<ImageButton>(R.id.btnStreamPlayPause)
        val seekBar = findViewById<SeekBar>(R.id.streamSeekBar)

        findViewById<TextView>(R.id.txtStreamTitle).text = title
        Glide.with(this).load(thumbnail).placeholder(R.drawable.music)
            .into(findViewById(R.id.imgStreamThumb))

        loadingText.text = "Loading..."
        playPauseBtn.isEnabled = false
        seekBar.isEnabled = false

        if (::player.isInitialized) {
            player.release()
        }

        RetrofitClient.api.getStreamUrl(videoId)
            .enqueue(object : retrofit2.Callback<String> {
                override fun onResponse(
                    call: retrofit2.Call<String>,
                    response: retrofit2.Response<String>
                ) {
                    if (response.isSuccessful) {
                        val streamUrl = response.body() ?: return
                        loadingText.text = "YouTube"
                        playPauseBtn.isEnabled = true
                        seekBar.isEnabled = true
                        setupPlayer(streamUrl)
                    } else {
                        loadingText.text = "Failed — trying next song"
                        playNextSong() // auto skip if failed
                    }
                }
                override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
                    loadingText.text = "Error — trying next"
                    playNextSong()
                }
            })
    }

    private fun setupPlayer(streamUrl: String) {
        player = ExoPlayer.Builder(this).build()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    playNextSong()
                }
            }
        })

        findViewById<SeekBar>(R.id.streamSeekBar)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) player.seekTo(progress.toLong())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        findViewById<ImageButton>(R.id.btnStreamPlayPause).setOnClickListener {
            if (player.isPlaying) player.pause() else player.play()
        }
        findViewById<ImageButton>(R.id.btnStreamPrev).setOnClickListener {
            playPrevSong()
        }
        findViewById<ImageButton>(R.id.btnStreamNext).setOnClickListener {
            playNextSong()
        }
    }

    private fun playNextSong() {
        if (videoList.isEmpty()) return

        // Pick next — go to next in list, wrap around to start
        currentPosition = (currentPosition + 1) % videoList.size
        val next = videoList[currentPosition]

        Toast.makeText(this, "Next: ${next.title}", Toast.LENGTH_SHORT).show()
        loadSong(next.videoId, next.title, next.thumbnail)
    }

    private fun playPrevSong() {
        if (videoList.isEmpty()) return

        currentPosition = if (currentPosition > 0) currentPosition - 1
        else videoList.size - 1
        val prev = videoList[currentPosition]

        Toast.makeText(this, "Previous: ${prev.title}", Toast.LENGTH_SHORT).show()
        loadSong(prev.videoId, prev.title, prev.thumbnail)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        if (::player.isInitialized) player.release()
    }
}