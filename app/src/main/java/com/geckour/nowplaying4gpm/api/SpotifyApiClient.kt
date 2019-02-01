package com.geckour.nowplaying4gpm.api

import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

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

    private suspend fun resetToken(): String? {
        val token = try {
            authService.getToken().await().accessToken
        } catch (t: Throwable) {
            Timber.e(t)
            Crashlytics.logException(t)
            null
        }
        OkHttpProvider.spotifyAuthToken = token
        return token
    }

    suspend fun getSpotifyUrl(trackCoreElement: TrackCoreElement): String? {
        resetToken() ?: return null
        return trackCoreElement.spotifySearchQuery?.let {
            try {
                service.searchSpotifyItem(it).await().tracks?.items?.firstOrNull()?.urls?.get("spotify")
            } catch (t: Throwable) {
                Timber.e(t)
                Crashlytics.logException(t)
                null
            }
        }
    }
}