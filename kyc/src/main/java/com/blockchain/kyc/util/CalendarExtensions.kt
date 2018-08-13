package com.blockchain.kyc.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

@SuppressLint("SimpleDateFormat")
fun Calendar.toSimpleDateString(): String = SimpleDateFormat("yyyy-MM-dd")
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
    .run { format(this@toSimpleDateString.time) }