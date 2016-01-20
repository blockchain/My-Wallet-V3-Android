package info.blockchain.wallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefsUtil implements PersistantPrefs {

    private static Context context = null;
    private static PrefsUtil instance = null;

    private PrefsUtil() {
        ;
    }

    public static PrefsUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new PrefsUtil();
        }

        return instance;
    }

    public String getValue(String name, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
    }

    public boolean setValue(String name, String value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(name, (value == null || value.length() < 1) ? "" : value);
        return editor.commit();
    }

    public int getValue(String name, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(name, 0);
    }

    public boolean setValue(String name, int value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        return editor.commit();
    }

    public boolean setValue(String name, long value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    public long getValue(String name, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long result = 0l;
        try {
            result = prefs.getLong(name, 0L);
        } catch (Exception e) {
            result = (long) prefs.getInt(name, 0);
        }

        return result;
    }

    public boolean getValue(String name, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(name, value);
    }

    public boolean setValue(String name, boolean value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    public boolean has(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(name);
    }

    public boolean removeValue(String name) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(name);
        return editor.commit();
    }

    public boolean clear() {
        String cookie = getValue(KEY_SESSION_ID, null);
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.clear();
        if (cookie != null) setValue(KEY_SESSION_ID, cookie);
        return editor.commit();
    }

}
