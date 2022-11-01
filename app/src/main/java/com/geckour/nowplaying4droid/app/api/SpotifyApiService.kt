package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.api.model.SpotifyNowPlayingResult
import com.geckour.nowplaying4droid.app.api.model.SpotifySearchResult
import com.geckour.nowplaying4droid.app.api.model.SpotifyUser
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyApiService {

    @GET("/v1/me")
    suspend fun getUser(): SpotifyUser

    @GET("/v1/search")
    suspend fun searchSpotifyItem(
        @Query("q")
        query: String,

        @Query("type")
        type: String = "track",

        @Query("market")
        marketCountryCode: String = "from_token",

        @Query("limit")
        limit: Int = 20
    ): SpotifySearchResult

    @GET("/v1/me/player")
    suspend fun getCurrentPlayback(
        @Query("market")
        marketCountryCode: String = "from_token"
    ): SpotifyNowPlayingResult?
}