package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.Results
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.gildor.coroutines.retrofit.await

class ITunesApiClient {
    val service = Retrofit.Builder()
            .client(OkHttpProvider.client)
            .baseUrl("https://itunes.apple.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITunesApiService::class.java)

    suspend fun searchAlbum(title: String?, artist: String?, album: String?): Results = service.searchAlbum("$artist $album").await()
}