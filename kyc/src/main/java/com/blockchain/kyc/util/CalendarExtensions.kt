package com.blockchain.kyc.util

import java.util.Calendar
import java.util.Locale

fun Calendar.toISO8601DateString(): String =
    String.format(
        Locale.US,
        "%04d-%02d-%02d",
        get(Calendar.YEAR),
        get(Calendar.MONTH) + 1,
        get(Calendar.DAY_OF_MONTH)
    )