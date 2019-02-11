package com.geckour.nowplaying4gpm.ui.widget.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.databinding.ItemDialogListFormatPatternModifierBinding
import com.geckour.nowplaying4gpm.util.FormatPatternModifier

class FormatPatternModifierListAdapter(val items: MutableList<FormatPatternModifier>) :
    RecyclerView.Adapter<FormatPatternModifierListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemDialogListFormatPatternModifierBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    inner class ViewHolder(private val binding: ItemDialogListFormatPatternModifierBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.prefix.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    items[adapterPosition] = items[adapterPosition].copy(prefix = p0?.toString())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            })
            binding.suffix.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    items[adapterPosition] = items[adapterPosition].copy(suffix = p0?.toString())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            })
        }

        fun bind() {
            binding.item = items[adapterPosition]
            binding.prefix.setOnFocusChangeListener { v, _ ->
                v.post { (v as EditText).setSelection(v.text.length) }
            }
            binding.suffix.setOnFocusChangeListener { v, _ ->
                v.post { (v as EditText).setSelection(v.text.length) }
            }
        }
    }
}