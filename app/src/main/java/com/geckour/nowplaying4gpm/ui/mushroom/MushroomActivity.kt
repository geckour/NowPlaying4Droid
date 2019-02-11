package com.geckour.nowplaying4gpm.ui.mushroom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import com.geckour.nowplaying4gpm.util.getSharingText
import com.geckour.nowplaying4gpm.util.setCrashlytics

class MushroomActivity : Activity() {

    private val ARGS_KEY_MUSHROOM = "replace_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        val subject = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getSharingText(this)
        if (subject != null) {
            setResult(RESULT_OK, Intent().apply {
                putExtra(ARGS_KEY_MUSHROOM, subject)
            })
        }

        finish()
    }
}