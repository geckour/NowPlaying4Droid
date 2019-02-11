package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.MastodonInstanceList
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface MastodonInstancesApiService {
    @GET("/api/1.0/instances/list/")
    fun getInstancesList(
        @Query("count") count: Int = 100,
        @Query("include_down") includeDown: Boolean = false,
        @Query("include_closed") includeClosed: Boolean = false,
        @Query("sort_by") sortBy: String = "users",
        @Query("sort_order") sortOrder: String = "desc"
    ): Deferred<MastodonInstanceList>
}