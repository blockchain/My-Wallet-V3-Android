package piuk.blockchain.android.data.access;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.PinStoreService;
import piuk.blockchain.android.ui.auth.LogoutActivity;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class AccessState {

    private static final long LOGOUT_TIMEOUT_MILLIS = 1000L * 30L;
    public static final String LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT";

    private PrefsUtil prefs;
    private PinStoreService pinStore;
    private AppUtil appUtil;
    private String mPin;
    private boolean isLoggedIn = false;
    private PendingIntent logoutPendingIntent;
    private static AccessState instance;
    private static final Subject<AuthEvent> authEventSubject = PublishSubject.create();


    public void initAccessState(Context context, PrefsUtil prefs, PinStoreService pinStore, AppUtil appUtil) {
        this.prefs = prefs;
        this.pinStore = pinStore;
        this.appUtil = appUtil;

        Intent intent = new Intent(context, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(AccessState.LOGOUT_ACTION);
        logoutPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static AccessState getInstance() {
        if (instance == null)
            instance = new AccessState();
        return instance;
    }

    public Observable<Boolean> createPin(CharSequenceX password, String passedPin) {
        return createPinObservable(password, passedPin)
                .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<CharSequenceX> validatePin(String passedPin) {
        mPin = passedPin;

        String key = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encryptedPassword = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");

        return pinStore.validateAccess(key, passedPin)
                .flatMap(jsonObject -> {
                    if (jsonObject.has("success")) {
                        try {
                            String decryptionKey = (String) jsonObject.get("success");
                            return Observable.just(new CharSequenceX(
                                    AESUtil.decrypt(encryptedPassword,
                                            new CharSequenceX(decryptionKey),
                                            AESUtil.PIN_PBKDF2_ITERATIONS)));
                        } catch (Exception e) {
                            throw Exceptions.propagate(new Throwable("Validate access failed"));
                        }
                    } else {
                        throw Exceptions.propagate(new Throwable("Validate access failed"));
                    }
                })
                .compose(RxUtil.applySchedulersToObservable());
    }

    // TODO: 14/10/2016 This should be moved elsewhere in the
    public Observable<Boolean> syncPayloadToServer() {
        return Observable.fromCallable(() -> PayloadManager.getInstance().savePayloadToServer())
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<Boolean> createPinObservable(CharSequenceX password, String passedPin) {
        if (passedPin == null || passedPin.equals("0000") || passedPin.length() != 4) {
            return Observable.just(false);
        }

        mPin = passedPin;
        appUtil.applyPRNGFixes();

        return Observable.create(subscriber -> {
            try {
                byte[] bytes = new byte[16];
                SecureRandom random = new SecureRandom();
                random.nextBytes(bytes);
                String key = new String(Hex.encode(bytes), "UTF-8");
                random.nextBytes(bytes);
                String value = new String(Hex.encode(bytes), "UTF-8");

                pinStore.setAccessKey(key, value, passedPin)
                        .subscribe(success -> {
                            String encryptedPassword = null;
                            try {
                                encryptedPassword = new AESUtilWrapper().encrypt(
                                        password.toString(), new CharSequenceX(value), AESUtil.PIN_PBKDF2_ITERATIONS);

                                prefs.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encryptedPassword);
                                prefs.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, key);

                                if (!subscriber.isDisposed()) {
                                    subscriber.onNext(true);
                                    subscriber.onComplete();
                                }

                            } catch (Exception e) {
                                throw Exceptions.propagate(e);
                            }

                        }, throwable -> {
                            if (!subscriber.isDisposed()) {
                                subscriber.onNext(false);
                                subscriber.onComplete();
                            }
                        });

            } catch (UnsupportedEncodingException e) {
                if (!subscriber.isDisposed()) {
                    subscriber.onError(new Throwable(e));
                }
            }
        });
    }

    public void setPIN(@Nullable String pin) {
        mPin = pin;
    }

    public String getPIN() {
        return mPin;
    }

    /**
     * Called from {@link BaseAuthActivity#onPause()}
     */
    public void startLogoutTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS, logoutPendingIntent);
    }

    /**
     * Called from {@link BaseAuthActivity#onResume()}
     */
    public void stopLogoutTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(logoutPendingIntent);
    }

    public void logout(Context context) {
        mPin = null;
        Intent intent = new Intent(context, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LOGOUT_ACTION);
        context.startActivity(intent);
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean loggedIn) {
        prefs.logIn();
        isLoggedIn = loggedIn;
        if (isLoggedIn) {
            authEventSubject.onNext(AuthEvent.Login);
        } else {
            authEventSubject.onNext(AuthEvent.Logout);
        }
    }

    /**
     * Returns a {@link Subject} that publishes login/logout events
     */
    public Subject<AuthEvent> getAuthEventSubject() {
        return authEventSubject;
    }

    @SuppressWarnings("WeakerAccess")
    public enum AuthEvent {
        Login,
        Logout
    }
}
