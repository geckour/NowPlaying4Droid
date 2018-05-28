package com.geckour.nowplaying4gpm.api

import android.graphics.Bitmap
import android.net.Uri
import twitter4j.auth.AccessToken

interface TwitterApiService {
    suspend fun getRequestOAuthUri(): Uri?
    suspend fun getAccessToken(verifier: String): AccessToken?
    fun post(accessToken: AccessToken, subject: String, artwork: Bitmap?, artworkTitle: String?)
}