package com.firstapp.nixin_music

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var historyRecycler: RecyclerView
    private lateinit var resultRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchView = findViewById(R.id.searchView)
        historyRecycler = findViewById(R.id.historyRecycler)
        resultRecycler = findViewById(R.id.resultRecycler)


        val title = findViewById<View>(R.id.txtSearch)

        historyRecycler.layoutManager = LinearLayoutManager(this)
        resultRecycler.layoutManager = LinearLayoutManager(this)

        // History data
        val historyItems = listOf(
            HistoryItem("zaalima"),
            HistoryItem("sirra"),
            HistoryItem("primagen"),
            HistoryItem("hor nach"),
            HistoryItem("tmkoc 1474 ep"),
            HistoryItem("cars 2 full movie in hindi"),
            HistoryItem("deva deva song"),
            HistoryItem("springboot")
        )

        historyRecycler.adapter = HistoryAdapter(historyItems)

        findViewById<ImageButton>(R.id.downloads).setOnClickListener {
            startActivity(Intent(this, DownloadActivity::class.java))
        }

        findViewById<ImageButton>(R.id.library).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->

            if (hasFocus) {

                title.visibility = View.GONE

                historyRecycler.visibility = View.VISIBLE
                resultRecycler.visibility = View.GONE
            }
        }

        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {

                    historyRecycler.visibility = View.GONE
                    resultRecycler.visibility = View.VISIBLE

                    val results = listOf(
                        VideoItem(
                            "Zaalima – Full Song",
                            "03:11",
                            "Sony Music India",
                            R.drawable.music
                        ),
                        VideoItem(
                            "Tera Hone Laga Hoon Lyrics",
                            "04:59",
                            "T-Series",
                            R.drawable.music
                        ),
                        VideoItem(
                            "Mai Rahoon Yaa Naa Rahoon - Lyrics",
                            "03:55",
                            "7clouds india",
                            R.drawable.music
                        ),
                        VideoItem(
                            "Dil Diya Gallan - Full Song",
                            "04:34",
                            "Atif Aslam",
                            R.drawable.music
                        ),
                        VideoItem(
                            "Sirra Official Song",
                            "04:16",
                            "Guru Randhawa",
                            R.drawable.music
                        ),
                        VideoItem(
                            "Azul - MV Song Video ",
                            "03:50",
                            "Guru Randhawa",
                            R.drawable.music
                        ),
                        VideoItem(
                            "Qatal Full Song",
                            "05:00",
                            "Guru Randhawa",
                            R.drawable.music
                        ),
                        VideoItem(
                            "For a Reason - Full Song",
                            "04:00",
                            "Karan Aujla",
                            R.drawable.music
                        )
                    )

                    resultRecycler.adapter =
                        VideoAdapter(results)

                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            }
        )
    }
}