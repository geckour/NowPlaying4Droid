package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.Results
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesApiService {

    @GET("search")
    fun searchAlbum(
            @Query("term")
            query: String,

            @Query("country")
            country: String = "JP",

            @Query("media")
            mediaType: String = "music",

            @Query("entity")
            entity: String = "album"
    ): Call<Results>
}