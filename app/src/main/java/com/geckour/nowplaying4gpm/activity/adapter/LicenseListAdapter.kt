package com.geckour.nowplaying4gpm.activity.adapter

import android.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.databinding.ItemLicenseBinding
import com.geckour.nowplaying4gpm.databinding.ItemLicenseFooterBinding
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.getDonateBillingState
import com.geckour.nowplaying4gpm.util.ui
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class LicenseListAdapter(private val items: List<LicenseItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ViewType {
        NORMAL,
        FOOTER
    }

    data class LicenseItem(
            val name: String,
            val text: String,
            var stateOpen: Boolean
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                ViewType.NORMAL.ordinal -> {
                    NormalItemViewHolder(
                            ItemLicenseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }
                ViewType.FOOTER.ordinal -> {
                    FooterItemViewHolder(
                            ItemLicenseFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }
                else -> throw IllegalArgumentException()
            }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int =
            when (position) {
                items.size -> ViewType.FOOTER.ordinal
                else -> ViewType.NORMAL.ordinal
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalItemViewHolder -> holder.bind(items[holder.adapterPosition])
            is FooterItemViewHolder -> holder.bind()
        }
    }

    private val jobs: ArrayList<Job> = ArrayList()

    class NormalItemViewHolder(val binding: ItemLicenseBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LicenseItem) {
            binding.item = item
            binding.executePendingBindings()
            binding.nameCover.setOnClickListener {
                item.stateOpen = item.stateOpen.not()
                binding.item = item
            }
        }
    }

    inner class FooterItemViewHolder(val binding: ItemLicenseFooterBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            ui(jobs) { Glide.with(binding.button).load(binding.button.context.getString(R.string.easter_egg_icon_url)).into(binding.button) }
            binding.buttonCover.setOnClickListener { toggleDonateState() }
        }

        private fun toggleDonateState() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(binding.root.context)

            sharedPreferences.edit().putBoolean(
                    PrefKey.PREF_KEY_BILLING_DONATE.name,
                    sharedPreferences.getDonateBillingState().not()
            ).apply()
        }
    }
}