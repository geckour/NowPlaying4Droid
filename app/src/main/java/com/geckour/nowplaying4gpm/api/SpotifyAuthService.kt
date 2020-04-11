package com.geckour.nowplaying4gpm.api

import com.geckour.nowplaying4gpm.api.model.SpotifyToken
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SpotifyAuthService {
    @FormUrlEncoded
    @POST("/api/token")
    suspend fun getToken(
        @Field("code")
        code: String,

        @Field("redirect_uri")
        redirectUri: String = SpotifyApiClient.SPOTIFY_CALLBACK,

        @Field("grant_type")
        grantType: String = "authorization_code"
    ): SpotifyToken
}