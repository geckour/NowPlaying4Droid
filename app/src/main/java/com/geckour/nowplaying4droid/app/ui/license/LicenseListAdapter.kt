package com.geckour.nowplaying4droid.app.ui.license

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.geckour.nowplaying4droid.databinding.ItemLicenseBinding
import com.geckour.nowplaying4droid.databinding.ItemLicenseFooterBinding
import com.geckour.nowplaying4droid.app.ui.debug.DebugActivity
import com.geckour.nowplaying4droid.app.util.PrefKey
import com.geckour.nowplaying4droid.app.util.getDonateBillingState

class LicenseListAdapter :
    ListAdapter<LicenseListAdapter.LicenseItem, RecyclerView.ViewHolder>(diffUtil) {

    companion object {
        private const val easterEggIconUrl =
            "https://www.gravatar.com/avatar/0ad8003a07b699905aec7bb9097a2101?size=600"

        private val diffUtil = object : DiffUtil.ItemCallback<LicenseItem>() {

            override fun areItemsTheSame(oldItem: LicenseItem, newItem: LicenseItem): Boolean =
                oldItem.nameResId == newItem.nameResId

            override fun areContentsTheSame(oldItem: LicenseItem, newItem: LicenseItem): Boolean =
                oldItem == newItem
        }
    }

    enum class ViewType {
        NORMAL,
        FOOTER
    }

    data class LicenseItem(
        @StringRes val nameResId: Int,
        @StringRes val textResId: Int,
        var stateOpen: Boolean
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            ViewType.NORMAL.ordinal -> {
                NormalItemViewHolder(
                    ItemLicenseBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            ViewType.FOOTER.ordinal -> {
                FooterItemViewHolder(
                    ItemLicenseFooterBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> throw IllegalArgumentException()
        }

    override fun getItemCount(): Int = currentList.size + 1

    override fun getItemViewType(position: Int): Int =
        when (position) {
            currentList.size -> ViewType.FOOTER.ordinal
            else -> ViewType.NORMAL.ordinal
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalItemViewHolder -> holder.bind(currentList[holder.adapterPosition])
            is FooterItemViewHolder -> holder.bind()
        }
    }

    class NormalItemViewHolder(private val binding: ItemLicenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LicenseItem) {
            binding.item = item
            binding.executePendingBindings()
            binding.nameCover.setOnClickListener {
                item.stateOpen = item.stateOpen.not()
                binding.item = item
            }
        }
    }

    inner class FooterItemViewHolder(private val binding: ItemLicenseFooterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(binding.root.context)

        fun bind() {
            binding.buttonStart.setOnClickListener { openDebugActivity() }
            binding.buttonEnd.load(easterEggIconUrl)
            binding.buttonEnd.setOnClickListener { toggleDonateState() }
        }

        private fun openDebugActivity() {
            with(binding.root.context) { startActivity(DebugActivity.getIntent(this)) }
        }

        private fun toggleDonateState() {
            sharedPreferences.edit().putBoolean(
                PrefKey.PREF_KEY_BILLING_DONATE.name,
                sharedPreferences.getDonateBillingState().not()
            ).apply()
        }
    }
}