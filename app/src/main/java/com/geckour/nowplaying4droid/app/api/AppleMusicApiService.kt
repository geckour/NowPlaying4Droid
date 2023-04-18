package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.api.model.AppleMusicArtistsResult
import com.geckour.nowplaying4droid.app.api.model.AppleMusicSearchData
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AppleMusicApiService {

    @GET("/v1/test")
    suspend fun test()

    @GET("/v1/catalog/{storefront}/search")
    suspend fun searchAppleMusicItem(
        @Path("storefront")
        countryCode: String,

        @Query("term")
        query: String,

        @Query("limit")
        limit: Int = 1,

        @Query("types")
        types: List<String> = listOf("songs"),
    ): AppleMusicSearchData

    @GET("/v1/catalog/{storefront}/songs/{id}")
    suspend fun getAppleMusicSong(
        @Path("storefront")
        countryCode: String,

        @Path("id")
        songId: String,

        @Query("include")
        include: List<String> = listOf("artist"),
    )

    @GET("/v1/catalog/{storefront}/artists")
    suspend fun getAppleMusicArtists(
        @Path("storefront")
        countryCode: String,

        @Query("ids")
        artistIds: List<String>,

        @Query("include")
        include: List<String> = emptyList(),
    ): AppleMusicArtistsResult
}