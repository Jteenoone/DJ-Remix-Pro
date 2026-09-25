package com.example.djremixpro.feature.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.core.model.Tip
import com.example.djremixpro.databinding.ItemTipBinding

/** Tip cards are read-only (App:180). */
class TipAdapter : ListAdapter<Tip, TipAdapter.Holder>(Diff) {

    class Holder(val binding: ItemTipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemTipBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tip = getItem(position)
        val b = holder.binding
        b.textTag.text = tip.tag
        b.textReadTime.text = tip.readTime
        b.textTitle.text = tip.title
        b.textBody.text = tip.body
    }

    private object Diff : DiffUtil.ItemCallback<Tip>() {
        override fun areItemsTheSame(oldItem: Tip, newItem: Tip) = oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: Tip, newItem: Tip) = oldItem == newItem
    }
}
