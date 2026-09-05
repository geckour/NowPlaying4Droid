package com.geckour.nowplaying4droid.app.util

import android.content.Context
import android.media.session.MediaSession
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.MediaMetadata as Media3MediaMetadata

/**
 * Media3 collapses the whole release date into a single year when it publishes metadata to the
 * platform [android.media.session.MediaSession]: its `LegacyConversions` maps only `recordingYear`
 * onto `METADATA_KEY_YEAR`, and never writes `METADATA_KEY_DATE`. So `releaseMonth` / `releaseDay`
 * set by a media3 based player are unreachable through [android.media.MediaMetadata].
 *
 * Connecting with a media3 [MediaController] instead gets the metadata over the media3 protocol,
 * where those fields survive. [SessionToken.createSessionToken] takes the platform token we already
 * hold and transparently upgrades it to the media3 protocol when the session turns out to be a
 * media3 one.
 */

/**
 * Players last seen exposing a platform-only session, so media3 has nothing extra to offer, kept
 * against the time we saw that. [SessionToken.createSessionToken] silently falls back to a legacy
 * token when the session does not answer within 500ms, so a player that was merely slow to start
 * must not be written off for good - the entry expires and gets probed again.
 */
private val platformOnlyPlayers: MutableMap<String, Long> =
    Collections.synchronizedMap(mutableMapOf())

private const val RESOLVE_TIMEOUT_MS = 3000L
private const val PLATFORM_ONLY_RETRY_INTERVAL_MS = 5 * 60 * 1000L

/**
 * Reads the release date of the currently playing item over the media3 protocol.
 *
 * Returns `null` whenever media3 cannot add anything - the session is a plain platform one, the
 * connection is refused or times out, or the player simply did not set a release date. Callers are
 * expected to fall back to [android.media.MediaMetadata]'s own value in that case.
 */
@OptIn(markerClass = [UnstableApi::class])
suspend fun Context.getReleasedAtViaMedia3(
    sessionToken: MediaSession.Token?,
    playerPackageName: String
): String? {
    sessionToken ?: return null
    if (playerPackageName.isKnownPlatformOnly()) return null

    // A media3 MediaController is bound to its application looper, so build, read and release it there.
    return withContext(Dispatchers.Main) {
        var controller: MediaController? = null
        try {
            withTimeoutOrNull(RESOLVE_TIMEOUT_MS.milliseconds) {
                val token = SessionToken.createSessionToken(
                    this@getReleasedAtViaMedia3,
                    sessionToken
                ).await()

                if (token.sessionVersion == SessionToken.PLATFORM_SESSION_VERSION) {
                    platformOnlyPlayers[playerPackageName] = SystemClock.elapsedRealtime()
                    return@withTimeoutOrNull null
                }

                val connected = MediaController.Builder(this@getReleasedAtViaMedia3, token)
                    .buildAsync()
                    .await()
                controller = connected

                connected.mediaMetadata.releasedAt
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Timber.e(t)
            null
        } finally {
            controller?.release()
        }
    }
}

/** Formats media3's split date fields as ISO 8601, matching what the Spotify / Apple Music data uses. */
private val Media3MediaMetadata.releasedAt: String?
    get() = formatReleasedAt(releaseYear, releaseMonth, releaseDay)
        ?: formatReleasedAt(recordingYear, recordingMonth, recordingDay)

private fun String.isKnownPlatformOnly(): Boolean {
    val seenAt = platformOnlyPlayers[this] ?: return false
    if (SystemClock.elapsedRealtime() - seenAt < PLATFORM_ONLY_RETRY_INTERVAL_MS) return true

    platformOnlyPlayers -= this
    return false
}

private fun formatReleasedAt(year: Int?, month: Int?, day: Int?): String? {
    year ?: return null

    return buildString {
        append(String.format(Locale.ROOT, "%04d", year))
        month ?: return@buildString
        append(String.format(Locale.ROOT, "-%02d", month))
        day ?: return@buildString
        append(String.format(Locale.ROOT, "-%02d", day))
    }
}

private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                runCatching { get() }
                    .onSuccess { continuation.resume(it) }
                    .onFailure { continuation.resumeWithException(it) }
            },
            { it.run() }
        )
        // Cancelling a MediaController future releases the controller it was about to hand over.
        continuation.invokeOnCancellation { cancel(false) }
    }
