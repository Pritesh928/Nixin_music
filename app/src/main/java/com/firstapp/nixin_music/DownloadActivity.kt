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
}