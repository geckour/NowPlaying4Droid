package com.geckour.nowplaying4gpm.app.ui.debug

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ItemDebugMenuBinding

class DebugMenuListAdapter(private val onItemClick: (debugMenu: DebugMenu, summaryView: TextView) -> Unit) :
    ListAdapter<DebugMenuListAdapter.DebugMenuItem, DebugMenuListAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DebugMenuItem>() {

            override fun areItemsTheSame(oldItem: DebugMenuItem, newItem: DebugMenuItem): Boolean =
                oldItem.debugMenu == newItem.debugMenu

            override fun areContentsTheSame(
                oldItem: DebugMenuItem,
                newItem: DebugMenuItem
            ): Boolean = oldItem == newItem
        }
    }

    data class DebugMenuItem(
        val debugMenu: DebugMenu,
        val initialSummary: String? = null
    )

    enum class DebugMenu(@StringRes val titleRes: Int) {
        TOGGLE_SPOTIFY_SEARCH_DEBUG(R.string.debug_spotify_show_search_result),
        REFRESH_SPOTIFY_TOKEN(R.string.debug_spotify_refresh_token)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemDebugMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(getItem(position), item.initialSummary)
    }

    class ViewHolder(
        private val binding: ItemDebugMenuBinding,
        private val onItemClick: (debugMenu: DebugMenu, summaryView: TextView) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DebugMenuItem, initialSummary: String? = null) {
            binding.item = item.debugMenu
            if (binding.summary.text.isNullOrEmpty()) {
                binding.initialSummary = initialSummary
            }
            binding.executePendingBindings()
            binding.root.setOnClickListener { onItemClick(item.debugMenu, binding.summary) }
        }
    }
}