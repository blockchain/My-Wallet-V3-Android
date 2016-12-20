package piuk.blockchain.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatEditText;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Created by adambennett on 29/07/2016.
 */

public class ViewUtils {

    /**
     * Converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp to convert to pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Converts device specific pixels to dp.
     *
     * @param pixels  A value in px to be converted to dp
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float pixels, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return pixels / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Returns a properly padded FrameLayout which wraps an {@link AppCompatEditText}. Once wrapped,
     * the view will conform to the Material Design guidelines for spacing within a Dialog.
     *
     * @param context  The current Activity or Fragment context
     * @param editText An AppCompatEditText for wrapping (Note: we're using the AppCompat version
     *                 for design compatibility - there's no reason to be using plain EditTexts)
     * @return A correctly padded FrameLayout containing the AppCompatEditText
     */
    public static FrameLayout getAlertDialogEditTextLayout(Context context, AppCompatEditText editText) {
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, context);
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(editText, params);

        return frameLayout;
    }

    /**
     * Sets an elevation value for a View using the appropriate method for a given API level. For
     * now, {@link ViewCompat#setElevation(View, float)} only accesses stub methods but this may
     * change in the future.
     *
     * @param view      The {@link View} to set elevation on
     * @param elevation A float value for elevation (in pixels, not dp)
     */
    public static void setElevation(View view, float elevation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(elevation);
        } else {
            ViewCompat.setElevation(view, elevation);
        }
    }

    /**
     * These annotations are hidden in the Android Jar for some reason. Defining them here instead
     * for use in ViewModel callbacks etc.
     */
    @IntDef({VISIBLE, INVISIBLE, GONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Visibility {
    }

    /**
     * Hides the keyboard in a specified {@link Activity}
     *
     * @param activity The Activity in which you want to hide the keyboard
     */
    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
