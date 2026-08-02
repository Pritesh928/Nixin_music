package com.firstapp.nixin_music

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter(
    private val videos: List<VideoItem>
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.imgThumb)
        val title: TextView = view.findViewById(R.id.txtTitle)
        val duration: TextView = view.findViewById(R.id.txtDuration)
        val channel: TextView = view.findViewById(R.id.txtChannel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = videos[position]
        holder.title.text = item.title
        holder.duration.text = ""
        holder.channel.text = ""

        Glide.with(holder.itemView.context)
            .load(item.thumbnail)
            .placeholder(R.drawable.music)
            .into(holder.thumb)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            Toast.makeText(context, "Loading ${item.title}...", Toast.LENGTH_SHORT).show()

            RetrofitClient.api.getStreamUrl(item.videoId)
                .enqueue(object : retrofit2.Callback<String> {
                    override fun onResponse(
                        call: retrofit2.Call<String>,
                        response: retrofit2.Response<String>
                    ) {
                        if (response.isSuccessful) {
                            val streamUrl = response.body() ?: return
                            val intent = Intent(context, StreamPlayerActivity::class.java).apply {
                                putExtra("STREAM_URL", streamUrl)
                                putExtra("SONG_TITLE", item.title)
                                putExtra("THUMBNAIL", item.thumbnail)
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Failed to get stream", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun getItemCount() = videos.size
}