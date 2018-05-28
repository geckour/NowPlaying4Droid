package com.geckour.nowplaying4gpm.api

import android.graphics.Bitmap
import android.net.Uri
import com.geckour.nowplaying4gpm.util.async
import com.geckour.nowplaying4gpm.util.getUri
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TwitterApiClient(consumerKey: String, consumerSecret: String) : TwitterApiService {

    companion object {
        const val TWITTER_CALLBACK = "np4gpm://twitter.callback"
    }

    private val twitter: Twitter = TwitterFactory().instance.apply {
        setOAuthConsumer(consumerKey, consumerSecret)
    }

    private var requestToken: RequestToken? = null

    override suspend fun getRequestOAuthUri(): Uri? =
            async {
                requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK)
                requestToken?.authorizationURL?.getUri()
            }.await()

    override suspend fun getAccessToken(verifier: String): AccessToken? =
            if (requestToken == null) null
            else async { twitter.getOAuthAccessToken(requestToken, verifier) }.await()

    override fun post(accessToken: AccessToken, subject: String, artwork: Bitmap?, artworkTitle: String?) {
        twitter.oAuthAccessToken = accessToken

        async {
            val status = StatusUpdate(subject)
                    .apply {
                        if (artwork != null) {
                            val bytes =
                                    ByteArrayOutputStream().apply {
                                        artwork.compress(Bitmap.CompressFormat.JPEG, 100, this)
                                    }.toByteArray()
                            setMedia(artworkTitle, ByteArrayInputStream(bytes))
                        }
                    }
            twitter.updateStatus(status)
        }
    }
}