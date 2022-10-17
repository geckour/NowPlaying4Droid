package com.geckour.nowplaying4gpm.app.api

import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.app.api.model.AlbumWrapper
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApiService {

    @GET("2.0")
    suspend fun searchAlbum(
        @Query("album")
        album: String,

        @Query("artist")
        artist: String,

        @Query("method")
        method: String = "album.getInfo",

        @Query("api_key")
        apiKey: String = BuildConfig.LAST_FM_API_KEY,

        @Query("format")
        format: String = "json"
    ): AlbumWrapper
}