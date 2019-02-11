package com.geckour.nowplaying4gpm.ui.widget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.databinding.ItemDialogListArtworkMethodBinding
import com.geckour.nowplaying4gpm.util.ArtworkResolveMethod
import com.geckour.nowplaying4gpm.util.swap

class ArtworkResolveMethodListAdapter(val items: MutableList<ArtworkResolveMethod>) :
    RecyclerView.Adapter<ArtworkResolveMethodListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemDialogListArtworkMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    private fun move(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices) return
        items.swap(from, to)
        notifyItemMoved(from, to)
    }

    inner class ViewHolder(private val binding: ItemDialogListArtworkMethodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.item = items[adapterPosition]
            binding.root.setOnClickListener { binding.enabled.performClick() }
            binding.enabled.setOnCheckedChangeListener { _, b ->
                val item = items[adapterPosition].copy(enabled = b)
                items[adapterPosition] = item
                binding.item = item
            }
        }
    }

    val itemTouchHolder =
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            var from: Int? = null
            var to: Int? = null

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                if (this.from == null) this.from = from
                this.to = to

                move(from, to)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        })
}