package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.app.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.app.util.FormatPattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class TrackInfoTest {

    private val emptyInfo = TrackInfo.empty
    private val fullInfo =
        TrackInfo(
            TrackInfo.TrackCoreElement("hoge", "fuga", "piyo", "nyan"),
            "baw", "com.pao", "Pao", "pyon")
    private val emptyCoreElement = emptyInfo.coreElement
    private val fullCoreElement = fullInfo.coreElement

    @Before
    fun setUp() {
    }

    @Test
    fun isSatisfiedSpecifier_givenNotEmptyPattern_withEmptyInfo_returnFalse() {
        val actual = emptyInfo.isSatisfiedSpecifier(FormatPattern.TITLE.value)
        assertThat(actual).isFalse()
    }

    @Test
    fun isSatisfiedSpecifier_givenEmptyPattern_withEmptyInfo_returnTrue() {
        val actual = emptyInfo.isSatisfiedSpecifier("")
        assertThat(actual).isTrue()
    }

    @Test
    fun isSatisfiedSpecifier_givenNotEmptyPattern_withFullInfo_returnTrue() {
        val actual = fullInfo.isSatisfiedSpecifier(FormatPattern.TITLE.value)
        assertThat(actual).isTrue()
    }

    @Test
    fun isSatisfiedSpecifier_givenEmptyPattern_withFullInfo_returnTrue() {
        val actual = fullInfo.isSatisfiedSpecifier("")
        assertThat(actual).isTrue()
    }

    @Test
    fun isSatisfiedSpecifier_givenNotEmptyPattern_withNotEmptyInfo_notMatch_returnFalse() {
        val info = TrackInfo.empty.copy(artworkUriString = "hoge")
        val actual = info.isSatisfiedSpecifier(FormatPattern.TITLE.value)
        assertThat(actual).isFalse()
    }

    @Test
    fun coreElement_isAllNonNull_withEmpty_returnFalse() {
        val actual = emptyCoreElement.isAllNonNull
        assertThat(actual).isFalse()
    }

    @Test
    fun coreElement_isAllNonNull_withNotEmpty_returnTrue() {
        val actual = fullCoreElement.isAllNonNull
        assertThat(actual).isTrue()
    }

    @Test
    fun coreElement_spotifySearchQuery_withEmpty_returnNull() {
        val actual = emptyCoreElement.spotifySearchQuery
        assertThat(actual).isNull()
    }

    @Test
    fun coreElement_spotifySearchQuery_withNotEmpty_returnNull() {
        val actual = TrackInfo.TrackCoreElement("hoge", null, null, null).spotifySearchQuery
        assertThat(actual).isNull()
    }

    @Test
    fun coreElement_spotifySearchQuery_withFull_returnNotNull() {
        val actual = fullCoreElement.spotifySearchQuery
        assertThat(actual).isEqualTo("track:\"${fullCoreElement.title}\" \"${fullCoreElement.artist}\" album:\"${fullCoreElement.album}\"")
    }

    @After
    fun tearDown() {
    }
}