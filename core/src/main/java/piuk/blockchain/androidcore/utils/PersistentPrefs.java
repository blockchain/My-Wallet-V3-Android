package piuk.blockchain.androidcore.utils;

public interface PersistentPrefs {

    String DEFAULT_CURRENCY = "USD";

    String KEY_PIN_IDENTIFIER = "pin_kookup_key";
    String KEY_ENCRYPTED_PASSWORD = "encrypted_password";
    String KEY_GUID = "guid";
    String KEY_SHARED_KEY = "sharedKey";
    String KEY_PIN_FAILS = "pin_fails";
    String KEY_SELECTED_FIAT = "ccurrency";
    String KEY_INITIAL_ACCOUNT_NAME = "_1ST_ACCOUNT_NAME";
    String KEY_EMAIL = "email";
    String KEY_EMAIL_VERIFIED = "code_verified";
    String KEY_EMAIL_VERIFY_ASK_LATER = "email_verify_ask_later";
    String KEY_SCHEME_URL = "scheme_url";
    String KEY_METADATA_URI = "metadata_uri";
    String KEY_CONTACTS_NOTIFICATION = "contacts_notification";
    String KEY_CURRENT_APP_VERSION = "KEY_CURRENT_APP_VERSION";
    String KEY_NEWLY_CREATED_WALLET = "newly_created_wallet";
    String LOGGED_OUT = "logged_out";
    String KEY_BACKEND_ENVIRONMENT = "backend_environment";
    String KEY_SECURITY_TIME_ELAPSED = "security_time_elapsed";
    String KEY_SECURITY_TWO_FA_NEVER = "security_two_fa_never";
    String KEY_SECURITY_BACKUP_NEVER = "security_backup_never";
    String KEY_EVENT_2ND_PW = "event_2nd_pw";
    String KEY_EVENT_LEGACY = "event_legacy";
    String KEY_EVENT_BACKUP = "event_backup";
    String KEY_ENCRYPTED_PIN_CODE = "encrypted_pin_code";
    String KEY_FINGERPRINT_ENABLED = "fingerprint_enabled";
    String KEY_RECEIVE_SHORTCUTS_ENABLED = "receive_shortcuts_enabled";
    String KEY_FIREBASE_TOKEN = "firebase_token";
    String KEY_SWIPE_TO_RECEIVE_ENABLED = "swipe_to_receive_enabled";
    String KEY_APP_VISITS = "app_visits";
    String KEY_SCREENSHOTS_ENABLED = "screenshots_enabled";
    String KEY_ONBOARDING_COMPLETE = "onboarding_complete_1";
    String KEY_OVERLAY_TRUSTED = "overlay_trusted";
    String KEY_CONTACTS_INTRODUCTION_COMPLETE = "contacts_intro_complete";
    String KEY_CURRENCY_CRYPTO_STATE = "KEY_CURRENCY_CRYPTO_STATE";
    String KEY_PUSH_NOTIFICATION_ENABLED = "push_notification_enabled";

    String KEY_LATEST_ANNOUNCEMENT_DISMISSED = "latest_announcement_dismissed";
    String KEY_LATEST_ANNOUNCEMENT_SEEN = "latest_announcement_seen";

    String KEY_WARN_ADVANCED_FEE = "pref_warn_advanced_fee";

    String getValue(String name, String value);

    void setValue(String name, String value);

    int getValue(String name, int value);

    void setValue(String name, int value);

    void setValue(String name, long value);

    long getValue(String name, long value);

    boolean getValue(String name, boolean value);

    void setValue(String name, boolean value);

    boolean has(String name);

    void removeValue(String name);

    void clear();

    void logOut();

    void logIn();
}
