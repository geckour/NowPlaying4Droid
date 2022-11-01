package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.api.model.MastodonInstance
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class MastodonInstancesApiClient {

    private val service = Retrofit.Builder()
        .client(OkHttpProvider.mastodonInstancesClient)
        .baseUrl("https://instances.social/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MastodonInstancesApiService::class.java)

    suspend fun getList(): List<MastodonInstance> =
        withCatching { service.getInstancesList().value ?: emptyList() } ?: emptyList()
}