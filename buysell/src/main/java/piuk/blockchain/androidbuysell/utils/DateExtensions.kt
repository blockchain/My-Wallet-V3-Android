package piuk.blockchain.androidbuysell.utils

import android.annotation.SuppressLint
import org.apache.commons.lang3.time.DateUtils
import timber.log.Timber
import java.text.ParseException
import java.util.Date

/**
 * Converts a [String] from an ISO 8601 date to a [Date] object. The receiving [String] can specify
 * both seconds AND seconds + milliseconds. This is necessary because the Coinify API seems to
 * return one buy specify the other in their documentation, and we can't be sure that we won't see
 * the documented format at some point. If the [String] is for some reason not parsable due to
 * otherwise incorrect formatting, the resulting [Date] will be null.
 *
 * The returned times will always be in UTC.
 *
 * @return A [Date] object or null if the [String] isn't formatted correctly.
 */
@SuppressLint("SimpleDateFormat")
fun String.fromIso8601(): Date? {
    val millisFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val secondsFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    return try {
        DateUtils.parseDate(this, millisFormat, secondsFormat)
    } catch (e: ParseException) {
        Timber.e(e)
        null
    }
}