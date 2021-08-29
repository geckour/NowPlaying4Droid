package com.geckour.nowplaying4gpm.ui.di

import com.geckour.nowplaying4gpm.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val settingsViewModelModule = module {
    viewModel { SettingsViewModel(androidApplication(), get(), get()) }
}