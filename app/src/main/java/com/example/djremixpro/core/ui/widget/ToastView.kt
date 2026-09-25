package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.databinding.ViewToastBinding

/**
 * In-app toast (App:260-264, flash() App:279): raised pill, radius 12, height 52, padding 0 16, check icon 20 in the
 * tone colour + text 14. Hides itself after 2200 ms; calling [show] again replaces the text and restarts the timer.
 * The host places it (left/right 16, above the mini player or bottom nav, D-24). Short fade in/out (150 ms),
 * which the system animator scale turns off.
 */
class ToastView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewToastBinding
    private val hideRunnable = Runnable { hide() }

    init {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = context.getDrawable(R.drawable.bg_toast)
            minimumHeight = resources.getDimensionPixelSize(R.dimen.toast_height)
            val pad = dp(16f).toInt()
            setPaddingRelative(pad, dp(6f).toInt(), pad, dp(6f).toInt())
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        binding = ViewToastBinding.inflate(LayoutInflater.from(context), row)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        if (!isInEditMode) visibility = GONE
    }

    fun show(message: ToastMessage) {
        binding.textMessage.text = message.text.resolve(context)
        val tint = when (message.tone) {
            ToastTone.SUCCESS, ToastTone.DECK_B -> R.color.md_deck_b
            ToastTone.DECK_A -> R.color.md_deck_a
            ToastTone.ERROR -> R.color.md_rec
        }
        binding.imageCheck.imageTintList = ColorStateList.valueOf(context.colorOf(tint))
        removeCallbacks(hideRunnable)
        animate().cancel()
        if (visibility != VISIBLE) {
            alpha = 0f
            visibility = VISIBLE
        }
        animate().alpha(1f).setDuration(FADE_MS).start()
        postDelayed(hideRunnable, DURATION_MS)
    }

    private fun hide() {
        animate().alpha(0f).setDuration(FADE_MS).withEndAction { visibility = GONE }.start()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideRunnable)
        animate().cancel()
        visibility = GONE
        super.onDetachedFromWindow()
    }

    private companion object {
        const val DURATION_MS = 2200L
        const val FADE_MS = 150L
    }
}
