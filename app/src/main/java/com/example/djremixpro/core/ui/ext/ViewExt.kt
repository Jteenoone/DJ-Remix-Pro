package com.example.djremixpro.core.ui.ext

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

/** 1 CSS px of the design = 1 dp (DESIGNER_REPORT §0). */
fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

fun Context.sp(value: Float): Float =
    android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

fun View.dp(value: Float): Float = context.dp(value)

fun View.sp(value: Float): Float = context.sp(value)

fun Context.colorOf(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

fun View.colorOf(@ColorRes id: Int): Int = context.colorOf(id)

fun Context.fontOf(@FontRes id: Int): Typeface = ResourcesCompat.getFont(this, id) ?: Typeface.DEFAULT

/**
 * Reduced motion: the system "Remove animations" / animator duration scale 0 disables animators.
 * Design equivalent of `prefers-reduced-motion: reduce` (Mixer:18).
 */
fun isReducedMotion(): Boolean = !ValueAnimator.areAnimatorsEnabled()

/** Short detent tick (crossfader / pitch centre, kill). Respects the system haptic setting. */
fun View.hapticTick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.hapticLongPress() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
