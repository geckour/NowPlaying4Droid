package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.Album
import com.geckour.nowplaying4gpm.util.moshi
import com.geckour.nowplaying4gpm.util.withCatching
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class LastFmApiClient {
    private val service = Retrofit.Builder()
        .client(OkHttpProvider.client)
        .baseUrl("http://ws.audioscrobbler.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(LastFmApiService::class.java)

    suspend fun searchAlbum(album: String?, artist: String?): Album? =
        if (album == null || artist == null) null
        else withCatching { service.searchAlbum(album, artist).album }
}