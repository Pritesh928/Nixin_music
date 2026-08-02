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
            holder.itemView.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    holder.itemView.animate()
                        .scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

            val context = holder.itemView.context

            val videoListJson = com.google.gson.Gson().toJson(videos)

            val intent = Intent(context, StreamPlayerActivity::class.java).apply {
                putExtra("VIDEO_ID", item.videoId)
                putExtra("SONG_TITLE", item.title)
                putExtra("THUMBNAIL", item.thumbnail)
                putExtra("CURRENT_POSITION", position)
                putExtra("VIDEO_LIST", videoListJson)  // pass full list
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = videos.size
}