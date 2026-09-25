package com.example.djremixpro.feature.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.core.model.GlossaryTerm
import com.example.djremixpro.databinding.ItemGlossaryBinding

class GlossaryAdapter(private val onSeeOnMixer: () -> Unit) :
    ListAdapter<GlossaryTerm, GlossaryAdapter.Holder>(Diff) {

    class Holder(val binding: ItemGlossaryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(ItemGlossaryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.binding.buttonSeeOnMixer.setOnClickListener { onSeeOnMixer() }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val term = getItem(position)
        holder.binding.textTerm.text = term.term
        holder.binding.textDescription.text = term.description
    }

    private object Diff : DiffUtil.ItemCallback<GlossaryTerm>() {
        override fun areItemsTheSame(oldItem: GlossaryTerm, newItem: GlossaryTerm) = oldItem.term == newItem.term
        override fun areContentsTheSame(oldItem: GlossaryTerm, newItem: GlossaryTerm) = oldItem == newItem
    }
}
