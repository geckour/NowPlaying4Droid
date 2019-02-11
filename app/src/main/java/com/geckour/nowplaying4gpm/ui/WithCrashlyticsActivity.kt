package com.geckour.nowplaying4gpm.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.geckour.nowplaying4gpm.util.setCrashlytics

abstract class WithCrashlyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCrashlytics()
    }
}