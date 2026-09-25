package com.example.djremixpro.feature.mixer.view

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.R
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.databinding.ItemLibraryPanelRowBinding
import com.example.djremixpro.feature.mixer.LibraryPanelRow

/** Rows of the mixer library panel (Mixer:342-356). A BPM match with Deck A gets the amber badge + "Hợp nhịp A". */
internal class LibraryPanelAdapter(
    private val onLoad: (trackId: String, deck: DeckId) -> Unit,
) : ListAdapter<LibraryPanelRow, LibraryPanelAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLibraryPanelRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(ItemLibraryPanelRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.binding.buttonLoadA.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onLoad(getItem(it).trackId, DeckId.A) }
        }
        holder.binding.buttonLoadB.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onLoad(getItem(it).trackId, DeckId.B) }
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
        b.textMeta.text = context.getString(R.string.song_meta, row.artist, row.durationLabel)
        b.textBpm.text = row.bpmLabel
        val hasBpm = row.bpmLabel != context.getString(R.string.bpm_unknown)
        val fg = when {
            row.isBpmMatch -> context.colorOf(R.color.md_booth)
            hasBpm -> context.colorOf(R.color.md_text)
            else -> context.colorOf(R.color.md_muted)
        }
        b.layoutBadge.setBackgroundResource(if (row.isBpmMatch) R.drawable.bg_badge_bpm_match else R.drawable.bg_badge_bpm_booth)
        b.textBpm.setTextColor(fg)
        b.textBpmUnit.setTextColor(fg)
        b.textMatch.isVisible = row.isBpmMatch
    }

    private object Diff : DiffUtil.ItemCallback<LibraryPanelRow>() {
        override fun areItemsTheSame(oldItem: LibraryPanelRow, newItem: LibraryPanelRow) = oldItem.trackId == newItem.trackId
        override fun areContentsTheSame(oldItem: LibraryPanelRow, newItem: LibraryPanelRow) = oldItem == newItem
    }
}
