package piuk.blockchain.androidcoreui.utils;

import android.os.Build;

public class AndroidUtils {

    public static boolean is19orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean is21orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean is23orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean is24orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean is25orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public static boolean is26orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

}
