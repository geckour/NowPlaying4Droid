package com.geckour.nowplaying4droid.app.ui.license

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.databinding.ActivityLicensesBinding

class LicensesActivity : AppCompatActivity() {

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

        adapter = LicenseListAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LicensesActivity, RecyclerView.VERTICAL, false)
            adapter = this@LicensesActivity.adapter
            this@LicensesActivity.adapter.notifyDataSetChanged()
            setHasFixedSize(true)
        }
        adapter.submitList(viewModel.listItems)
    }
}