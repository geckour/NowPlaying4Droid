package com.geckour.nowplaying4gpm.api

import android.content.Context
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyApiClient {
    private lateinit var service: SpotifyApiService

    suspend fun init() {
        if (OkHttpProvider.spotifyAuthToken == null || ::service.isInitialized.not()) {
            OkHttpProvider.spotifyAuthToken = Retrofit.Builder()
                    .client(OkHttpProvider.spotifyAuthClient)
                    .baseUrl("https://accounts.spotify.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .build()
                    .create(SpotifyAuthService::class.java)
                    .getToken()
                    .await()
                    .accessToken

            service = Retrofit.Builder()
                    .client(OkHttpProvider.spotifyApiClient)
                    .baseUrl("https://api.spotify.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .build()
                    .create(SpotifyApiService::class.java)
        }
    }

    suspend fun getSpotifyUrl(context: Context, trackCoreElement: TrackCoreElement): String? {
        init()
        return trackCoreElement.spotifySearchQuery?.let {
            service.searchSpotifyItem(it).await().tracks?.items?.firstOrNull()?.urls?.get("spotify")
        }
    }
}