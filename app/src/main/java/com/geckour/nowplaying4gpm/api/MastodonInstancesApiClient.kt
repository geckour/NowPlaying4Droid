package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.MastodonInstance
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MastodonInstancesApiClient {

    private val service = Retrofit.Builder()
            .client(OkHttpProvider.mastodonInstancesClient)
            .baseUrl("https://instances.social/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(MastodonInstancesApiService::class.java)

    suspend fun getList(): List<MastodonInstance> = service.getInstancesList().await().value
}