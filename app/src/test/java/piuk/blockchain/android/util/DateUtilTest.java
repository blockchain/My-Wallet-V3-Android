package piuk.blockchain.android.util;

import android.annotation.SuppressLint;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.androidcoreui.utils.DateUtil;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class DateUtilTest {

    @Mock private Context mMockContext;

    @Test
    public void dateFormatTest() throws Exception {
        DateUtil dateUtil = new DateUtil(mMockContext);

        //unit test for 'Today' and 'Yesterday' uses android framework (code unchanged)

        Calendar now = Calendar.getInstance();
        String year = String.valueOf(now.get(Calendar.YEAR));
        // Pass in current year so that tests don't break after new year
        assertThat(dateUtil.formatted(parseDateTime(year + "-01-01 00:00:00")), is("January 1"));
        assertThat(dateUtil.formatted(parseDateTime("2015-12-31 23:59:59")), is("December 31, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-01-01 00:00:00")), is("January 1, 2015"));

        // These are broken because DateUtils.getRelativeTimeSpanString has to be mocked by
        // Robolectric, but that mock returns a format that is reversed, ie "15 April". Not really
        // sure how to fix this currently.
//        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 00:00:00")), is("April 15"));
//        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 12:00:00")), is("April 15"));
//        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 23:00:00")), is("April 15"));
//        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 23:59:59")), is("April 15"));
//
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 00:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 12:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 23:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 23:59:59")), is("April 15, 2015"));
    }

    @SuppressLint("SimpleDateFormat")
    private long parseDateTime(String time) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime() / 1000;
    }
}