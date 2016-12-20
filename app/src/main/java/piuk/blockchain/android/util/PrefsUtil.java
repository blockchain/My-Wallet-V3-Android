package piuk.blockchain.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import piuk.blockchain.android.data.api.DebugSettings;

public class PrefsUtil implements PersistentPrefs {

    private SharedPreferences preferenceManager;

    public PrefsUtil(Context context) {
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getValue(String name, String value) {
        return preferenceManager.getString(name, (value == null || value.isEmpty()) ? "" : value);
    }

    @Override
    public void setValue(String name, String value) {
        Editor editor = preferenceManager.edit();
        editor.putString(name, (value == null || value.isEmpty()) ? "" : value);
        editor.apply();
    }

    @Override
    public int getValue(String name, int value) {
        return preferenceManager.getInt(name, 0);
    }

    @Override
    public void setValue(String name, int value) {
        Editor editor = preferenceManager.edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        editor.apply();
    }

    @Override
    public void setValue(String name, long value) {
        Editor editor = preferenceManager.edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        editor.apply();
    }

    @Override
    public long getValue(String name, long value) {

        long result;
        try {
            result = preferenceManager.getLong(name, 0L);
        } catch (Exception e) {
            result = (long) preferenceManager.getInt(name, 0);
        }

        return result;
    }

    @Override
    public boolean getValue(String name, boolean value) {
        return preferenceManager.getBoolean(name, value);
    }

    @Override
    public void setValue(String name, boolean value) {
        Editor editor = preferenceManager.edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    @Override
    public boolean has(String name) {
        return preferenceManager.contains(name);
    }

    @Override
    public void removeValue(String name) {
        Editor editor = preferenceManager.edit();
        editor.remove(name);
        editor.apply();
    }

    @Override
    public void clear() {
        Editor editor = preferenceManager.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Clears everything but the GUID for logging back in
     */
    @Override
    public void logOut() {
        String guid = getValue(PrefsUtil.KEY_GUID, "");
        clear();

        setValue(PrefsUtil.LOGGED_OUT, true);
        setValue(PrefsUtil.KEY_GUID, guid);
    }

    /**
     * Reset value once user logged in
     */
    @Override
    public void logIn() {
        setValue(PrefsUtil.LOGGED_OUT, false);
    }

    @Override
    public void clearPrefsAndKeepEnvironment() {
        String environment = getValue(DebugSettings.KEY_CURRENT_ENVIRONMENT, "");
        clear();
        setValue(DebugSettings.KEY_CURRENT_ENVIRONMENT, environment);
    }
}
