package com.firstapp.nixin_music

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.nixin_music.databinding.ItemSongBinding

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, position: Int) {
            binding.txtSongTitle.text  = song.title
            binding.txtArtistName.text = song.artist

            // Placeholder icon — replace with Glide for real album art:
            // Glide.with(binding.root).load(song.albumArtUri)
            //     .placeholder(R.drawable.ic_music_note)
            //     .into(binding.imgAlbumArt)
            binding.imgAlbumArt.setImageResource(android.R.drawable.ic_media_play)

            binding.root.setOnClickListener { onSongClick(position) }

            binding.btnMore.setOnClickListener {
                // TODO: PopupMenu (add to playlist, share, etc.)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position)
    }

    override fun getItemCount() = songs.size
}