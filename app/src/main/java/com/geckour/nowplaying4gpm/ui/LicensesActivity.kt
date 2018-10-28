package com.geckour.nowplaying4gpm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.ui.adapter.LicenseListAdapter
import com.geckour.nowplaying4gpm.databinding.ActivityLicensesBinding
import com.geckour.nowplaying4gpm.util.setCrashlytics

class LicensesActivity : Activity() {

    companion object {
        fun getIntent(context: Context): Intent =
                Intent(context, LicensesActivity::class.java)
    }

    private lateinit var binding: ActivityLicensesBinding
    private lateinit var adapter: LicenseListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_licenses)

        binding.toolbar.title = "${getString(R.string.activity_title_licenses)} - ${getString(R.string.app_name)}"

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LicensesActivity, LinearLayoutManager.VERTICAL, false)
            this@LicensesActivity.adapter = LicenseListAdapter(
                    listOf(
                            LicenseListAdapter.LicenseItem(getString(R.string.library_coroutines), getString(R.string.license_coroutines), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_android_support), getString(R.string.license_android_support), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_databinding), getString(R.string.license_databinding), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_timber), getString(R.string.license_timber), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_stetho), getString(R.string.license_stetho), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_retrofit), getString(R.string.license_retrofit), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_okhttp), getString(R.string.license_okhttp), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_coroutines4retrofit), getString(R.string.license_coroutines4retrofit), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_glide), getString(R.string.license_glide), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_twitter4j), getString(R.string.license_twitter4j), false),
                            LicenseListAdapter.LicenseItem(getString(R.string.library_gson), getString(R.string.license_gson), false)
                    )
            )
            adapter = this@LicensesActivity.adapter
            this@LicensesActivity.adapter.notifyDataSetChanged()
            setHasFixedSize(true)
        }
    }
}