package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.Album
import com.geckour.nowplaying4gpm.util.json
import com.geckour.nowplaying4gpm.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class LastFmApiClient {
    private val service = Retrofit.Builder()
        .client(OkHttpProvider.client)
        .baseUrl("http://ws.audioscrobbler.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(LastFmApiService::class.java)

    suspend fun searchAlbum(album: String?, artist: String?): Album? =
        if (album == null || artist == null) null
        else withCatching { service.searchAlbum(album, artist).album }
}