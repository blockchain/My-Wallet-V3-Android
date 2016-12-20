package piuk.blockchain.android.data.services;

import info.blockchain.api.Settings;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class SettingsService {

    private Settings settingsApi;

    public SettingsService(Settings settingsApi) {
        this.settingsApi = settingsApi;
    }

    /**
     * Updates the settings object by syncing it with the server
     *
     * @param guid      The user's GUID
     * @param sharedKey The shared key
     * @return {@link Observable<Settings>} wrapping the Settings object
     */
    public Observable<Settings> updateSettings(String guid, String sharedKey) {
        return Observable.fromCallable(() -> new Settings(guid, sharedKey))
                .doOnNext(settings -> settingsApi = settings)
                .flatMap(settings -> Observable.just(settingsApi));
    }

    /**
     * Update the user's email
     *
     * @param email The email to be stored
     * @return An {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updateEmail(String email) {
        return Observable.create(subscriber -> settingsApi.setEmail(email, new SettingsResultListener(subscriber)));
    }

    /**
     * Update the user's phone number
     *
     * @param sms The phone number to be stored
     * @return An {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updateSms(String sms) {
        return Observable.create(subscriber -> settingsApi.setSms(sms, new SettingsResultListener(subscriber)));
    }

    /**
     * Verify the user's phone number with a verification code
     *
     * @param code The verification code
     * @return An {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> verifySms(String code) {
        return Observable.create(subscriber -> settingsApi.verifySms(code, new SettingsResultListener(subscriber)));
    }

    /**
     * Update the user's Tor blocking preference
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updateTor(boolean blocked) {
        return Observable.create(subscriber -> settingsApi.setTorBlocked(blocked, new SettingsResultListener(subscriber)));
    }

    /**
     * Update the user's password hint
     *
     * @param hint The user's password hint
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     */
    public Observable<Boolean> updatePasswordHint(String hint) {
        return Observable.create(subscriber -> settingsApi.setPasswordHint1(hint, new SettingsResultListener(subscriber)));
    }

    /**
     * Disable a specific notification type for a user
     *
     * @param notificationType The type of notification to disable
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     * @see Settings
     */
    public Observable<Boolean> disableNotifications(int notificationType) {
        return Observable.create(subscriber -> settingsApi.disableNotification(notificationType, new SettingsResultListener(subscriber)));
    }

    /**
     * Enable a specific notification type for a user
     *
     * @param notificationType The type of notification to enable
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     * @see Settings
     */
    public Observable<Boolean> enableNotifications(int notificationType) {
        return Observable.create(subscriber -> settingsApi.enableNotification(notificationType, new SettingsResultListener(subscriber)));
    }

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return A {@link Observable<Boolean>}, where the boolean represents a successful save or not
     * @see Settings
     */
    public Observable<Boolean> updateTwoFactor(int authType) {
        return Observable.create(subscriber -> settingsApi.setAuthType(authType, new SettingsResultListener(subscriber)));
    }

    private static class SettingsResultListener implements Settings.ResultListener {

        private final ObservableEmitter<? super Boolean> subscriber;

        SettingsResultListener(ObservableEmitter<? super Boolean> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSuccess() {
            if (!subscriber.isDisposed()) {
                subscriber.onNext(true);
                subscriber.onComplete();
            }
        }

        @Override
        public void onFail() {
            if (!subscriber.isDisposed()) {
                subscriber.onNext(false);
                subscriber.onComplete();
            }
        }

        @Override
        public void onBadRequest() {
            if (!subscriber.isDisposed()) {
                subscriber.onError(new Throwable("Request invalid"));
            }
        }
    }
}
