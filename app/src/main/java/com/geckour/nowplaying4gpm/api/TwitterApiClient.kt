package com.geckour.nowplaying4gpm.api

import android.graphics.Bitmap
import android.net.Uri
import com.geckour.nowplaying4gpm.util.getUri
import timber.log.Timber
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

    override suspend fun getRequestOAuthUri(): Uri? {
        try {
            requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK)
        } catch (t: Throwable) {
            Timber.e(t)
        }
        return requestToken?.authorizationURL?.getUri()
    }

    override suspend fun getAccessToken(verifier: String): AccessToken? =
            try {
                if (requestToken == null) null
                else twitter.getOAuthAccessToken(requestToken, verifier)
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }

    override suspend fun post(accessToken: AccessToken,
                      subject: String, artwork: Bitmap?, artworkTitle: String?) =
            try {
                twitter.oAuthAccessToken = accessToken
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
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }
}