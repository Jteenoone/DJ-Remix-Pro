package com.example.djremixpro.core.ui.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.core.ui.ext.fontOf
import com.example.djremixpro.core.ui.ext.sp

/**
 * Drag value bubble of knobs and faders (MixDeck:541, :568): height 26, padding 0 8, radius 8, background text,
 * Chakra Petch 600 14 in booth colour. Drawn in the root view overlay so it is never clipped by the tight
 * mixer grid; positioned in root coordinates next to the anchor.
 */
internal class ValueBubble(private val anchor: View) {

    enum class Placement { ABOVE, BESIDE }

    private val drawable = BubbleDrawable()
    private var host: ViewGroup? = null
    private val anchorLoc = IntArray(2)
    private val hostLoc = IntArray(2)

    fun show(text: String, placement: Placement, anchorTopInView: Float = 0f, anchorBottomInView: Float = 0f) {
        val root = anchor.rootView as? ViewGroup ?: return
        if (host !== root) {
            host?.overlay?.remove(drawable)
            host = root
            root.overlay.add(drawable)
        }
        drawable.text = text
        anchor.getLocationInWindow(anchorLoc)
        root.getLocationInWindow(hostLoc)
        val ax = (anchorLoc[0] - hostLoc[0]).toFloat()
        val ay = (anchorLoc[1] - hostLoc[1]).toFloat()
        val w = drawable.measureWidth()
        val h = drawable.bubbleHeight
        val left: Float
        val top: Float
        when (placement) {
            Placement.ABOVE -> {
                left = ax + anchor.width / 2f - w / 2f
                top = ay - h - anchor.dp(8f)
            }
            Placement.BESIDE -> {
                // Towards the screen centre so the bubble does not leave the screen.
                val towardsRight = ax + anchor.width / 2f < root.width / 2f
                left = if (towardsRight) ax + anchor.width + anchor.dp(4f) else ax - w - anchor.dp(4f)
                val thumbCentre = ay + (anchorTopInView + anchorBottomInView) / 2f
                top = thumbCentre - h / 2f
            }
        }
        val l = left.coerceIn(0f, (root.width - w).coerceAtLeast(0f))
        val t = top.coerceIn(0f, (root.height - h).coerceAtLeast(0f))
        drawable.setBounds(l.toInt(), t.toInt(), (l + w).toInt(), (t + h).toInt())
        drawable.invalidateSelf()
    }

    fun hide() {
        host?.overlay?.remove(drawable)
        host = null
    }

    private inner class BubbleDrawable : Drawable() {
        var text: String = ""
        private val ctx = anchor.context
        val bubbleHeight = anchor.dp(26f)
        private val padH = anchor.dp(8f)
        private val radius = anchor.dp(8f)
        private val rect = RectF()
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ctx.colorOf(R.color.md_text) }
        private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ctx.colorOf(R.color.md_booth)
            typeface = ctx.fontOf(R.font.chakra_petch_semibold)
            textSize = anchor.sp(14f)
            fontFeatureSettings = "tnum"
        }

        fun measureWidth(): Float = fg.measureText(text) + 2 * padH

        override fun draw(canvas: Canvas) {
            rect.set(bounds)
            canvas.drawRoundRect(rect, radius, radius, bg)
            val baseline = rect.centerY() - (fg.descent() + fg.ascent()) / 2f
            canvas.drawText(text, rect.left + padH, baseline, fg)
        }

        override fun setAlpha(alpha: Int) {
            bg.alpha = alpha
            fg.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            bg.colorFilter = colorFilter
            fg.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
