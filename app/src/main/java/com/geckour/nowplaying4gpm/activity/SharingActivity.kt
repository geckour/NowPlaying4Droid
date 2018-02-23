package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ShareCompat
import com.geckour.nowplaying4gpm.R

class SharingActivity: Activity() {

    enum class ArgKey {
        TEXT,
        ALBUM_ART_URI
    }

    companion object {
        fun createIntent(context: Context, text: String, albumArtUri: Uri? = null): Intent =
                Intent(context, SharingActivity::class.java).apply {
                    putExtra(ArgKey.TEXT.name, text)
                    if (albumArtUri != null) putExtra(ArgKey.ALBUM_ART_URI.name, albumArtUri)
                }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let {
            ShareCompat.IntentBuilder.from(this).apply {
                setChooserTitle(R.string.share_title)
                if (it.hasExtra(ArgKey.TEXT.name)) setText(it.getStringExtra(ArgKey.TEXT.name))
                if (it.hasExtra(ArgKey.ALBUM_ART_URI.name)) {
                    setStream(it.extras[ArgKey.ALBUM_ART_URI.name] as Uri)
                    setType("image/jpeg")
                } else setType("text/plain")
            }.startChooser().apply { finish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
    }
}