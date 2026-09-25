package com.example.djremixpro.core.ui.widget

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf

/**
 * Colours of one deck, resolved from md_* resources (theme-aware accent) plus the fixed vinyl colours
 * that stay dark in both themes (D-16, Mixer:102-110).
 */
class DeckPalette private constructor(
    @param:ColorInt val accent: Int,
    @param:ColorInt val labelBg: Int,
    @param:ColorInt val vinyl: Int,
) {
    /** `color-mix(in srgb, accent N%, transparent)`. */
    @ColorInt
    fun accentAlpha(percent: Int): Int = ColorUtils.setAlphaComponent(accent, percent * 255 / 100)

    companion object {
        const val DECK_A = 0
        const val DECK_B = 1

        fun of(context: Context, deck: Int): DeckPalette = if (deck == DECK_B) {
            DeckPalette(
                context.colorOf(R.color.md_deck_b),
                context.colorOf(R.color.md_fixed_label_b),
                context.colorOf(R.color.md_fixed_vinyl_b),
            )
        } else {
            DeckPalette(
                context.colorOf(R.color.md_deck_a),
                context.colorOf(R.color.md_fixed_label_a),
                context.colorOf(R.color.md_fixed_vinyl_a),
            )
        }
    }
}
