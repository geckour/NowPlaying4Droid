package com.geckour.nowplaying4gpm.util

import kotlinx.coroutines.experimental.Job

interface JobHandler {
    val job: Job
}