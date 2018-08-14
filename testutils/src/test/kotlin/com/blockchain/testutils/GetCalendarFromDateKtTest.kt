package com.blockchain.testutils

import org.amshove.kluent.`should equal to`
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class GetCalendarFromDateKtTest {

    @Test
    fun `get date should return valid date object with month set correctly`() {
        date(Locale.US, 1000, 9, 30).run {
            get(Calendar.YEAR) `should equal to` 1000
            get(Calendar.MONTH) `should equal to` 8
            get(Calendar.DAY_OF_MONTH) `should equal to` 30
        }
    }
}