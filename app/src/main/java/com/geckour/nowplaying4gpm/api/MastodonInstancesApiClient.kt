package com.geckour.nowplaying4gpm.api

import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.api.model.MastodonInstance
import com.geckour.nowplaying4gpm.util.moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class MastodonInstancesApiClient {

    private val service = Retrofit.Builder()
        .client(OkHttpProvider.mastodonInstancesClient)
        .baseUrl("https://instances.social/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MastodonInstancesApiService::class.java)

    suspend fun getList(): List<MastodonInstance> =
        try {
            service.getInstancesList().value ?: emptyList()
        } catch (t: Throwable) {
            Timber.e(t)
            Crashlytics.logException(t)
            emptyList()
        }
}