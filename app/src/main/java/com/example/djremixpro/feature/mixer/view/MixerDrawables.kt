package com.example.djremixpro.feature.mixer.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import androidx.annotation.ColorInt
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp

/**
 * Runtime backgrounds of the mixer controls whose colours depend on the deck (Mixer:130, 142, 147, 159, 164).
 * Created only when a control changes state, never per frame.
 */
internal class MixerDrawables(private val context: Context) {

    private val ripple = ColorStateList.valueOf(context.colorOf(R.color.md_line2))
    private val r10 = context.dp(10f)
    private val r12 = context.dp(12f)
    private val stroke = context.dp(1.5f)

    @ColorInt val raised = context.colorOf(R.color.md_raised)
    @ColorInt val line = context.colorOf(R.color.md_line)
    @ColorInt val text = context.colorOf(R.color.md_text)
    @ColorInt val muted = context.colorOf(R.color.md_muted)
    @ColorInt val booth = context.colorOf(R.color.md_booth)
    @ColorInt val text12 = context.colorOf(R.color.md_text_12)

    fun rounded(@ColorInt fill: Int, radius: Float, @ColorInt strokeColor: Int? = null, dashed: Boolean = false): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fill)
            if (strokeColor != null) {
                if (dashed) {
                    setStroke(stroke.toInt().coerceAtLeast(1), strokeColor, context.dp(4f), context.dp(3f))
                } else {
                    setStroke(stroke.toInt().coerceAtLeast(1), strokeColor)
                }
            }
        }

    fun withRipple(content: Drawable): Drawable = RippleDrawable(ripple, content, null)

    /** Toggle button r12 with 1.5 border (Scratch, Sync). */
    fun toggle(@ColorInt fill: Int, @ColorInt border: Int): Drawable = withRipple(rounded(fill, r12, border))

    /** Play button r12 without border. */
    fun play(@ColorInt fill: Int): Drawable = withRipple(rounded(fill, r12))

    /** Pad r10 with 1.5 border. */
    fun pad(@ColorInt fill: Int, @ColorInt border: Int): Drawable = withRipple(rounded(fill, r10, border))

    /**
     * Pad tab (Mixer:142): selected = raised r10 with `box-shadow: inset 0 -2px` in the deck colour,
     * i.e. a full-width 2dp band inside the bottom edge, clipped by the rounded corners.
     */
    fun tab(selected: Boolean, @ColorInt accent: Int): Drawable =
        if (selected) {
            withRipple(TabDrawable(raised, accent, r10, context.dp(2f)))
        } else {
            withRipple(rounded(android.graphics.Color.TRANSPARENT, r10))
        }

    private class TabDrawable(
        @ColorInt private val fill: Int,
        @ColorInt private val bar: Int,
        private val radius: Float,
        private val barHeight: Float,
    ) : Drawable() {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val rect = android.graphics.RectF()
        private val clip = android.graphics.Path()

        override fun onBoundsChange(bounds: android.graphics.Rect) {
            rect.set(bounds)
            clip.reset()
            clip.addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        }

        override fun draw(canvas: android.graphics.Canvas) {
            paint.color = fill
            canvas.drawRoundRect(rect, radius, radius, paint)
            val save = canvas.save()
            canvas.clipPath(clip)
            paint.color = bar
            canvas.drawRect(rect.left, rect.bottom - barHeight, rect.right, rect.bottom, paint)
            canvas.restoreToCount(save)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }
}
