package com.geckour.nowplaying4gpm.ui.license

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ActivityLicensesBinding
import com.geckour.nowplaying4gpm.ui.WithCrashlyticsActivity

class LicensesActivity : WithCrashlyticsActivity() {

    companion object {

        fun getIntent(context: Context): Intent =
            Intent(context, LicensesActivity::class.java)
    }

    private lateinit var binding: ActivityLicensesBinding
    private val viewModel: LicenseViewModel by viewModels()
    private lateinit var adapter: LicenseListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_licenses)

        binding.toolbarTitle =
            "${getString(R.string.activity_title_licenses)} - ${getString(R.string.app_name)}"
        setSupportActionBar(binding.toolbar)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LicensesActivity, RecyclerView.VERTICAL, false)
            this@LicensesActivity.adapter = LicenseListAdapter(viewModel)
            adapter = this@LicensesActivity.adapter
            this@LicensesActivity.adapter.notifyDataSetChanged()
            setHasFixedSize(true)
        }
    }
}