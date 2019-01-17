package piuk.blockchain.androidcoreui.utils;

import android.content.Context;
import android.text.format.DateUtils;
import piuk.blockchain.androidcoreui.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    private Context context;

    public DateUtil(Context context) {
        this.context = context;
    }

    /**
     * @param ts The Unix timestamp to format
     */
    public String formatted(long ts) {
        return formatted(ts, System.currentTimeMillis() / 1000);
    }

    /**
     * @param ts  The Unix timestamp to format
     * @param now The Unix timestamp of now
     */
    public String formatted(long ts, long now) {
        String ret;
        ts *= 1000;
        now *= 1000;

        Date localTime = new Date(ts);
        long date = localTime.getTime();

        long hours24 = 60L * 60L * 24L * 1000L;

        Calendar calNow = Calendar.getInstance();
        calNow.setTime(new Date(now));

        Calendar calThen = Calendar.getInstance();
        calThen.setTime(new Date(date));
        int thenDay = calThen.get(Calendar.DAY_OF_MONTH);

        long todayStart = getDayStart(now);
        long yesterdayStart = getDayStart(now - hours24);

        if (date >= todayStart) {
            //today
            ret = (String) DateUtils.getRelativeTimeSpanString(date, now, DateUtils.SECOND_IN_MILLIS, 0);
        } else if (date >= yesterdayStart) {
            //yesterday
            ret = context.getString(R.string.YESTERDAY);
        } else if (calNow.get(Calendar.YEAR) != calThen.get(Calendar.YEAR)) {
            //previous years
            int year = calThen.get(Calendar.YEAR);
            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            ret = month + " " + thenDay + ", " + year;
        } else {
            //this year
            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            ret = month + " " + thenDay;
        }

        return ret;
    }

    private long getDayStart(long time) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date(time));
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        return instance.getTime().getTime();
    }
}
