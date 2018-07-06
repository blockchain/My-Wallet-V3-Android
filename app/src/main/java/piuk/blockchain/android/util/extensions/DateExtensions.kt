package piuk.blockchain.android.util.extensions

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Takes a [Date] object and converts it to our standard date format, ie March 09, 2018 @11:47.
 *
 * @param locale The current [Locale].
 * @return A formatted [String] object.
 */
fun Date.toFormattedString(locale: Locale): String {
    val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)
    val timeFormat = SimpleDateFormat("hh:mm a", locale)
    val dateText = dateFormat.format(this)
    val timeText = timeFormat.format(this)

    return "$dateText @ $timeText"
}