package piuk.blockchain.android.ui.settings;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.settings.SettingsManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

public class SettingsPresenter extends BasePresenter<SettingsView> {

    private FingerprintHelper fingerprintHelper;
    private AuthDataManager authDataManager;
    private SettingsDataManager settingsDataManager;
    private PayloadManager payloadManager;
    private PayloadDataManager payloadDataManager;
    private StringUtils stringUtils;
    private PrefsUtil prefsUtil;
    private AccessState accessState;
    private MonetaryUtil monetaryUtil;
    @VisibleForTesting Settings settings;

    @Inject
    SettingsPresenter(FingerprintHelper fingerprintHelper,
                      AuthDataManager authDataManager,
                      SettingsDataManager settingsDataManager,
                      PayloadManager payloadManager,
                      PayloadDataManager payloadDataManager,
                      StringUtils stringUtils,
                      PrefsUtil prefsUtil,
                      AccessState accessState) {

        this.fingerprintHelper = fingerprintHelper;
        this.authDataManager = authDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadManager = payloadManager;
        this.payloadDataManager = payloadDataManager;
        this.stringUtils = stringUtils;
        this.prefsUtil = prefsUtil;
        this.accessState = accessState;

        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    @Override
    public void onViewReady() {
        getView().showProgressDialog(R.string.please_wait);
        // Fetch updated settings
        getCompositeDisposable().add(
                settingsDataManager.getSettings()
                        .doAfterTerminate(this::handleUpdate)
                        .subscribe(
                                updatedSettings -> settings = updatedSettings,
                                throwable -> {
                                    if (settings == null) {
                                        // Show unloaded if necessary, keep old settings if failed update
                                        settings = new Settings();
                                    }
                                    // Warn error when updating
                                    getView().showToast(R.string.settings_error_updating, ToastCustom.TYPE_ERROR);
                                }));
    }

    private void handleUpdate() {
        getView().hideProgressDialog();
        getView().setUpUi();
        updateUi();
    }

    private void updateUi() {
        // GUID
        getView().setGuidSummary(settings.getGuid());

        // Email
        String emailAndStatus = settings.getEmail();
        if (emailAndStatus == null || emailAndStatus.isEmpty()) {
            emailAndStatus = stringUtils.getString(R.string.not_specified);
        } else if (settings.isEmailVerified()) {
            emailAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")";
        } else {
            emailAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")";
        }
        getView().setEmailSummary(emailAndStatus);

        // Phone
        String smsAndStatus = settings.getSmsNumber();
        if (smsAndStatus == null || smsAndStatus.isEmpty()) {
            smsAndStatus = stringUtils.getString(R.string.not_specified);
        } else if (settings.isSmsVerified()) {
            smsAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")";
        } else {
            smsAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")";
        }
        getView().setSmsSummary(smsAndStatus);

        // Units
        getView().setUnitsSummary(getDisplayUnits());

        // Fiat
        getView().setFiatSummary(getFiatUnits());

        // Email notifications
        getView().setEmailNotificationsVisibility(settings.isEmailVerified());

        // SMS notifications
        getView().setSmsNotificationsVisibility(settings.isSmsVerified());

        // SMS and Email notification status
        getView().setEmailNotificationPref(false);
        getView().setSmsNotificationPref(false);

        if (settings.isNotificationsOn() && !settings.getNotificationsType().isEmpty()) {
            for (int type : settings.getNotificationsType()) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL) {
                    getView().setEmailNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_SMS) {
                    getView().setSmsNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_ALL) {
                    getView().setSmsNotificationPref(true);
                    getView().setEmailNotificationPref(true);
                    break;
                }
            }
        }

        // Fingerprint
        getView().setFingerprintVisibility(getIfFingerprintHardwareAvailable());
        getView().updateFingerprintPreferenceStatus();

        // 2FA
        getView().setTwoFaPreference(settings.getAuthType() != Settings.AUTH_TYPE_OFF);

        // Tor
        getView().setTorBlocked(settings.isBlockTorIps());

        // Screenshots
        getView().setScreenshotsEnabled(prefsUtil.getValue(PrefsUtil.KEY_SCREENSHOTS_ENABLED, false));

        // Launcher shortcuts
        getView().setLauncherShortcutVisibility(AndroidUtils.is25orHigher());
    }

    /**
     * @return true if the device has usable fingerprint hardware
     */
    boolean getIfFingerprintHardwareAvailable() {
        return fingerprintHelper.isHardwareDetected();
    }

    /**
     * @return true if the user has previously enabled fingerprint login
     */
    boolean getIfFingerprintUnlockEnabled() {
        return fingerprintHelper.isFingerprintUnlockEnabled();
    }

    /**
     * Sets fingerprint unlock enabled and clears the encrypted PIN if {@param enabled} is false
     *
     * @param enabled Whether or not the fingerprint unlock feature is set up
     */
    void setFingerprintUnlockEnabled(boolean enabled) {
        fingerprintHelper.setFingerprintUnlockEnabled(enabled);
        if (!enabled) {
            fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        }
    }

    /**
     * Handle fingerprint preference toggle
     */
    void onFingerprintClicked() {
        if (getIfFingerprintUnlockEnabled()) {
            // Show dialog "are you sure you want to disable fingerprint login?
            getView().showDisableFingerprintDialog();
        } else if (!fingerprintHelper.areFingerprintsEnrolled()) {
            // No fingerprints enrolled, prompt user to add some
            getView().showNoFingerprintsAddedDialog();
        } else {
            if (accessState.getPIN() != null && !accessState.getPIN().isEmpty()) {
                getView().showFingerprintDialog(accessState.getPIN());
            } else {
                throw new IllegalStateException("PIN code not found in AccessState");
            }
        }
    }

    private boolean isStringValid(String string) {
        return string != null && !string.isEmpty() && string.length() < 256;
    }

    /**
     * @return position of user's BTC unit preference
     */
    int getBtcUnitsPosition() {
        return prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
    }

    /**
     * @return the user's preferred BTC units
     */
    @NonNull
    private String getDisplayUnits() {
        return monetaryUtil.getBtcUnits()[getBtcUnitsPosition()];
    }

    /**
     * @return an array of possible BTC units
     */
    @NonNull
    CharSequence[] getBtcUnits() {
        return monetaryUtil.getBtcUnits();
    }

    /**
     * @return the user's preferred Fiat currency unit
     */
    @NonNull
    String getFiatUnits() {
        return prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    /**
     * @return the temporary password from the Payload Manager
     */
    @NonNull
    String getTempPassword() {
        return payloadManager.getTempPassword();
    }

    /**
     * @return the user's email or an empty string if not set
     */
    @NonNull
    String getEmail() {
        return settings.getEmail() != null ? settings.getEmail() : "";
    }

    /**
     * @return the user's phone number or an empty string if not set
     */
    @NonNull
    String getSms() {
        return settings.getSmsNumber() != null ? settings.getSmsNumber() : "";
    }

    /**
     * @return is the user's phone number is verified
     */
    boolean isSmsVerified() {
        return settings.isSmsVerified();
    }

    /**
     * @return the current auth type
     * @see Settings
     */
    int getAuthType() {
        return settings.getAuthType();
    }

    /**
     * Write key/value to {@link android.content.SharedPreferences}
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as a String
     */
    void updatePreferences(String key, String value) {
        prefsUtil.setValue(key, value);
        updateUi();
    }

    /**
     * Write key/value to {@link android.content.SharedPreferences}
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as an int
     */
    void updatePreferences(String key, int value) {
        prefsUtil.setValue(key, value);
        updateUi();
    }

    /**
     * Write key/value to {@link android.content.SharedPreferences}
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as a boolean
     */
    void updatePreferences(String key, boolean value) {
        prefsUtil.setValue(key, value);
        updateUi();
    }

    /**
     * Updates the user's email, prompts user to check their email for verification after success
     *
     * @param email The email address to be saved
     */
    void updateEmail(String email) {
        if (!isStringValid(email)) {
            getView().setEmailSummary(stringUtils.getString(R.string.not_specified));
        } else {
            getCompositeDisposable().add(
                    settingsDataManager.updateEmail(email)
                            .subscribe(settings -> {
                                this.settings = settings;
                                updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false);
                                getView().showDialogEmailVerification();
                            }, throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Updates the user's phone number, prompts user to verify their number after success
     *
     * @param sms The phone number to be saved
     */
    void updateSms(String sms) {
        if (!isStringValid(sms)) {
            getView().setSmsSummary(stringUtils.getString(R.string.not_specified));
        } else {
            getCompositeDisposable().add(
                    settingsDataManager.updateSms(sms)
                            .subscribe(settings -> {
                                this.settings = settings;
                                updateNotification(Settings.NOTIFICATION_TYPE_SMS, false);
                                getView().showDialogVerifySms();
                            }, throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Verifies a user's number, shows verified dialog after success
     *
     * @param code The verification code which has been sent to the user
     */
    void verifySms(@NonNull String code) {
        getView().showProgressDialog(R.string.please_wait);
        getCompositeDisposable().add(
                settingsDataManager.verifySms(code)
                        .doAfterTerminate(() -> {
                            getView().hideProgressDialog();
                            updateUi();
                        })
                        .subscribe(settings -> {
                            this.settings = settings;
                            getView().showDialogSmsVerified();
                        }, throwable -> getView().showWarningDialog(R.string.verify_sms_failed)));
    }

    /**
     * Updates the user's Tor blocking preference
     *
     * @param blocked Whether or not to block Tor requests
     */
    void updateTor(boolean blocked) {
        getCompositeDisposable().add(
                settingsDataManager.updateTor(blocked)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> this.settings = settings,
                                throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Sets the auth type used for 2FA. Pass in {@link Settings#AUTH_TYPE_OFF} to disable 2FA
     *
     * @param type The auth type used for 2FA
     * @see Settings
     */
    void updateTwoFa(int type) {
        getCompositeDisposable().add(
                settingsDataManager.updateTwoFactor(type)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> this.settings = settings,
                                throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Updates the user's notification preferences. Will not make any web requests if not necessary.
     *
     * @param type   The notification type to be updated
     * @param enable Whether or not to enable the notification type
     * @see Settings
     */
    void updateNotification(int type, boolean enable) {
        if (enable && isNotificationTypeEnabled(type)) {
            // No need to change
            updateUi();
            return;
        } else if (!enable && isNotificationTypeDisabled(type)) {
            // No need to change
            updateUi();
            return;
        }

        getCompositeDisposable().add(
                Observable.just(enable)
                        .flatMap(aBoolean -> {
                            if (aBoolean) {
                                return settingsDataManager.enableNotification(type, settings.getNotificationsType());
                            } else {
                                return settingsDataManager.disableNotification(type, settings.getNotificationsType());
                            }
                        })
                        .doOnNext(settings -> this.settings = settings)
                        .flatMapCompletable(ignored -> {
                            if (enable) {
                                return payloadDataManager.syncPayloadAndPublicKeys();
                            } else {
                                return payloadDataManager.syncPayloadWithServer();
                            }
                        })
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                () -> {
                                    // No-op
                                },
                                throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    private boolean isNotificationTypeEnabled(int type) {
        return settings.isNotificationsOn()
                && (settings.getNotificationsType().contains(type)
                || settings.getNotificationsType().contains(SettingsManager.NOTIFICATION_TYPE_ALL));
    }

    private boolean isNotificationTypeDisabled(int type) {
        return settings.getNotificationsType().contains(SettingsManager.NOTIFICATION_TYPE_NONE)
                || (!settings.getNotificationsType().contains(SettingsManager.NOTIFICATION_TYPE_ALL)
                && !settings.getNotificationsType().contains(type));
    }

    /**
     * PIN code validated, take user to PIN change page
     */
    void pinCodeValidatedForChange() {
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

        getView().goToPinEntryPage();
    }

    /**
     * Updates the user's password
     *
     * @param password         The requested new password as a {@link String}
     * @param fallbackPassword The user's current password as a fallback
     */
    void updatePassword(@NonNull String password, @NonNull String fallbackPassword) {
        getView().showProgressDialog(R.string.please_wait);
        payloadManager.setTempPassword(password);

        getCompositeDisposable().add(
                authDataManager.createPin(password, accessState.getPIN())
                        .andThen(payloadDataManager.syncPayloadWithServer())
                        .doAfterTerminate(() -> getView().hideProgressDialog())
                        .subscribe(
                                () -> getView().showToast(R.string.password_changed, ToastCustom.TYPE_OK),
                                throwable -> showUpdatePasswordFailed(fallbackPassword)));
    }

    private void showUpdatePasswordFailed(@NonNull String fallbackPassword) {
        payloadManager.setTempPassword(fallbackPassword);

        getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
        getView().showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR);
    }

    /**
     * Updates the user's btcUnit unit preference
     */
    void updateBtcUnit(int btcUnitIndex) {

        String btcUnit = Settings.UNIT_BTC;

        switch (btcUnitIndex) {
            case 0:
                btcUnit = Settings.UNIT_BTC;
                break;
            case 1:
                btcUnit = Settings.UNIT_MBC;
                break;
            case 2:
                btcUnit = Settings.UNIT_UBC;
                break;
        }

        getCompositeDisposable().add(
                settingsDataManager.updateBtcUnit(btcUnit)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> this.settings = settings,
                                throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Updates the user's fiat unit preference
     */
    void updateFiatUnit(String fiatUnit) {
        getCompositeDisposable().add(
                settingsDataManager.updateFiatUnit(fiatUnit)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> this.settings = settings,
                                throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }
}
