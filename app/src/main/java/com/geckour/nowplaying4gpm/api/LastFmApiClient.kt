package com.geckour.nowplaying4gpm.api

import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.api.model.Album
import com.geckour.nowplaying4gpm.util.moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class LastFmApiClient {
    private val service = Retrofit.Builder()
        .client(OkHttpProvider.client)
        .baseUrl("http://ws.audioscrobbler.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(LastFmApiService::class.java)

    suspend fun searchAlbum(album: String?, artist: String?): Album? =
        if (album == null || artist == null) null
        else try {
            service.searchAlbum(album, artist).album
        } catch (t: Throwable) {
            Timber.e(t)
            Crashlytics.logException(t)
            null
        }
}