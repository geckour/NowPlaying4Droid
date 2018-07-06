package com.geckour.nowplaying4gpm.api

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.experimental.Deferred
import twitter4j.Status
import twitter4j.auth.AccessToken

interface TwitterApiService {
    fun getRequestOAuthUri(): Deferred<Uri?>
    fun getAccessToken(verifier: String): Deferred<AccessToken?>
    fun post(accessToken: AccessToken, subject: String, artwork: Bitmap?, artworkTitle: String?): Deferred<Status?>
}