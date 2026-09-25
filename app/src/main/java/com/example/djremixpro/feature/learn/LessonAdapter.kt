package com.example.djremixpro.feature.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.djremixpro.R
import com.example.djremixpro.core.model.LessonStatus
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.databinding.ItemLessonBinding

/** Lesson rows (App:154-162): done = raised circle + check, current = text circle + "Bắt đầu", todo = outlined. */
class LessonAdapter(private val onClick: (Int) -> Unit) : ListAdapter<LessonRow, LessonAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.binding.root.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onClick(getItem(it).id) }
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val b = holder.binding
        val context = b.root.context
        val done = row.status == LessonStatus.DONE
        val current = row.status == LessonStatus.CURRENT
        b.root.setBackgroundResource(if (current) R.drawable.sel_panel_r12 else R.drawable.sel_transparent_r12)
        b.frameNumber.setBackgroundResource(
            when (row.status) {
                LessonStatus.DONE -> R.drawable.bg_lesson_done
                LessonStatus.CURRENT -> R.drawable.bg_lesson_current
                LessonStatus.TODO -> R.drawable.bg_lesson_todo
            },
        )
        b.imageDone.isVisible = done
        b.textNumber.isVisible = !done
        b.textNumber.text = row.number.toString()
        b.textNumber.setTextColor(context.colorOf(if (current) R.color.md_booth else R.color.md_muted))
        b.textTitle.text = row.title
        b.textTitle.setTextColor(context.colorOf(if (done) R.color.md_muted else R.color.md_text))
        b.textSteps.text = row.stepsLabel.resolve(context)
        b.textStart.isVisible = current
    }

    private object Diff : DiffUtil.ItemCallback<LessonRow>() {
        override fun areItemsTheSame(oldItem: LessonRow, newItem: LessonRow) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LessonRow, newItem: LessonRow) = oldItem == newItem
    }
}
