package com.firstapp.nixin_music

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/search")
    fun search(@Query("q") query: String): Call<List<VideoItem>>
}
