package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.SpotifySearchResult
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyApiService {

    @GET("/v1/search")
    suspend fun searchSpotifyItem(
        @Query("q")
        query: String,

        @Query("type")
        type: String = "track",

        @Query("limit")
        limit: Int = 1
    ): SpotifySearchResult
}