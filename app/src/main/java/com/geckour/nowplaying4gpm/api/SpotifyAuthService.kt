package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.SpotifyToken
import kotlinx.coroutines.Deferred
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SpotifyAuthService {
    @FormUrlEncoded
    @POST("/api/token")
    fun getToken(
        @Field("grant_type")
        grantType: String = "client_credentials"
    ): Deferred<SpotifyToken>
}