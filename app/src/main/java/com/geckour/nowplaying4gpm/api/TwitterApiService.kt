package com.geckour.nowplaying4gpm.api

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Deferred
import twitter4j.Status
import twitter4j.auth.AccessToken

interface TwitterApiService {
    suspend fun getRequestOAuthUri(): Uri?
    suspend fun getAccessToken(verifier: String): AccessToken?
    suspend fun post(accessToken: AccessToken, subject: String, artwork: Bitmap?, artworkTitle: String?): Status?
}