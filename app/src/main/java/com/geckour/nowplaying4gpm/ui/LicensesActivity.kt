package com.geckour.nowplaying4gpm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ActivityLicensesBinding
import com.geckour.nowplaying4gpm.ui.adapter.LicenseListAdapter

class LicensesActivity : ScopedActivity() {

    companion object {
        fun getIntent(context: Context): Intent =
                Intent(context, LicensesActivity::class.java)
    }

    private lateinit var binding: ActivityLicensesBinding
    private lateinit var adapter: LicenseListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_licenses)

        binding.toolbar.title = "${getString(R.string.activity_title_licenses)} - ${getString(R.string.app_name)}"

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LicensesActivity, RecyclerView.VERTICAL, false)
            this@LicensesActivity.adapter = LicenseListAdapter(listOf(
                    LicenseListAdapter.LicenseItem(getString(R.string.library_coroutines), getString(R.string.license_coroutines), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_androidx), getString(R.string.license_androidx), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_databinding), getString(R.string.license_databinding), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_timber), getString(R.string.license_timber), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_stetho), getString(R.string.license_stetho), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_retrofit), getString(R.string.license_retrofit), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_okhttp), getString(R.string.license_okhttp), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_coroutines4retrofit), getString(R.string.license_coroutines4retrofit), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_glide), getString(R.string.license_glide), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_twitter4j), getString(R.string.license_twitter4j), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_gson), getString(R.string.license_gson), false),
                    LicenseListAdapter.LicenseItem(getString(R.string.library_mastodon4j), getString(R.string.license_mastodon4j), false)
            ))
            adapter = this@LicensesActivity.adapter
            this@LicensesActivity.adapter.notifyDataSetChanged()
            setHasFixedSize(true)
        }
    }
}