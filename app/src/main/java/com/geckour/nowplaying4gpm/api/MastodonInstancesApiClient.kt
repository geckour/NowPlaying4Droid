package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.MastodonInstance
import com.geckour.nowplaying4gpm.util.moshi
import com.geckour.nowplaying4gpm.util.withCatching
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MastodonInstancesApiClient {

    private val service = Retrofit.Builder()
        .client(OkHttpProvider.mastodonInstancesClient)
        .baseUrl("https://instances.social/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MastodonInstancesApiService::class.java)

    suspend fun getList(): List<MastodonInstance> =
        withCatching { service.getInstancesList().value ?: emptyList() } ?: emptyList()
}