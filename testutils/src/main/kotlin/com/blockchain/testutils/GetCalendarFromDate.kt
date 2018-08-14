package com.blockchain.testutils

import java.util.Calendar
import java.util.Locale

fun date(
    locale: Locale,
    year: Int,
    month: Int,
    dayOfMonth: Int
): Calendar = Calendar.getInstance(locale).apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month - 1)
    set(Calendar.DAY_OF_MONTH, dayOfMonth)
}