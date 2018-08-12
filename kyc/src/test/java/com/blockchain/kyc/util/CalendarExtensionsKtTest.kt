package com.blockchain.kyc.util

import org.amshove.kluent.`should equal to`
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class CalendarExtensionsKtTest {

    @Test
    fun `calendar should be formatted as simple date`() {
        Calendar.getInstance(Locale.UK).apply {
            set(Calendar.YEAR, 2000)
            // Months start at zero
            set(Calendar.MONTH, 7)
            set(Calendar.DAY_OF_MONTH, 12)
        }.toSimpleDateString() `should equal to` "2000-08-12"
    }
}