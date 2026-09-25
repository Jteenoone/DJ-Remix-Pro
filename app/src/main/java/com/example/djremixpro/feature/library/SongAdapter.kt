package com.example.djremixpro.feature.library

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.databinding.ItemSongBinding

class SongAdapter(private val onClick: (String) -> Unit) : ListAdapter<SongRow, SongAdapter.Holder>(Diff) {

    class Holder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.binding.root.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onClick(getItem(position).id)
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val b = holder.binding
        val context = b.root.context
        b.textCover.text = row.initials
        b.textCover.backgroundTintList = ColorStateList.valueOf(row.coverColor)
        b.textTitle.text = row.title
        b.textSubtitle.text = row.subtitle
        b.textBpm.text = row.bpmLabel
        b.textBpm.setTextColor(context.colorOf(if (row.hasBpm) R.color.md_text else R.color.md_muted))
    }

    private object Diff : DiffUtil.ItemCallback<SongRow>() {
        override fun areItemsTheSame(oldItem: SongRow, newItem: SongRow) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SongRow, newItem: SongRow) = oldItem == newItem
    }
}
