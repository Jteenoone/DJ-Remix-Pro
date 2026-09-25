package com.example.djremixpro.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** U04–U05. Chữ tắt theo `ini` App:307; bỏ dấu theo D-17. */
class TextUtils2Test {

    @Test
    fun initials_takesFirstLetterOfFirstTwoWords_uppercased() {
        assertEquals("TT", TextUtils2.initials("Tầng thượng 102"))
        assertEquals("PĐ", TextUtils2.initials("Phố đêm"))
        assertEquals("ÁĐ", TextUtils2.initials("Ánh đèn sân khấu"))
        assertEquals("MS", TextUtils2.initials("Mưa sao băng (Club mix)"))
    }

    @Test
    fun initials_allSeedTitles_matchPrototype() {
        val titles = listOf(
            "Tết về rồi", "Vòng quay", "Chạy về phía biển", "Sài Gòn lên đèn", "Không cần lời",
            "Tầng thượng 102", "Mưa sao băng (Club mix)", "Phố đêm", "Ánh đèn sân khấu", "Đi đâu cũng được",
        )
        // Giá trị do Node chạy nguyên văn `ini` của App:307.
        assertEquals(
            "TV,VQ,CV,SG,KC,TT,MS,PĐ,ÁĐ,ĐĐ",
            titles.joinToString(",") { TextUtils2.initials(it) },
        )
    }

    @Test
    fun initials_singleWord_returnsOneLetter() {
        assertEquals("M", TextUtils2.initials("Mix"))
    }

    @Test
    fun initials_blankOrRepeatedSpaces_doesNotCrash() {
        TextUtils2.initials("")
        TextUtils2.initials("   ")
        TextUtils2.initials("Mix  25-09")
    }

    @Test
    fun normalizeForSearch_removesVietnameseDiacriticsAndLowercases() {
        assertEquals("den neon", TextUtils2.normalizeForSearch("Đèn Neon"))
        assertEquals("tang thuong", TextUtils2.normalizeForSearch("Tầng Thượng"))
        assertEquals("di dau", TextUtils2.normalizeForSearch("ĐI ĐÂU"))
        assertEquals("mua sao bang (club mix)", TextUtils2.normalizeForSearch("Mưa sao băng (Club mix)"))
    }

    @Test
    fun normalizeForSearch_plainTextOnlyLowercased_emptyStaysEmpty() {
        assertEquals("dj tam", TextUtils2.normalizeForSearch("dj tam"))
        assertEquals("english", TextUtils2.normalizeForSearch("English"))
        assertEquals("", TextUtils2.normalizeForSearch(""))
    }
}
