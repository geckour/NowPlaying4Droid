package com.geckour.nowplaying4droid.app.api

import android.graphics.Bitmap
import android.net.Uri
import twitter4j.CreateTweetResponse
import twitter4j.auth.AccessToken

interface TwitterApiService {

    suspend fun getRequestOAuthUri(): Uri?

    suspend fun getAccessToken(verifier: String): AccessToken?

    suspend fun post(
        accessToken: AccessToken,
        subject: String,
        artwork: Bitmap?,
        artworkTitle: String?
    ): CreateTweetResponse?
}