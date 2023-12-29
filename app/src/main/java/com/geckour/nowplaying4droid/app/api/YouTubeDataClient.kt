package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.util.*

class YouTubeDataClient {

    @OptIn(ExperimentalSerializationApi::class)
    private val service: YouTubeDataService = Retrofit.Builder()
        .client(OkHttpProvider.client)
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
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