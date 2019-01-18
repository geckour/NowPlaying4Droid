package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyApiClient {
    private val authService = Retrofit.Builder()
            .client(OkHttpProvider.spotifyAuthClient)
            .baseUrl("https://accounts.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(SpotifyAuthService::class.java)

    private val service: SpotifyApiService
        get() = Retrofit.Builder()
                .client(OkHttpProvider.spotifyApiClient)
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(SpotifyApiService::class.java)

    private suspend fun resetToken() {
        OkHttpProvider.spotifyAuthToken = authService.getToken().await().accessToken
    }

    suspend fun getSpotifyUrl(trackCoreElement: TrackCoreElement): String? {
        resetToken()
        return trackCoreElement.spotifySearchQuery?.let {
            service.searchSpotifyItem(it).await().tracks?.items?.firstOrNull()?.urls?.get("spotify")
        }
    }
}