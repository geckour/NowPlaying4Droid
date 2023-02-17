package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.BuildConfig
import com.geckour.nowplaying4droid.app.api.model.SpotifyNowPlayingResult
import com.geckour.nowplaying4droid.app.api.model.SpotifySearchResult
import com.geckour.nowplaying4droid.app.api.model.SpotifyUser
import com.geckour.nowplaying4droid.app.api.model.YouTubeSearchResult
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeDataService {

    @GET("/youtube/v3/search")
    suspend fun searchYouTube(
        @Query("q")
        query: String,

        @Query("key")
        key: String = BuildConfig.GOOGLE_CLOUD_API_KEY,

        @Query("part")
        part: String = "id",

        @Query("fields")
        fields: String = "items(id/videoId)",

        @Query("type")
        type: String = "video",

        @Query("maxResults")
        maxResults: Int = 1,
    ): YouTubeSearchResult
}