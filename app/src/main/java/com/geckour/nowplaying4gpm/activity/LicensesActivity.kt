package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.adapter.LicenseListAdapter
import com.geckour.nowplaying4gpm.databinding.ActivityLicensesBinding
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric

class LicensesActivity: Activity() {

    companion object {
        fun getIntent(context: Context): Intent =
                Intent(context, LicensesActivity::class.java)
    }

    private lateinit var binding: ActivityLicensesBinding
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var adapter: LicenseListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG.not()) Fabric.with(this, Crashlytics())
        analytics = FirebaseAnalytics.getInstance(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_licenses)

        binding.toolbar.title = "OSSライセンス - ${getString(R.string.app_name)}"

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LicensesActivity, LinearLayoutManager.VERTICAL, false)
            this@LicensesActivity.adapter = LicenseListAdapter(
                    listOf(
                            Pair(getString(R.string.library_coroutines), getString(R.string.license_coroutines)),
                            Pair(getString(R.string.library_android_support), getString(R.string.license_android_support)),
                            Pair(getString(R.string.library_databinding), getString(R.string.license_databinding)),
                            Pair(getString(R.string.library_timber), getString(R.string.license_timber)),
                            Pair(getString(R.string.library_stetho), getString(R.string.license_stetho)),
                            Pair(getString(R.string.library_retrofit), getString(R.string.license_retrofit)),
                            Pair(getString(R.string.library_okhttp), getString(R.string.license_okhttp)),
                            Pair(getString(R.string.library_coroutines4retrofit), getString(R.string.license_coroutines4retrofit)),
                            Pair(getString(R.string.library_glide), getString(R.string.license_glide))
                    )
            )
            adapter = this@LicensesActivity.adapter
            adapter.notifyDataSetChanged()
            setHasFixedSize(true)
        }
    }
}