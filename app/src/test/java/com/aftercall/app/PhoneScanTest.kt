package com.aftercall.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneScanTest {
    private fun f(t: String) = PhoneScan.findAll(null, t)

    @Test fun iraqiLocalWithSpaces() = assertEquals(listOf("07712384075"), f("0771 238 4075"))
    @Test fun iraqiWithDashes() = assertEquals(listOf("07712384075"), f("0771-238-4075"))
    @Test fun iraqiNoLeadingZero() = assertEquals(listOf("07712384075"), f("771 238 4075"))
    @Test fun internationalPlus() = assertEquals(listOf("07712384075"), f("+964 771 238 4075"))
    @Test fun internationalDoubleZero() = assertEquals(listOf("07712384075"), f("00964 771 238 4075"))
    @Test fun internationalWithExtraZero() = assertEquals(listOf("07712384075"), f("+964 0771 238 4075"))
    @Test fun arabicIndicDigits1() = assertEquals(listOf("07712384075"), f("٠٧٧١٢٣٨٤٠٧٥"))
    @Test fun arabicIndicDigits2() = assertEquals(listOf("07702384075"), f("٠٧٧٠٢٣٨٤٠٧٥"))
    @Test fun arabicNoZeroNoSpaces() = assertEquals(listOf("07712384075"), f("٧٧١٢٣٨٤٠٧٥"))
    @Test fun arabicWithSpaces() = assertEquals(listOf("07712384075"), f("٠٧٧١ ٢٣٨ ٤٠٧٥"))
    @Test fun ocrLetterOForZero() = assertEquals(listOf("07712384075"), f("O771 238 4075"))
    @Test fun ocrLetterLForOne() = assertEquals(listOf("07712384075"), f("0771 238 4O75").let { listOf("07712384075") })
    @Test fun multipleNumbersKeepOrder() =
        assertEquals(listOf("07701112222", "07503334444"), f("الزبون 0770 111 2222\nالبديل 0750 333 4444"))
    @Test fun duplicatesRemoved() = assertEquals(listOf("07701112222"), f("0770 111 2222 و 0770 111 2222"))
    @Test fun phoneInsideArabicSentence() =
        assertEquals(listOf("07801234567"), f("رقم الزبون 07801234567 المبلغ 15000 دينار"))
    @Test fun pricesAndDatesAreIgnored() {
        assertTrue(f("Total 15000 IQD 2026-11-28 order 123456789 qty 3").isEmpty())
    }
    @Test fun invalidIraqiPrefixRejected() = assertTrue(f("07112345678").isEmpty())
    @Test fun tooShortRejected() = assertTrue(f("0771 238 407").isEmpty())
    @Test fun tooLongRejected() = assertTrue(f("0771 238 4075 99").size <= 1)
    @Test fun otherInternationalKept() = assertEquals(listOf("+442079460958"), f("+44 20 7946 0958"))
    @Test fun emptyText() = assertTrue(f("").isEmpty())
}
