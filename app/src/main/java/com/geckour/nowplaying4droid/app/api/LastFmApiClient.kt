package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.api.model.Album
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.withCatching
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class LastFmApiClient {
    private val service = Retrofit.Builder()
        .client(OkHttpProvider.client)
        .baseUrl("http://ws.audioscrobbler.com/")
        .addConverterFactory(
            json.asConverterFactory("application/json; charset=UTF8".toMediaType())
        )
        .build()
        .create(LastFmApiService::class.java)

    suspend fun searchAlbum(album: String?, artist: String?): Album? =
        if (album == null || artist == null) null
        else withCatching { service.searchAlbum(album, artist).album }
}