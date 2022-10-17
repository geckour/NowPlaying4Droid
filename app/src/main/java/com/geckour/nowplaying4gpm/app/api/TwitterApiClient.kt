package com.geckour.nowplaying4gpm.app.api

import android.graphics.Bitmap
import android.net.Uri
import com.geckour.nowplaying4gpm.app.util.getUri
import com.geckour.nowplaying4gpm.app.util.withCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import twitter4j.createTweet
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

    override suspend fun getRequestOAuthUri(): Uri? = withContext(Dispatchers.IO) {
        withCatching { requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK) }
        return@withContext requestToken?.authorizationURL?.getUri()
    }

    override suspend fun getAccessToken(verifier: String): AccessToken? =
        withContext(Dispatchers.IO) {
            withCatching { requestToken?.let { twitter.getOAuthAccessToken(it, verifier) } }
        }

    override suspend fun post(
        accessToken: AccessToken,
        subject: String, artwork: Bitmap?, artworkTitle: String?
    ) = withContext(Dispatchers.IO) {
        withCatching {
            twitter.oAuthAccessToken = accessToken
            twitter.createTweet(
                text = subject,
                mediaIds = artwork?.let {
                    val bytes =
                        ByteArrayOutputStream().apply {
                            it.compress(Bitmap.CompressFormat.JPEG, 100, this)
                        }.toByteArray()
                    val media = twitter.uploadMedia(artworkTitle, ByteArrayInputStream(bytes))
                    arrayOf(media.mediaId)
                }
            )
        }
    }
}