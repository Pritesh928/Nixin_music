package com.firstapp.nixin_music


data class Song(
    val title: String,
    val artist: String = "Unknown Artist",
    val path: String
)

