package com.geckour.nowplaying4droid.app.ui.di

import androidx.preference.PreferenceManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
}