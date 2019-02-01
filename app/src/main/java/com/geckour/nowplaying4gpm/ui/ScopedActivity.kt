package com.geckour.nowplaying4gpm.ui

import android.app.Activity
import android.os.Bundle
import com.geckour.nowplaying4gpm.util.getExceptionHandler
import com.geckour.nowplaying4gpm.util.setCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class ScopedActivity : Activity(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + getExceptionHandler() + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setCrashlytics()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}