package com.firstapp.nixin_music

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide

class StreamPlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    private val updateSeekBar = object : Runnable {
        override fun run() {
            val duration = player.duration.takeIf { it > 0 } ?: 0
            val position = player.currentPosition

            findViewById<SeekBar>(R.id.streamSeekBar).apply {
                max = duration.toInt()
                progress = position.toInt()
            }
            findViewById<TextView>(R.id.txtStreamElapsed).text = formatTime(position)
            findViewById<TextView>(R.id.txtStreamDuration).text = formatTime(duration)

            if (player.isPlaying) {
                findViewById<ImageButton>(R.id.btnStreamPlayPause)
                    .setImageResource(android.R.drawable.ic_media_pause)
            } else {
                findViewById<ImageButton>(R.id.btnStreamPlayPause)
                    .setImageResource(android.R.drawable.ic_media_play)
            }

            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_player)

        val streamUrl = intent.getStringExtra("STREAM_URL") ?: return
        val title = intent.getStringExtra("SONG_TITLE") ?: ""
        val thumbnail = intent.getStringExtra("THUMBNAIL") ?: ""

        findViewById<TextView>(R.id.txtStreamTitle).text = title

        Glide.with(this)
            .load(thumbnail)
            .placeholder(R.drawable.music)
            .into(findViewById(R.id.imgStreamThumb))

        player = ExoPlayer.Builder(this).build()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()

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

        findViewById<ImageButton>(R.id.btnStreamBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnStreamPrev).setOnClickListener {
            player.seekTo(maxOf(0, player.currentPosition - 10000))
        }
        findViewById<ImageButton>(R.id.btnStreamNext).setOnClickListener {
            player.seekTo(minOf(player.duration, player.currentPosition + 10000))
        }

        handler.post(updateSeekBar)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        player.release()
    }
}