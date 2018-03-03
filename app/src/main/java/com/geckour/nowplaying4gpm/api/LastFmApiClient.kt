package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.Album
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.gildor.coroutines.retrofit.await

class LastFmApiClient {
    private val service = Retrofit.Builder()
            .client(OkHttpProvider.client)
            .baseUrl("http://ws.audioscrobbler.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApiService::class.java)

    suspend fun searchAlbum(album: String?, artist: String?): Album? =
            if (album == null || artist == null) null
            else service.searchAlbum(album, artist).await().album
}