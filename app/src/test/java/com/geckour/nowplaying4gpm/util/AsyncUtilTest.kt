package com.geckour.nowplaying4gpm.util

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsyncUtilTest {

    private val sampleValidUrl = "https://www.gravatar.com/avatar/0ad8003a07b699905aec7bb9097a2101?size=600"

    @Before
    fun setUp() {
    }

    @Test
    fun asyncOrNull_thrownError_returnNull() {
        val actual = runBlocking { asyncOrNull<String?> { throw IllegalStateException() }.await() }
        assertThat(actual).isNull()
    }

    @Test
    fun getArtworkUriFromDevice_withEmptyInfo_returnNull() { // TODO: mock ContentProvider
    }

    @Test
    fun refreshArtworkUriFromLastFmApi() { // TODO: mock API response
    }

    @Test
    fun refreshArtworkUriFromBitmap() { // TODO: mock SharedPreference
    }

    @Test
    fun getBitmapFromUrl_withInvalidUrl_returnNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actual = runBlocking { getBitmapFromUrl(context, "") }
        assertThat(actual).isNull()
    }

    @Test
    fun getBitmapFromUrl_withValidUrl_returnNotNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actual = runBlocking { getBitmapFromUrl(context, sampleValidUrl) }
        assertThat(actual).isNotNull
    }

    @Test
    fun getBitmapFromUri_withEmptyUri_returnNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.EMPTY
        val actual = runBlocking { getBitmapFromUri(context, uri) }
        assertThat(actual).isNull()
    }

    @Test
    fun getBitmapFromUri_withValidUri_returnNotNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.parse(sampleValidUrl)
        val actual = runBlocking { getBitmapFromUri(context, uri) }
        assertThat(actual).isNotNull
    }

    @Test
    fun getBitmapFromUriString_withInvalidUrl_returnNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actual = runBlocking { getBitmapFromUriString(context, "") }
        assertThat(actual).isNull()
    }

    @Test
    fun getBitmapFromUriString_withValidUrl_returnNotNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actual = runBlocking { getBitmapFromUriString(context, sampleValidUrl) }
        assertThat(actual).isNotNull
    }

    @After
    fun tearDown() {
    }
}