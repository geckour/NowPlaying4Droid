package com.geckour.nowplaying4gpm.app.api

import com.geckour.nowplaying4gpm.app.api.model.MastodonInstanceList
import retrofit2.http.GET
import retrofit2.http.Query

interface MastodonInstancesApiService {
    @GET("/api/1.0/instances/list/")
    suspend fun getInstancesList(
        @Query("count") count: Int = 100,
        @Query("include_down") includeDown: Boolean = false,
        @Query("include_closed") includeClosed: Boolean = false,
        @Query("sort_by") sortBy: String = "users",
        @Query("sort_order") sortOrder: String = "desc"
    ): MastodonInstanceList
}