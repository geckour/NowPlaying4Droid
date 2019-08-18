package com.geckour.nowplaying4gpm.ui.widget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.databinding.ItemDialogListPlayerPackageBinding
import com.geckour.nowplaying4gpm.util.PlayerPackageState

class PlayerPackageListAdapter(private val items: List<PlayerPackageState>) :
    RecyclerView.Adapter<PlayerPackageListAdapter.ViewHolder>() {

    private val modifiedItems = items.toMutableList()

    val itemsDiff get() = modifiedItems - items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemDialogListPlayerPackageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    inner class ViewHolder(private val binding: ItemDialogListPlayerPackageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.item = items[adapterPosition]
            binding.root.setOnClickListener { binding.enabled.performClick() }
            binding.enabled.setOnCheckedChangeListener { _, b ->
                val item = items[adapterPosition].copy(state = b)
                modifiedItems[adapterPosition] = item
                binding.item = item
            }
        }
    }
}