package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.databinding.ViewMiniPlayerBinding
import com.example.djremixpro.feature.shell.MiniPlayerUi

/**
 * Mini player bar (App:227-234): 64dp, panel background, 2dp progress line at the top (line / text),
 * stop button 44 (text-coloured circle, pause glyph 18 in booth), title 14/500 ellipsized, time Chakra 500 14 muted.
 * Set layout_height to @dimen/mini_player_height in the host layout.
 */
class MiniPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMiniPlayerBinding
    private val trackPaint = Paint().apply { color = context.colorOf(R.color.md_line) }
    private val progressPaint = Paint().apply { color = context.colorOf(R.color.md_text) }
    private var progress = 0f
    private var onStop: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(context.colorOf(R.color.md_panel))
        val pad = dp(16f).toInt()
        setPaddingRelative(pad, 0, pad, 0)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.mini_player_height)
        setWillNotDraw(false)
        binding = ViewMiniPlayerBinding.inflate(LayoutInflater.from(context), this)
        binding.buttonStop.setOnClickListener { onStop?.invoke() }
        if (!isInEditMode) visibility = GONE
    }

    /** null → GONE. */
    fun bind(state: MiniPlayerUi?) {
        if (state == null) {
            visibility = GONE
            return
        }
        visibility = VISIBLE
        binding.textTitle.text = state.title
        binding.textTime.text = state.timeLabel
        progress = state.progress.coerceIn(0f, 1f)
        invalidate()
    }

    fun setOnStopClickListener(l: () -> Unit) {
        onStop = l
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = dp(2f)
        val w = width.toFloat()
        canvas.drawRect(0f, 0f, w, h, trackPaint)
        // Progress grows from the start edge (right in RTL).
        if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            canvas.drawRect(w - w * progress, 0f, w, h, progressPaint)
        } else {
            canvas.drawRect(0f, 0f, w * progress, h, progressPaint)
        }
    }
}
