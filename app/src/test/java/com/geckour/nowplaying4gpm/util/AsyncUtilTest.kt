package com.geckour.nowplaying4gpm.util

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsyncUtilTest {

    private val sampleValidUrl =
        "https://www.gravatar.com/avatar/0ad8003a07b699905aec7bb9097a2101?size=600"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Fabric.with(context, Crashlytics())
    }

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
    fun getBitmapFromUrl_withInvalidUrl_returnNull() { // TODO: mock Glide
    }

    @Test
    fun getBitmapFromUrl_withValidUrl_returnNotNull() { // TODO: mock Glide
    }

    @Test
    fun getBitmapFromUri_withEmptyUri_returnNull() { // TODO: mock Glide
    }

    @Test
    fun getBitmapFromUri_withValidUri_returnNotNull() { // TODO: mock Glide
    }

    @Test
    fun getBitmapFromUriString_withInvalidUrl_returnNull() { // TODO: mock Glide
    }

    @Test
    fun getBitmapFromUriString_withValidUrl_returnNotNull() { // TODO: mock Glide
    }

    @After
    fun tearDown() {
    }
}