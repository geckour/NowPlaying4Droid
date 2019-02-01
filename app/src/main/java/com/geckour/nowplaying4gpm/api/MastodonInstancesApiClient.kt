package com.geckour.nowplaying4gpm.api

import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.api.model.MastodonInstance
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class MastodonInstancesApiClient {

    private val service = Retrofit.Builder()
            .client(OkHttpProvider.mastodonInstancesClient)
            .baseUrl("https://instances.social/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(MastodonInstancesApiService::class.java)

    suspend fun getList(): List<MastodonInstance> =
            try {
                service.getInstancesList().await().value
            } catch (t: Throwable) {
                Timber.e(t)
                Crashlytics.logException(t)
                emptyList()
            }
}