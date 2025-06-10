package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.withCatching
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class YouTubeDataClient {

    private val service: YouTubeDataService = Retrofit.Builder()
        .client(OkHttpProvider.client)
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(
            json.asConverterFactory("application/json; charset=UTF8".toMediaType())
        )
        .build()
        .create(YouTubeDataService::class.java)

    /**
     * @return YouTube Music URL
     */
    suspend fun searchYouTube(query: String): String? {
        return withCatching {
            service.searchYouTube(query)
                .items
                .firstOrNull()
                ?.id
                ?.videoId
                ?.let { "https://music.youtube.com/watch?v=$it" }
        }
    }

    /**
     * @return YouTube Music URL
     */
    suspend fun searchYouTube(
        trackCoreElement: TrackDetail.TrackCoreElement,
    ): String? {
        val query = trackCoreElement.youTubeSearchQuery
        return searchYouTube(query)
    }
}