package com.geckour.nowplaying4droid.app.api

import android.content.Context
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4droid.BuildConfig
import com.geckour.nowplaying4droid.app.api.model.SpotifyUser
import com.geckour.nowplaying4droid.app.domain.model.SpotifyResult
import com.geckour.nowplaying4droid.app.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.getSpotifyUserInfo
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.storeSpotifyUserInfoImmediately
import com.geckour.nowplaying4droid.app.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import timber.log.Timber
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
    suspend fun searchYouTube(
        trackCoreElement: TrackDetail.TrackCoreElement,
    ): String? {
        val query = trackCoreElement.youTubeSearchQuery
        return withCatching {
            service.searchYouTube(query)
                .items
                .firstOrNull()
                ?.id
                ?.videoId
                ?.let { "https://music.youtube.com/watch?v=$it" }
        }
    }
}