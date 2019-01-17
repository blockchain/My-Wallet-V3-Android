package piuk.blockchain.android.ui.settings;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.blockchain.kyc.models.nabu.NabuApiException;
import com.blockchain.kyc.models.nabu.NabuErrorCodes;
import com.blockchain.kycui.settings.KycStatusHelper;
import com.blockchain.notifications.NotificationTokenManager;
import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.settings.SettingsManager;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.auth.AuthDataManager;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater;
import piuk.blockchain.androidcore.data.settings.SettingsDataManager;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcoreui.ui.base.BasePresenter;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AndroidUtils;
import timber.log.Timber;

import javax.inject.Inject;

public class SettingsPresenter extends BasePresenter<SettingsView> {

    private FingerprintHelper fingerprintHelper;
    private AuthDataManager authDataManager;
    private SettingsDataManager settingsDataManager;
    private EmailSyncUpdater emailUpdater;
    private PayloadManager payloadManager;
    private PayloadDataManager payloadDataManager;
    private StringUtils stringUtils;
    private PrefsUtil prefsUtil;
    private AccessState accessState;
    private SwipeToReceiveHelper swipeToReceiveHelper;
    private NotificationTokenManager notificationTokenManager;
    private ExchangeRateDataManager exchangeRateDataManager;
    private CurrencyFormatManager currencyFormatManager;
    private KycStatusHelper kycStatusHelper;
    @VisibleForTesting
    Settings settings;

    @Inject
    SettingsPresenter(FingerprintHelper fingerprintHelper,
                      AuthDataManager authDataManager,
                      SettingsDataManager settingsDataManager,
                      EmailSyncUpdater emailUpdater,
                      PayloadManager payloadManager,
                      PayloadDataManager payloadDataManager,
                      StringUtils stringUtils,
                      PrefsUtil prefsUtil,
                      AccessState accessState,
                      SwipeToReceiveHelper swipeToReceiveHelper,
                      NotificationTokenManager notificationTokenManager,
                      ExchangeRateDataManager exchangeRateDataManager,
                      CurrencyFormatManager currencyFormatManager,
                      KycStatusHelper kycStatusHelper) {

        this.fingerprintHelper = fingerprintHelper;
        this.authDataManager = authDataManager;
        this.settingsDataManager = settingsDataManager;
        this.emailUpdater = emailUpdater;
        this.payloadManager = payloadManager;
        this.payloadDataManager = payloadDataManager;
        this.stringUtils = stringUtils;
        this.prefsUtil = prefsUtil;
        this.accessState = accessState;
        this.swipeToReceiveHelper = swipeToReceiveHelper;
        this.notificationTokenManager = notificationTokenManager;
        this.exchangeRateDataManager = exchangeRateDataManager;
        this.currencyFormatManager = currencyFormatManager;
        this.kycStatusHelper = kycStatusHelper;
    }

    @Override
    public void onViewReady() {
        getView().showProgressDialog(R.string.please_wait);
        // Fetch updated settings
        getCompositeDisposable().add(
                settingsDataManager.fetchSettings()
                        .doAfterTerminate(this::handleUpdate)
                        .doOnNext(ignored -> loadKyc2TierState())
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

    private void loadKyc2TierState() {
        getCompositeDisposable().add(
                kycStatusHelper.getSettingsKycState2Tier()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                settingsKycState -> getView().setKycState(settingsKycState),
                                Timber::e)
        );
    }

    void onKycStatusClicked() {
        getView().launchKycFlow();
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

        // Fiat
        getView().setFiatSummary(getFiatUnits());

        // Email notifications
        getView().setEmailNotificationsVisibility(settings.isEmailVerified());

        // Push and Email notification status
        getView().setEmailNotificationPref(false);

        getView().setPushNotificationPref(isPushNotificationEnabled());

        if (settings.isNotificationsOn() && !settings.getNotificationsType().isEmpty()) {
            for (int type : settings.getNotificationsType()) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL || type == Settings.NOTIFICATION_TYPE_ALL) {
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

    boolean isEmailVerified() {
        return settings.isEmailVerified();
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
                    emailUpdater.updateEmailAndSync(email)
                            .flatMap(e -> settingsDataManager.fetchSettings().singleOrError())
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
                            .doOnNext(settings -> this.settings = settings)
                            .flatMapCompletable(ignored -> syncPhoneNumberWithNabu())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {
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
                        .doOnNext(settings -> this.settings = settings)
                        .flatMapCompletable(ignored -> syncPhoneNumberWithNabu())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doAfterTerminate(() -> {
                            getView().hideProgressDialog();
                            updateUi();
                        })
                        .subscribe(
                                () -> getView().showDialogSmsVerified(),
                                throwable -> getView().showWarningDialog(R.string.verify_sms_failed)));
    }

    private Completable syncPhoneNumberWithNabu() {
        return kycStatusHelper.syncPhoneNumberWithNabu()
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof NabuApiException) {
                        if (((NabuApiException) throwable).getErrorCode() == NabuErrorCodes.AlreadyRegistered) {
                            return Completable.complete();
                        }
                    }

                    return Completable.error(throwable);
                });
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
        payloadManager.setTempPassword(password);

        authDataManager.createPin(password, accessState.getPIN())
                .doOnSubscribe(ignored -> getView().showProgressDialog(R.string.please_wait))
                .doOnTerminate(() -> getView().hideProgressDialog())
                .andThen(payloadDataManager.syncPayloadWithServer())
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        () -> getView().showToast(R.string.password_changed, ToastCustom.TYPE_OK),
                        throwable -> showUpdatePasswordFailed(fallbackPassword));
    }

    private void showUpdatePasswordFailed(@NonNull String fallbackPassword) {
        payloadManager.setTempPassword(fallbackPassword);

        getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
        getView().showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR);
    }

    /**
     * Updates the user's fiat unit preference
     */
    void updateFiatUnit(String fiatUnit) {
        getCompositeDisposable().add(
                settingsDataManager.updateFiatUnit(fiatUnit)
                        .doAfterTerminate(this::updateUi)
                        .subscribe(
                                settings -> {
                                    currencyFormatManager.invalidateFiatCode();
                                    this.settings = settings;
                                },
                                throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    void storeSwipeToReceiveAddresses() {
        getCompositeDisposable().add(
                swipeToReceiveHelper.storeAll()
                        .subscribeOn(Schedulers.computation())
                        .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.please_wait))
                        .doOnTerminate(() -> getView().hideProgressDialog())
                        .subscribe(() -> {
                            // No-op
                        }, throwable -> getView().showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)));
    }

    void clearSwipeToReceiveData() {
        swipeToReceiveHelper.clearStoredData();
    }

    boolean isPushNotificationEnabled() {
        return prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
    }

    void enablePushNotifications() {
        notificationTokenManager.enableNotifications()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnComplete(() -> {
                    getView().setPushNotificationPref(true);
                })
                .subscribe(() -> {
                    //no-op
                }, Timber::e);
    }

    void disablePushNotifications() {

        notificationTokenManager.disableNotifications()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnComplete(() -> {
                    getView().setPushNotificationPref(false);
                })
                .subscribe(() -> {
                    //no-op
                }, Timber::e);
    }

    public String[] getCurrencyLabels() {
        return exchangeRateDataManager.getCurrencyLabels();
    }
}
