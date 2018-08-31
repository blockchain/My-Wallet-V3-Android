package com.blockchain.notifications;

import android.support.annotation.NonNull;
import com.google.common.base.Optional;
import com.google.firebase.iid.FirebaseInstanceId;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.androidcore.data.access.AuthEvent;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import timber.log.Timber;

public class NotificationTokenManager {

    private NotificationService notificationService;
    private PayloadManager payloadManager;
    private PrefsUtil prefsUtil;
    private FirebaseInstanceId firebaseInstanceId;
    private RxBus rxBus;

    private Observable loginObservable;

    public NotificationTokenManager(NotificationService notificationService,
                                    PayloadManager payloadManager,
                                    PrefsUtil prefsUtil,
                                    FirebaseInstanceId firebaseInstanceId,
                                    RxBus rxBus) {
        this.notificationService = notificationService;
        this.payloadManager = payloadManager;
        this.prefsUtil = prefsUtil;
        this.firebaseInstanceId = firebaseInstanceId;
        this.rxBus = rxBus;
    }

    /**
     * Sends the access token to the update-firebase endpoint once the user is logged in fully, as
     * the token is generally only generated on first install or first start after updating.
     *
     * @param token A Firebase access token
     */
    void storeAndUpdateToken(@NonNull String token) {
        prefsUtil.setValue(PrefsUtil.KEY_FIREBASE_TOKEN, token);
        if (!token.isEmpty()) {
            sendFirebaseToken(token)
                    .subscribeOn(Schedulers.io())
                    .subscribe(() -> {
                        //no-op
                    }, Timber::e);
        }
    }

    public void registerAuthEvent() {
        loginObservable = rxBus.register(AuthEvent.class);
        loginObservable
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(authEvent -> {
                    if (authEvent == AuthEvent.LOGIN) {
                        String storedToken = prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "");

                        if (!storedToken.isEmpty()) {
                            return sendFirebaseToken(storedToken);
                        } else {
                            return resendNotificationToken();
                        }

                    } else if (authEvent == AuthEvent.FORGET) {
                        prefsUtil.removeValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED);
                        return revokeAccessToken();
                    } else {
                        return Completable.complete();
                    }
                })
                .subscribe(() -> {
                    //no-op
                }, Timber::e);
    }

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by {@link InstanceIdService}
     *
     * @return The Firebase token
     */
    private Observable<Optional<String>> getStoredFirebaseToken() {
        String storedToken = prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "");

        if (!storedToken.isEmpty()) {
            return Observable.just(Optional.of(storedToken));
        } else {
            return Observable.fromCallable(() -> {
                firebaseInstanceId.getToken();
                return Optional.absent();
            });
        }
    }

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    public Completable disableNotifications() {
        prefsUtil.setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, false);
        return revokeAccessToken()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    private Completable revokeAccessToken() {
        return Completable.fromCallable(() -> {
            firebaseInstanceId.deleteInstanceId();
            return Void.TYPE;
        }).andThen(removeNotificationToken())
                .doOnComplete(this::clearStoredToken)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Enables push notifications flag.
     * Resend stored notification token, or generate and send new token if no stored token exists.
     */
    public Completable enableNotifications() {
        prefsUtil.setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        return resendNotificationToken();
    }

    /**
     * Re-sends the notification token. The token is only ever generated on app installation, so it
     * may be preferable to store the token and resend it on startup rather than have an app end up
     * in a state where it may not have registered with the right endpoint or similar.
     * <p>
     * If no stored notification token exists, it will be refreshed
     * and will be handled appropriately by FcmCallbackService
     */
    private Completable resendNotificationToken() {
        return getStoredFirebaseToken()
                .flatMapCompletable(optional -> {
                    if (optional.isPresent()) {
                        return sendFirebaseToken(optional.get());
                    } else {
                        return Completable.complete();
                    }
                });
    }

    private Completable sendFirebaseToken(@NonNull String refreshedToken) {
        if (prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)
                && payloadManager.getPayload() != null) {

            Wallet payload = payloadManager.getPayload();
            String guid = payload.getGuid();
            String sharedKey = payload.getSharedKey();

            // TODO: 09/11/2016 Decide what to do if sending fails, perhaps retry?
            return notificationService.sendNotificationToken(refreshedToken, guid, sharedKey)
                    .subscribeOn(Schedulers.io());
        } else {
            return Completable.complete();
        }
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private void clearStoredToken() {
        prefsUtil.removeValue(PrefsUtil.KEY_FIREBASE_TOKEN);
    }

    /**
     * Removes the stored token from back end
     */
    private Completable removeNotificationToken() {

        String token = prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "");

        if (!token.isEmpty()) {
            return notificationService.removeNotificationToken(token);
        } else {
            return Completable.complete();
        }
    }
}
