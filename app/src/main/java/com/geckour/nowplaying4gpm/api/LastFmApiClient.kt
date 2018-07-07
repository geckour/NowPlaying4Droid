package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.Album
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class LastFmApiClient {
    private val service = Retrofit.Builder()
            .client(OkHttpProvider.client)
            .baseUrl("http://ws.audioscrobbler.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(LastFmApiService::class.java)

    suspend fun searchAlbum(album: String?, artist: String?): Album? =
            if (album == null || artist == null) null
            else try {
                service.searchAlbum(album, artist).await().album
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }
}