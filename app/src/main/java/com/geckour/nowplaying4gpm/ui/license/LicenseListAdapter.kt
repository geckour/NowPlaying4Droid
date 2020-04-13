package com.geckour.nowplaying4gpm.ui.license

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.geckour.nowplaying4gpm.databinding.ItemLicenseBinding
import com.geckour.nowplaying4gpm.databinding.ItemLicenseFooterBinding
import com.geckour.nowplaying4gpm.ui.debug.DebugActivity
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.getDonateBillingState

class LicenseListAdapter(private val viewModel: LicenseViewModel) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val easterEggIconUrl =
            "https://www.gravatar.com/avatar/0ad8003a07b699905aec7bb9097a2101?size=600"
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

    override fun getItemCount(): Int = viewModel.listItems.size + 1

    override fun getItemViewType(position: Int): Int =
        when (position) {
            viewModel.listItems.size -> ViewType.FOOTER.ordinal
            else -> ViewType.NORMAL.ordinal
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalItemViewHolder -> holder.bind(viewModel.listItems[holder.adapterPosition])
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
            Glide.with(binding.buttonEnd)
                .load(easterEggIconUrl)
                .into(binding.buttonEnd)
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