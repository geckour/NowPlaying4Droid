package com.geckour.nowplaying4droid.app.domain.model

import org.junit.Test
import kotlin.test.assertEquals

internal class TrackDetailTest {

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 1`() {
        val actual = TrackDetail.empty.coreElement.appleMusicSearchQuery
        assertEquals("", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 2`() {
        val actual = TrackDetail.empty.coreElement.copy(title = "title").appleMusicSearchQuery
        assertEquals("title", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 3`() {
        val actual = TrackDetail.empty.coreElement.copy(artist = "artist").appleMusicSearchQuery
        assertEquals("artist", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 4`() {
        val actual = TrackDetail.empty.coreElement.copy(album = "album").appleMusicSearchQuery
        assertEquals("album", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 5`() {
        val actual = TrackDetail.empty.coreElement.copy(title = "abc def").appleMusicSearchQuery
        assertEquals("abc+def", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 6`() {
        val actual = TrackDetail.empty.coreElement
            .copy(title = "abc def", album = "ghi")
            .appleMusicSearchQuery
        assertEquals("abc+def+ghi", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 7`() {
        val actual = TrackDetail.empty.coreElement.copy(title = "abc+def").appleMusicSearchQuery
        assertEquals("abc+def", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 8`() {
        val actual = TrackDetail.empty.coreElement.copy(title = " abc def ").appleMusicSearchQuery
        assertEquals("abc+def", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 9`() {
        val actual = TrackDetail.empty.coreElement.copy(title = "abc     def").appleMusicSearchQuery
        assertEquals("abc+def", actual)
    }

    @Test
    fun `TrackCoreElement#appleMusicSearchQuery 10`() {
        val actual = TrackDetail.empty.coreElement.copy(title = "abc　def").appleMusicSearchQuery
        assertEquals("abc　def", actual)
    }
}