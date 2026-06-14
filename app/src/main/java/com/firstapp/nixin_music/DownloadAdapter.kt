package com.firstapp.nixin_music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DownloadAdapter(
    private val songs: List<DownloadSong>
) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val image: ImageView =
            view.findViewById(R.id.imgSong)

        val title: TextView =
            view.findViewById(R.id.txtTitle)

        val artist: TextView =
            view.findViewById(R.id.txtArtist)

        val more: ImageButton =
            view.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val song = songs[position]

        holder.title.text = song.title
        holder.artist.text = song.artist
        holder.image.setImageResource(song.thumbnailRes)
    }

    override fun getItemCount() = songs.size
}