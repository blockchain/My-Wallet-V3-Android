package piuk.blockchain.android.ui.customviews;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import piuk.blockchain.android.R;

public class MaterialProgressDialog {

    private AlertDialog mAlertDialog;
    private TextView mMessageTextView;

    /**
     * Creates an {@link AlertDialog} with a custom view for emulating a Material Design progress
     * dialog on pre-Lollipop devices.
     *
     * @param context The Activity Context
     */
    public MaterialProgressDialog(Context context) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.progress_dialog_compat, null);

        mMessageTextView = (TextView) layout.findViewById(R.id.message);
        ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);

        mAlertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(context.getString(R.string.app_name))
                .setView(layout)
                .create();

        TypedArray a;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.colorAccent});
        } else {
            a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.colorAccent});
        }
        progressBar.getIndeterminateDrawable().setColorFilter(a.getColor(0, 0), PorterDuff.Mode.SRC_IN);
    }

    public void setMessage(String message) {
        if (mAlertDialog != null && mMessageTextView != null) {
            mMessageTextView.setText(message);
        }
    }

    public void setMessage(@StringRes int message) {
        if (mAlertDialog != null && mMessageTextView != null) {
            mMessageTextView.setText(message);
        }
    }

    public void setTitle(String title) {
        if (mAlertDialog != null) {
            mAlertDialog.setTitle(title);
        }
    }

    public void setTitle(@StringRes int title) {
        if (mAlertDialog != null) {
            mAlertDialog.setTitle(title);
        }
    }

    public void setCancelable(boolean cancelable) {
        if (mAlertDialog != null) {
            mAlertDialog.setCancelable(cancelable);
        }
    }

    public boolean isShowing() {
        return mAlertDialog != null && mAlertDialog.isShowing();
    }

    public void show() {
        if (mAlertDialog != null) {
            mAlertDialog.show();
        }
    }

    public void dismiss() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (mAlertDialog != null) {
            mAlertDialog.setOnCancelListener(listener);
        }
    }
}
