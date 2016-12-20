package piuk.blockchain.android.util;

interface PersistentPrefs {

    String DEFAULT_CURRENCY = "USD";

    String KEY_PIN_IDENTIFIER = "pin_kookup_key";
    String KEY_ENCRYPTED_PASSWORD = "encrypted_password";
    String KEY_GUID = "guid";
    String KEY_SHARED_KEY = "sharedKey";
    String KEY_PIN_FAILS = "pin_fails";
    String KEY_BTC_UNITS = "btcUnits";
    String KEY_SELECTED_FIAT = "ccurrency";
    String KEY_INITIAL_ACCOUNT_NAME = "_1ST_ACCOUNT_NAME";
    String KEY_EMAIL = "email";
    String KEY_EMAIL_VERIFIED = "code_verified";
    String KEY_EMAIL_VERIFY_ASK_LATER = "email_verify_ask_later";
    String KEY_BALANCE_DISPLAY_STATE = "balance_display_state";
    String KEY_SCHEME_URL = "scheme_url";
    String KEY_CURRENT_APP_VERSION = "KEY_CURRENT_APP_VERSION";
    String KEY_NEWLY_CREATED_WALLET = "newly_created_wallet";
    String LOGGED_OUT = "logged_out";
    String KEY_BACKEND_ENVIRONMENT = "backend_environment";
    String KEY_FIRST_RUN = "first_run";
    String KEY_SECURITY_TIME_ELAPSED = "security_time_elapsed";
    String KEY_SECURITY_TWO_FA_NEVER = "security_two_fa_never";
    String KEY_SECURITY_BACKUP_NEVER = "security_backup_never";
    String KEY_EVENT_2ND_PW = "event_2nd_pw";
    String KEY_EVENT_LEGACY = "event_legacy";
    String KEY_EVENT_BACKUP = "event_backup";
    String KEY_ENCRYPTED_PIN_CODE = "encrypted_pin_code";
    String KEY_FINGERPRINT_ENABLED = "fingerprint_enabled";
    String KEY_RECEIVE_SHORTCUTS_ENABLED = "receive_shortcuts_enabled";
    String KEY_SWIPE_TO_RECEIVE_ENABLED = "swipe_to_receive_enabled";

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

    void clearPrefsAndKeepEnvironment();

}
