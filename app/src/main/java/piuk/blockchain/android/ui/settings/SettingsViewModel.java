package piuk.blockchain.android.ui.settings;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.api.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class SettingsViewModel extends BaseViewModel {

    @Inject protected FingerprintHelper fingerprintHelper;
    @Inject protected SettingsDataManager settingsDataManager;
    @Inject protected PayloadManager payloadManager;
    @Inject protected StringUtils stringUtils;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected AccessState accessState;
    @VisibleForTesting Settings settings;
    private DataListener dataListener;
    private MonetaryUtil monetaryUtil;

    interface DataListener {

        void setUpUi();

        void verifyPinCode();

        void showFingerprintDialog(CharSequenceX pincode);

        void showDisableFingerprintDialog();

        void updateFingerprintPreferenceStatus();

        void showNoFingerprintsAddedDialog();

        void showProgressDialog(@StringRes int message);

        void hideProgressDialog();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void setGuidSummary(String summary);

        void setEmailSummary(String summary);

        void setSmsSummary(String summary);

        void setUnitsSummary(String summary);

        void setFiatSummary(String summary);

        void setEmailNotificationsVisibility(boolean visible);

        void setSmsNotificationsVisibility(boolean visible);

        void setEmailNotificationPref(boolean enabled);

        void setSmsNotificationPref(boolean enabled);

        void setFingerprintVisibility(boolean visible);

        void setTwoFaPreference(boolean enabled);

        void setTwoFaSummary(String summary);

        void setPasswordHintSummary(String summary);

        void setTorBlocked(boolean blocked);

        void showDialogEmailVerification();

        void showDialogVerifySms();

        void showDialogSmsVerified();

        void goToPinEntryPage();

        void setLauncherShortcutVisibility(boolean visible);

        void showWarningDialog(@StringRes int message);
    }

    SettingsViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    @Override
    public void onViewReady() {
        dataListener.showProgressDialog(R.string.please_wait);
        // Fetch updated settings
        compositeDisposable.add(
                settingsDataManager.updateSettings(
                        payloadManager.getPayload().getGuid(),
                        payloadManager.getPayload().getSharedKey())
                        .doAfterTerminate(() -> {
                            dataListener.hideProgressDialog();
                            dataListener.setUpUi();
                            updateUi();
                        })
                        .subscribe(
                                updatedSettings -> settings = updatedSettings,
                                throwable -> settings = new Settings()));
    }

    private void updateUi() {
        // GUID
        dataListener.setGuidSummary(settings.getGuid());

        // Email
        String emailAndStatus = settings.getEmail();
        if (emailAndStatus == null || emailAndStatus.isEmpty()) {
            emailAndStatus = stringUtils.getString(R.string.not_specified);
        } else if (settings.isEmailVerified()) {
            emailAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")";
        } else {
            emailAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")";
        }
        dataListener.setEmailSummary(emailAndStatus);

        // Phone
        String smsAndStatus = settings.getSms();
        if (smsAndStatus == null || smsAndStatus.isEmpty()) {
            smsAndStatus = stringUtils.getString(R.string.not_specified);
        } else if (settings.isSmsVerified()) {
            smsAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")";
        } else {
            smsAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")";
        }
        dataListener.setSmsSummary(smsAndStatus);

        // Units
        dataListener.setUnitsSummary(getDisplayUnits());

        // Fiat
        dataListener.setFiatSummary(getFiatUnits());

        // Email notifications
        dataListener.setEmailNotificationsVisibility(settings.isEmailVerified());

        // SMS notifications
        dataListener.setSmsNotificationsVisibility(settings.isSmsVerified());

        // SMS and Email notification status
        dataListener.setEmailNotificationPref(false);
        dataListener.setSmsNotificationPref(false);

        if (settings.isNotificationsOn() && settings.getNotificationTypes().size() > 0) {
            for (int type : settings.getNotificationTypes()) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL) {
                    dataListener.setEmailNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_SMS) {
                    dataListener.setSmsNotificationPref(true);
                }

                if (type == Settings.NOTIFICATION_TYPE_ALL) {
                    dataListener.setSmsNotificationPref(true);
                    dataListener.setEmailNotificationPref(true);
                    break;
                }
            }
        }

        // Fingerprint
        dataListener.setFingerprintVisibility(getIfFingerprintHardwareAvailable());
        dataListener.updateFingerprintPreferenceStatus();

        // 2FA
        dataListener.setTwoFaPreference(settings.getAuthType() != Settings.AUTH_TYPE_OFF);
        dataListener.setTwoFaSummary(getTwoFaSummary(settings.getAuthType()));

        // Password hint
        if (settings.getPasswordHint1() != null && !settings.getPasswordHint1().isEmpty()) {
            dataListener.setPasswordHintSummary(settings.getPasswordHint1());
        } else {
            dataListener.setPasswordHintSummary("");
        }

        // Tor
        dataListener.setTorBlocked(settings.isTorBlocked());

        // Launcher shortcuts
        dataListener.setLauncherShortcutVisibility(AndroidUtils.is25orHigher());
    }

    private String getTwoFaSummary(int type) {
        String summary;
        switch (type) {
            case Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR:
                summary = stringUtils.getString(R.string.google_authenticator);
                break;
            case Settings.AUTH_TYPE_SMS:
                summary = stringUtils.getString(R.string.sms);
                break;
            case Settings.AUTH_TYPE_YUBI_KEY:
                summary = stringUtils.getString(R.string.yubikey);
                break;
            default:
                summary = "";
                break;
        }
        return summary;
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
        return fingerprintHelper.getIfFingerprintUnlockEnabled();
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
            dataListener.showDisableFingerprintDialog();
        } else if (!fingerprintHelper.areFingerprintsEnrolled()) {
            // No fingerprints enrolled, prompt user to add some
            dataListener.showNoFingerprintsAddedDialog();
        } else {
            // Verify PIN before continuing
            dataListener.verifyPinCode();
        }
    }

    /**
     * Displays fingerprint dialog after the PIN has been validated by {@link
     * piuk.blockchain.android.ui.auth.PinEntryActivity}
     *
     * @param pinCode A {@link CharSequenceX} wrapping the validated PIN code
     */
    void pinCodeValidatedForFingerprint(CharSequenceX pinCode) {
        dataListener.showFingerprintDialog(pinCode);
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
    String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[getBtcUnitsPosition()];
    }

    /**
     * @return an array of possible BTC units
     */
    @NonNull
    CharSequence[] getBtcUnits() {
        return monetaryUtil.getBTCUnits();
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
    CharSequenceX getTempPassword() {
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
        return settings.getSms() != null ? settings.getSms() : "";
    }

    /**
     * @return the user's password hint or an empty string if not set
     */
    @NonNull
    String getPasswordHint() {
        return settings.getPasswordHint1() != null ? settings.getPasswordHint1() : "";
    }

    /**
     * @return is the user's phone number is verified
     */
    boolean isSmsVerified() {
        return settings.isSmsVerified();
    }

    /**
     * @return the current auth type
     * @see {@link Settings}
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
     * Updates the user's email, prompts user to check their email for verification after success
     *
     * @param email The email address to be saved
     */
    void updateEmail(String email) {
        if (!isStringValid(email)) {
            dataListener.setEmailSummary(stringUtils.getString(R.string.not_specified));
        } else {
            compositeDisposable.add(
                    settingsDataManager.updateEmail(email)
                            .subscribe(success -> {
                                if (success) {
                                    updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false);
                                    dataListener.showDialogEmailVerification();
                                } else {
                                    throw Exceptions.propagate(new Throwable("Update email failed"));
                                }
                            }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Updates the user's phone number, prompts user to verify their number after success
     *
     * @param sms The phone number to be saved
     */
    void updateSms(String sms) {
        if (!isStringValid(sms)) {
            dataListener.setSmsSummary(stringUtils.getString(R.string.not_specified));
        } else {
            compositeDisposable.add(
                    settingsDataManager.updateSms(sms)
                            .subscribe(success -> {
                                if (success) {
                                    updateNotification(Settings.NOTIFICATION_TYPE_SMS, false);
                                    dataListener.showDialogVerifySms();
                                } else {
                                    throw Exceptions.propagate(new Throwable("Update SMS failed"));
                                }
                            }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Verifies a user's number, shows verified dialog after success
     *
     * @param code The verification code which has been sent to the user
     */
    void verifySms(@NonNull String code) {
        dataListener.showProgressDialog(R.string.please_wait);
        compositeDisposable.add(
                settingsDataManager.verifySms(code)
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(success -> {
                            if (success) {
                                dataListener.showDialogSmsVerified();
                                updateUi();
                            } else {
                                dataListener.showWarningDialog(R.string.verify_sms_failed);
                            }
                        }, throwable -> dataListener.showWarningDialog(R.string.verify_sms_failed)));
    }

    /**
     * Updates the user's Tor blocking preference
     *
     * @param blocked Whether or not to block Tor requests
     */
    void updateTor(boolean blocked) {
        compositeDisposable.add(
                settingsDataManager.updateTor(blocked)
                        .subscribe(success -> {
                            if (success) {
                                updateUi();
                            } else {
                                throw Exceptions.propagate(new Throwable("Update TOR failed"));
                            }
                        }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Updates the user's password hint
     *
     * @param hint The new password hint
     */
    void updatePasswordHint(String hint) {
        if (!isStringValid(hint)) {
            dataListener.showToast(R.string.settings_field_cant_be_empty, ToastCustom.TYPE_ERROR);
        } else {
            compositeDisposable.add(
                    settingsDataManager.updatePasswordHint(hint)
                            .subscribe(success -> {
                                if (success) {
                                    updateUi();
                                } else {
                                    throw Exceptions.propagate(new Throwable("Update password hint failed"));
                                }
                            }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    /**
     * Sets the auth type used for 2FA. Pass in {@link Settings#AUTH_TYPE_OFF} to disable 2FA
     *
     * @param type The auth type used for 2FA
     * @see {@link Settings}
     */
    void updateTwoFa(int type) {
        compositeDisposable.add(
                settingsDataManager.updateTwoFactor(type)
                        .subscribe(success -> {
                            if (success) {
                                updateUi();
                            } else {
                                throw Exceptions.propagate(new Throwable("Update 2FA failed"));
                            }
                        }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Updates the user's notification preferences
     *
     * @param type    The notification type to be updated
     * @param enabled Whether or not to enable the notification type
     * @see {@link Settings}
     */
    void updateNotification(int type, boolean enabled) {
        compositeDisposable.add(
                settingsDataManager.updateNotifications(type, enabled)
                        .subscribe(success -> {
                            if (success) {
                                updateUi();
                            } else {
                                throw Exceptions.propagate(new Throwable("Update notification failed"));
                            }
                        }, throwable -> dataListener.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    /**
     * PIN code validated, take user to PIN change page
     */
    void pinCodeValidatedForChange() {
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

        dataListener.goToPinEntryPage();
    }

    /**
     * Updates the user's password
     *
     * @param password         The requested new password as a {@link CharSequenceX}
     * @param fallbackPassword The user's current password as a fallback
     */
    void updatePassword(@NonNull CharSequenceX password, @NonNull CharSequenceX fallbackPassword) {
        dataListener.showProgressDialog(R.string.please_wait);
        payloadManager.setTempPassword(password);

        compositeDisposable.add(
                accessState.createPin(password, accessState.getPIN())
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .flatMap(success -> {
                            if (success) {
                                return accessState.syncPayloadToServer();
                            } else {
                                return Observable.just(false);
                            }
                        })
                        .subscribe(success -> {
                            if (success) {
                                dataListener.showToast(R.string.password_changed, ToastCustom.TYPE_OK);
                            } else {
                                showUpdatePasswordFailed(fallbackPassword);
                            }
                        }, throwable -> showUpdatePasswordFailed(fallbackPassword)));
    }

    private void showUpdatePasswordFailed(@NonNull CharSequenceX fallbackPassword) {
        payloadManager.setTempPassword(fallbackPassword);

        dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
        dataListener.showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR);
    }
}
