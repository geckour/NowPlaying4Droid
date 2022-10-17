package com.geckour.nowplaying4gpm.app.ui.mushroom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.app.util.getSharingText

class MushroomActivity : Activity() {

    companion object {
        private const val ARGS_KEY_MUSHROOM = "replace_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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