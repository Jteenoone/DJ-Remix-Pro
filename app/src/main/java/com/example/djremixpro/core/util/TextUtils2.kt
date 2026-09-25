package com.example.djremixpro.core.util

import java.text.Normalizer
import java.util.Locale

object TextUtils2 {
    private val combiningMarks = Regex("\\p{Mn}+")

    /** First letter of the first two words, upper-cased: "Tầng thượng 102" → "TT" (App.dc.html:307). */
    fun initials(title: String): String =
        title.split(' ').take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase(Locale.ROOT)

    /** Lower-case, accents removed, đ → d: "Sài Gòn" → "sai gon". */
    fun normalizeForSearch(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
}
