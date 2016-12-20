package piuk.blockchain.android.ui.customviews;

import android.content.Context;
import android.os.Build;
import android.support.annotation.StringDef;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import piuk.blockchain.android.R;

public class ToastCustom {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            TYPE_ERROR, TYPE_GENERAL, TYPE_OK
    })
    public @interface ToastType {
    }

    public static final String TYPE_ERROR = "TYPE_ERROR";
    public static final String TYPE_GENERAL = "TYPE_GENERAL";
    public static final String TYPE_OK = "TYPE_OK";

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    @UiThread
    public static void makeText(final Context context, final CharSequence text, final int duration, final @ToastType String type) {
        Toast toast = Toast.makeText(context, text, duration);

        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.transient_notification, null);
        TextView tv = (TextView) v.findViewById(R.id.message);
        tv.setText(text);

        switch (type) {
            case TYPE_ERROR:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    tv.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_error));
                else
                    //noinspection deprecation
                    tv.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_error));
                tv.setTextColor(ContextCompat.getColor(context, R.color.toast_error_text));

                break;
            case TYPE_GENERAL:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    tv.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_warning));
                else
                    //noinspection deprecation
                    tv.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_warning));
                tv.setTextColor(ContextCompat.getColor(context, R.color.toast_warning_text));

                break;
            case TYPE_OK:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    tv.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_info));
                else
                    //noinspection deprecation
                    tv.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_info));
                tv.setTextColor(ContextCompat.getColor(context, R.color.toast_info_text));
                break;
        }
        toast.setView(v);
        toast.show();
    }
}
