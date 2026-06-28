package com.firstapp.nixin_music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter (
    private  val videos: List<VideoItem>
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.imgThumb)
        val title: TextView = view.findViewById(R.id.txtTitle)
        val duration: TextView = view.findViewById(R.id.txtDuration)
        val channel: TextView = view.findViewById(R.id.txtChannel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = videos[position]
        holder.title.text = item.title
        holder.duration.text = item.videoId
        holder.channel.text = ""

        //load thumbnail from url
        Glide.with(holder.itemView.context)
            .load(item.thumbnail)
            .placeholder(R.drawable.music)
            .into(holder.thumb)
    }

    override fun getItemCount() = videos.size
}