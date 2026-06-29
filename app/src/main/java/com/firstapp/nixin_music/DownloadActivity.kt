package com.firstapp.nixin_music

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DownloadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        val recycler =
            findViewById<RecyclerView>(R.id.downloadRecycler)

        recycler.layoutManager =
            LinearLayoutManager(this)

        val downloads = listOf(
            DownloadSong(
                "HOR NACH",
                "T-Series",
                R.drawable.music
            ),

            DownloadSong(
                "Midnight Drive",
                "Neon Beats",
                R.drawable.music
            ),

            DownloadSong(
                "Summer Vibes",
                "DJ Nova",
                R.drawable.music
            ),
            
            DownloadSong(
                "Lost in Rhythm",
                "Echo Sound",
                R.drawable.music
            ),
            
            DownloadSong(
                "Dream Chaser",
                "Skyline Records",
                R.drawable.music
            ),
            
            DownloadSong(
                "Fire Within",
                "Pulse Music",
                R.drawable.music
            ),
            
            DownloadSong(
                "Ocean Breeze",
                "Blue Waves",
                R.drawable.music
            ),
            
            DownloadSong(
                "Golden Hour",
                "Sunset Studio",
                R.drawable.music
            ),
            
            DownloadSong(
                "Electric Love",
                "Voltage Beats",
                R.drawable.music
            ),
            
            DownloadSong(
                "Moonlight Melody",
                "Harmony House",
                R.drawable.music
            ),
            
            DownloadSong(
                "Endless Journey",
                "Infinity Tunes",
                R.drawable.music
            ),
            
            DownloadSong(
                "Heartbeat",
                "Rhythm Nation",
                R.drawable.music
            ),
            
            DownloadSong(
                "Silent Echoes",
                "Acoustic Souls",
                R.drawable.music
            ),
            
            DownloadSong(
                "City Lights",
                "Urban Records",
                R.drawable.music
            ),
            
            DownloadSong(
                "Beyond the Sky",
                "Cloud Nine",
                R.drawable.music
            ),
            
            DownloadSong(
                "Rise Again",
                "Epic Sound",
                R.drawable.music
            ),
            
            DownloadSong(
                "Kesariya",
                "Sony Music",
                R.drawable.music
            ),

            DownloadSong(
                "Heeriye",
                "T-Series",
                R.drawable.music
            )
        )

        recycler.adapter =
            DownloadAdapter(downloads)
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
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
//        prevent back skip from application
        findViewById<ImageButton>(R.id.searchpage).setOnClickListener {
            startActivity(Intent(this, DownloadActivity::class.java))
        }
    }
}