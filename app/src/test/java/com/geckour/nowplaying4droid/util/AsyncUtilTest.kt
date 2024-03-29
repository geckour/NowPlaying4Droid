package com.geckour.nowplaying4droid.util

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.geckour.nowplaying4droid.app.util.PrefKey
import com.geckour.nowplaying4droid.app.util.withCatching
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsyncUtilTest {

    private val sampleValidUrl =
        "https://www.gravatar.com/avatar/0ad8003a07b699905aec7bb9097a2101?size=600"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun asyncOrNull_thrownError_returnNull() {
        val actual = withCatching<String?> { throw IllegalStateException() }
        assertThat(actual).isNull()
    }

    @Test
    fun getArtworkUriFromDevice_withEmptyInfo_returnNull() { // TODO: mock ContentProvider
    }

    @Test
    fun refreshArtworkUriFromLastFmApi() { // TODO: mock API response
    }

    @Test
    fun containsArtworkUri_beforeSet_returnFalse() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val actual = sharedPreferences.contains(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name)
        assertThat(actual).isFalse()
    }

    @Test
    fun containsArtworkUri_afterRefreshArtworkUriFromBitmap_returnTrue() { // TODO: mock FileProvider
    }

    @Test
    fun getBitmapFromUrl_withInvalidUrl_returnNull() {
    }

    @Test
    fun getBitmapFromUrl_withValidUrl_returnNotNull() {
    }

    @Test
    fun getBitmapFromUri_withEmptyUri_returnNull() {
    }

    @Test
    fun getBitmapFromUri_withValidUri_returnNotNull() {
    }

    @Test
    fun getBitmapFromUriString_withInvalidUrl_returnNull() {
    }

    @Test
    fun getBitmapFromUriString_withValidUrl_returnNotNull() {
    }

    @After
    fun tearDown() {
    }
}