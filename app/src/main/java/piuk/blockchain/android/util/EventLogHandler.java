package piuk.blockchain.android.util;

import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;


public class EventLogHandler {

    public static final String URL_EVENT_BASE = "https://blockchain.info/event?name=";
    public static final String URL_EVENT_2ND_PW = URL_EVENT_BASE + "wallet_login_second_password_";
    public static final String URL_EVENT_LEGACY = URL_EVENT_BASE + "wallet_login_legacy_use_";
    public static final String URL_EVENT_BACKUP = URL_EVENT_BASE + "wallet_login_android_recovery_phrase_";

    PrefsUtil prefsUtil;
    WebUtil webUtil;

    public EventLogHandler(PrefsUtil prefsUtil, WebUtil webUtil) {
        this.webUtil = webUtil;
        this.prefsUtil = prefsUtil;
    }

    public void log2ndPwEvent(boolean active) {
        logEvent(PrefsUtil.KEY_EVENT_2ND_PW, URL_EVENT_2ND_PW, active);
    }

    public void logLegacyEvent(boolean active) {
        // TODO: 20/12/2016 Disabled
//        logEvent(PrefsUtil.KEY_EVENT_LEGACY, URL_EVENT_LEGACY, active);
    }

    public void logBackupEvent(boolean active) {
        // TODO: 20/12/2016 Disabled
//        logEvent(PrefsUtil.KEY_EVENT_BACKUP, URL_EVENT_BACKUP, active);
    }

    private void logEvent(String key, String url, boolean active) {

        if (!prefsUtil.getValue(key, false)) {
            sendEvent(url, active)
                    .subscribeOn(Schedulers.io())
                    .subscribe(response -> {
                        if (response != null && FormatsUtil.getInstance().isValidJson(response)) {

                            try {
                                JSONObject responseJson = new JSONObject(response);
                                if(responseJson.has("success") && responseJson.getBoolean("success")) {
                                    prefsUtil.setValue(key, true);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, Throwable::printStackTrace);
        }
    }

    private Observable<String> sendEvent(String url, boolean active) {

        String value;
        if (active)
            value = "1";
        else
            value = "0";

        return Observable.fromCallable(() -> webUtil.getURL(url + value));
    }
}
