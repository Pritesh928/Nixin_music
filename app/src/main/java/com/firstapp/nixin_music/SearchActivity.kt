package com.firstapp.nixin_music

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Response
import retrofit2.Callback

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

        val prefs = getSharedPreferences("search_history", MODE_PRIVATE)
        val history = prefs.getStringSet("queries", emptySet())!!.toList()
        historyRecycler.adapter = HistoryAdapter(history.map { HistoryItem(it) })

        findViewById<ImageButton>(R.id.searchpage).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

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

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrBlank()) return false

                historyRecycler.visibility = View.GONE
                resultRecycler.visibility = View.VISIBLE

                val preferences = getSharedPreferences("search_history", MODE_PRIVATE)
                val history = preferences.getStringSet("queries", mutableSetOf())!!.toMutableSet()
                history.add(query)
                preferences.edit().putStringSet("queries",history).apply()


                RetrofitClient.api.search(query).enqueue(object : Callback<List<VideoItem>> {
                    override fun onResponse(
                        p0: retrofit2.Call<List<VideoItem>>,
                        response: Response<List<VideoItem>>
                    ) {
                        if (response.isSuccessful) {
                            val results = response.body() ?: emptyList()
                            resultRecycler.adapter = VideoAdapter(results)
                        } else {
                            Toast.makeText(this@SearchActivity, "Search failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(p0: retrofit2.Call<List<VideoItem>>, t: Throwable) {
                        Toast.makeText(this@SearchActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })
    }
}