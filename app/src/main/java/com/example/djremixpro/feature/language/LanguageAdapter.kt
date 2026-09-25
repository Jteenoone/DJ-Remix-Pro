package com.example.djremixpro.feature.language

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.R
import com.example.djremixpro.databinding.ItemLanguageBinding

class LanguageAdapter(private val onSelect: (String) -> Unit) :
    ListAdapter<LanguageRow, LanguageAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        var selected = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.binding.root.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onSelect(getItem(it).tag) }
        }
        // Row = radio button for TalkBack.
        holder.binding.root.accessibilityDelegate = object : View.AccessibilityDelegate() {
            @Suppress("DEPRECATION") // setChecked(Boolean) is the only API below 36
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.RadioButton"
                info.isCheckable = true
                info.isChecked = holder.selected
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val b = holder.binding
        holder.selected = row.isSelected
        b.textNative.text = row.nativeName
        b.textVietnamese.text = row.vietnameseName
        b.viewRadio.setBackgroundResource(if (row.isSelected) R.drawable.bg_radio_on else R.drawable.bg_radio_off)
    }

    private object Diff : DiffUtil.ItemCallback<LanguageRow>() {
        override fun areItemsTheSame(oldItem: LanguageRow, newItem: LanguageRow) = oldItem.tag == newItem.tag
        override fun areContentsTheSame(oldItem: LanguageRow, newItem: LanguageRow) = oldItem == newItem
    }
}
