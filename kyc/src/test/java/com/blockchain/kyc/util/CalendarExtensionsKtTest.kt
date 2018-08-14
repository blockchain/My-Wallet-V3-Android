package com.blockchain.kyc.util

import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class CalendarExtensionsKtTest {

    @Test
    fun `calendar should be formatted as 8601 date`() {
        Calendar.getInstance(Locale.UK).apply {
            set(Calendar.YEAR, 2000)
            // Months start at zero
            set(Calendar.MONTH, 7)
            set(Calendar.DAY_OF_MONTH, 12)
        }.toISO8601DateString() `should equal to` "2000-08-12"
    }

    @Test
    fun `calendar should be formatted as 8601 date with single digit date`() {
        Calendar.getInstance(Locale.UK).apply {
            set(Calendar.YEAR, 1337)
            // Months start at zero
            set(Calendar.MONTH, 11)
            set(Calendar.DAY_OF_MONTH, 1)
        }.toISO8601DateString() `should equal to` "1337-12-01"
    }

    @Test
    fun `calendar should be formatted as simple date - all locales`() {
        val list = mutableListOf<Locale>()
        Locale.getAvailableLocales().forEach {
            if (Calendar.getInstance(it).apply {
                    set(Calendar.YEAR, 1999)
                    // Months start at zero
                    set(Calendar.MONTH, 0)
                    set(Calendar.DAY_OF_MONTH, 31)
                }.toISO8601DateString() != "1999-01-31") {
                list.add(it)
            }
        }
        list `should equal` emptyList()
    }
}