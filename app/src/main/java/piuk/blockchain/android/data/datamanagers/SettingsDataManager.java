package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.Settings;

import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.SettingsService;
import io.reactivex.Observable;

public class SettingsDataManager {

    private SettingsService settingsService;

    public SettingsDataManager(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Updates the settings object by syncing it with the server
     *
     * @param guid      The user's GUID
     * @param sharedKey The shared key
     * @return {@link Observable < Settings >} wrapping the Settings object
     */
    public Observable<Settings> updateSettings(String guid, String sharedKey) {
        return settingsService.updateSettings(guid, sharedKey)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's email
     *
     * @param email The email to be stored
     * @return An {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updateEmail(String email) {
        return settingsService.updateEmail(email)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's phone number
     *
     * @param sms The phone number to be stored
     * @return An {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updateSms(String sms) {
        return settingsService.updateSms(sms)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Verify the user's phone number with a verification code
     *
     * @param code The verification code
     * @return An {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> verifySms(String code) {
        return settingsService.verifySms(code)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's Tor blocking preference
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updateTor(boolean blocked) {
        return settingsService.updateTor(blocked)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's password hint
     *
     * @param hint The user's password hint
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updatePasswordHint(String hint) {
        return settingsService.updatePasswordHint(hint)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Update the user's notification preferences
     *
     * @param notificationType The type of notification to enable
     * @param enabled          Whether to enable or disable this particular notification type
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     * @see {@link Settings}
     */
    public Observable<Boolean> updateNotifications(int notificationType, boolean enabled) {
        if (enabled) {
            return settingsService.enableNotifications(notificationType)
                    .compose(RxUtil.applySchedulersToObservable());
        } else {
            return settingsService.disableNotifications(notificationType)
                    .compose(RxUtil.applySchedulersToObservable());
        }
    }

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     * @see {@link Settings}
     */
    public Observable<Boolean> updateTwoFactor(int authType) {
        return settingsService.updateTwoFactor(authType)
                .compose(RxUtil.applySchedulersToObservable());
    }
}
