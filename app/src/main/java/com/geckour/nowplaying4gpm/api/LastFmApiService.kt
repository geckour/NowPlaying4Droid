package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.model.AlbumWrapper
import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApiService {

    @GET("2.0")
    fun searchAlbum(
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
    ): Deferred<AlbumWrapper>
}