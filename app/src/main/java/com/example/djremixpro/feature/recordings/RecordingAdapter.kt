package com.example.djremixpro.feature.recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.R
import com.example.djremixpro.databinding.ItemRecordingBinding

class RecordingAdapter(
    private val onRowClick: (String) -> Unit,
    private val onMenuClick: (String) -> Unit,
) : ListAdapter<RecordingRow, RecordingAdapter.Holder>(Diff) {

    class Holder(val binding: ItemRecordingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(ItemRecordingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.binding.buttonPlay.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onRowClick(getItem(it).id) }
        }
        holder.binding.buttonMenu.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onMenuClick(getItem(it).id) }
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val b = holder.binding
        val context = b.root.context
        b.layoutRow.setBackgroundResource(if (row.isPlaying) R.drawable.bg_panel_r12 else 0)
        b.wave.setSeed(row.waveSeed)
        b.wave.setActive(row.isPlaying)
        b.textName.text = row.name
        b.textMeta.text = row.meta.resolve(context)
        b.buttonPlay.contentDescription = context.getString(
            if (row.isPlaying) R.string.recording_pause_cd else R.string.recording_listen_cd,
            row.name,
        )
    }

    private object Diff : DiffUtil.ItemCallback<RecordingRow>() {
        override fun areItemsTheSame(oldItem: RecordingRow, newItem: RecordingRow) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecordingRow, newItem: RecordingRow) = oldItem == newItem
    }
}
