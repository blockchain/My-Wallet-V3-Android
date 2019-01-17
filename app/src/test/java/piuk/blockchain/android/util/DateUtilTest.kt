package piuk.blockchain.android.util

import android.annotation.SuppressLint
import org.amshove.kluent.`should equal`
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.androidcoreui.utils.DateUtil
import java.text.SimpleDateFormat

@Config(sdk = [23], constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class DateUtilTest {

    private val dateUtil = DateUtil(RuntimeEnvironment.application)

    @Test
    fun dateFormatSameYear() {
        val toFormat = parseDateTime("2019-01-01 00:00:00")
        val now = parseDateTime("2019-12-31 14:41:00")
        dateUtil.formatted(toFormat, now) `should equal` "January 1"
    }

    @Test
    fun dateFormatDifferentYear() {
        val toFormat = parseDateTime("2019-01-01 00:00:00")
        val now = parseDateTime("2020-12-31 14:41:00")
        dateUtil.formatted(toFormat, now) `should equal` "January 1, 2019"
    }

    @Test
    fun dateFormatToday() {
        val toFormat = parseDateTime("2019-01-02 14:11:00")
        val now = parseDateTime("2019-01-02 14:41:00")
        dateUtil.formatted(toFormat, now) `should equal` "30 minutes ago"
    }

    @Test
    fun dateFormatYesterday() {
        val toFormat = parseDateTime("2019-01-01 00:00:00")
        val now = parseDateTime("2019-01-02 14:41:00")
        dateUtil.formatted(toFormat, now) `should equal` "Yesterday"
    }

    @Test
    fun dateFormatTest() {
        dateUtil.formatted(parseDateTime("2015-12-31 23:59:59")) `should equal` "December 31, 2015"
        dateUtil.formatted(parseDateTime("2015-01-01 00:00:00")) `should equal` "January 1, 2015"

        dateUtil.formatted(parseDateTime("2015-04-15 00:00:00")) `should equal` "April 15, 2015"
        dateUtil.formatted(parseDateTime("2015-04-15 12:00:00")) `should equal` "April 15, 2015"
        dateUtil.formatted(parseDateTime("2015-04-15 23:00:00")) `should equal` "April 15, 2015"
        dateUtil.formatted(parseDateTime("2015-04-15 23:59:59")) `should equal` "April 15, 2015"
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseDateTime(time: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).time / 1000
}
