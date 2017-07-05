package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.api.data.WalletOptions;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;

import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.Exceptions;
import okhttp3.ResponseBody;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.annotations.Thunk;
import retrofit2.Response;

@SuppressWarnings("WeakerAccess")
public class AuthDataManager {

    @VisibleForTesting static final String AUTHORIZATION_REQUIRED = "authorization_required";

    private WalletService walletService;
    private AppUtil appUtil;
    private AccessState accessState;
    private StringUtils stringUtils;
    private PayloadDataManager payloadDataManager;
    private RxPinning rxPinning;
    private AESUtilWrapper aesUtilWrapper;
    @Thunk PrefsUtil prefsUtil;
    @VisibleForTesting protected int timer;

    public AuthDataManager(PayloadDataManager payloadDataManager,
                           PrefsUtil prefsUtil,
                           WalletService walletService,
                           AppUtil appUtil,
                           AccessState accessState,
                           StringUtils stringUtils,
                           AESUtilWrapper aesUtilWrapper,
                           RxBus rxBus) {

        this.payloadDataManager = payloadDataManager;
        this.prefsUtil = prefsUtil;
        this.walletService = walletService;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.stringUtils = stringUtils;
        this.aesUtilWrapper = aesUtilWrapper;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Attempts to retrieve an encrypted Payload from the server, but may also return just part of a
     * Payload or an error response.
     *
     * @param guid      The user's unique GUID
     * @param sessionId The current session ID
     * @return An {@link Observable} wrapping a {@link Response<ResponseBody>} which could notify
     * the user that authentication (ie checking your email, 2FA etc) is required
     * @see #getSessionId(String)
     */
    public Observable<Response<ResponseBody>> getEncryptedPayload(String guid, String sessionId) {
        return rxPinning.call(() -> walletService.getEncryptedPayload(guid, sessionId))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Gets an ephemeral session ID from the server.
     *
     * @param guid The user's unique GUID
     * @return An {@link Observable} wrapping a session ID as a String
     */
    public Observable<String> getSessionId(String guid) {
        return rxPinning.call(() -> walletService.getSessionId(guid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Submits a user's 2FA code to the server and returns a response. This response will contain
     * the user's encrypted Payload if successful, if not it will contain an error.
     *
     * @param sessionId     The current session ID
     * @param guid          The user's unique GUID
     * @param twoFactorCode A valid 2FA code generated from Google Authenticator or similar
     * @see #getSessionId(String)
     */
    public Observable<ResponseBody> submitTwoFactorCode(String sessionId, String guid, String twoFactorCode) {
        return rxPinning.call(() -> walletService.submitTwoFactorCode(sessionId, guid, twoFactorCode))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Fetches the user's wallet payload, and then initializes the {@link PayloadManager} and
     * decrypts a payload using the user's password.
     *
     * @param sharedKey The shared key negotiated between the server and the user
     * @param guid      The user's unique GUID
     * @param password  The user's password
     * @return A {@link Completable} object
     */
    public Completable updatePayload(String sharedKey, String guid, String password) {
        return payloadDataManager.initializeAndDecrypt(sharedKey, guid, password);
    }

    /**
     * Returns a {@link WalletOptions} object from the website. This object is used to get the
     * current buy/sell partner info as well as a list of countries where buy/sell is rolled out.
     *
     * @return An {@link Observable} wraping a {@link WalletOptions} object
     */
    public Observable<WalletOptions> getWalletOptions() {
        return rxPinning.call(() -> walletService.getWalletOptions())
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Creates a new HD Wallet and saves it to the API.
     *
     * @param password   The user's chosen password
     * @param walletName The name for the wallet, usually some default that has been localised
     * @param email      The user's email address
     * @return An {@link Observable} wrapping a {@link Wallet} object
     */
    public Observable<Wallet> createHdWallet(String password, String walletName, String email) {
        return payloadDataManager.createHdWallet(password, walletName, email)
                .doOnNext(payload -> {
                    // Successfully created and saved
                    appUtil.setNewlyCreated(true);
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                    appUtil.setSharedKey(payload.getSharedKey());
                });
    }

    /**
     * Restores a HD Wallet from a valid 12 word mnemonic whilst creating a new login on the
     * website. Saves the wallet if successful.
     *
     * @param email    The user's email address
     * @param password The user's chosen password
     * @param mnemonic A valid 12 word mnemonic for an existing Wallet
     * @return An {@link Observable} wrapping a {@link Wallet} object
     */
    public Observable<Wallet> restoreHdWallet(String email, String password, String mnemonic) {
        return payloadDataManager.restoreHdWallet(
                mnemonic,
                stringUtils.getString(R.string.default_wallet_name),
                email,
                password)
                .doOnNext(payload -> {
                    appUtil.setNewlyCreated(true);
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                    appUtil.setSharedKey(payload.getSharedKey());
                });
    }

    /**
     * Polls for the auth status of a user's account every 2 seconds until either the user checks
     * their email and a valid Payload is returned, or the call fails.
     *
     * @param guid      The user's unique GUID
     * @param sessionId The current session ID
     * @return An {@link Observable} wrapping a String which represents the user's Payload OR an
     * auth required response from the API
     */
    public Observable<String> startPollingAuthStatus(String guid, String sessionId) {
        // Emit tick every 2 seconds
        return Observable.interval(2, TimeUnit.SECONDS)
                // For each emission from the timer, try to get the payload
                .map(tick -> getEncryptedPayload(guid, sessionId).blockingFirst())
                // If auth not required, emit payload
                .filter(s -> s.errorBody() == null || !s.errorBody().string().contains(AUTHORIZATION_REQUIRED))
                // Return message in response
                .map(responseBodyResponse -> responseBodyResponse.body().string())
                // If error called, emit Auth Required
                .onErrorReturn(throwable -> AUTHORIZATION_REQUIRED)
                // Only emit the first object
                .firstElement()
                // As Observable rather than Maybe
                .toObservable()
                // Apply correct threading
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Creates a timer which counts down for two minutes and emits the remaining time on each count.
     * This is used to show the user how long they have to check their email before the login
     * request expires.
     *
     * @return An {@link Observable} where the emitted int is the number of seconds left
     */
    public Observable<Integer> createCheckEmailTimer() {
        timer = 2 * 60;

        return Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(aLong -> timer--)
                .takeUntil(aLong -> timer < 0);
    }

    /**
     * Initializes the {@link PayloadManager} using an encrypted Payload response from the server.
     *
     * @return A {@link Completable} object
     */
    public Completable initializeFromPayload(String payload, String password) {
        return payloadDataManager.initializeFromPayload(payload, password)
                .doOnComplete(() -> {
                    prefsUtil.setValue(PrefsUtil.KEY_GUID, payloadDataManager.getWallet().getGuid());
                    appUtil.setSharedKey(payloadDataManager.getWallet().getSharedKey());
                    prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                });
    }

    /**
     * Validates the passed PIN for the user's GUID and shared key and returns a decrypted password.
     *
     * @param passedPin The PIN to be used
     * @return An {@link Observable} where the wrapped String is the user's decrypted password
     */
    public Observable<String> validatePin(String passedPin) {
        return rxPinning.call(() -> getValidatePinObservable(passedPin))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Creates a new PIN for a user
     *
     * @param password The user's password
     * @param pin      The new chosen PIN
     * @return A {@link Completable} object
     */
    public Completable createPin(String password, String pin) {
        return rxPinning.call(() -> getCreatePinObservable(password, pin))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    private Observable<String> getValidatePinObservable(String passedPin) {
        accessState.setPIN(passedPin);
        String key = prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encryptedPassword = prefsUtil.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        return walletService.validateAccess(key, passedPin)
                .map(response -> {
                    if (response.isSuccessful()) {
                        appUtil.setNewlyCreated(false);
                        String decryptionKey = response.body().getSuccess();

                        return aesUtilWrapper.decrypt(encryptedPassword,
                                decryptionKey,
                                AESUtil.PIN_PBKDF2_ITERATIONS);
                    } else {
                        // Server error
                        throw new ServerConnectionException(response.code()+" "+response.message());
                    }
                });
    }

    private Completable getCreatePinObservable(String password, String passedPin) {
        if (passedPin == null || passedPin.equals("0000") || passedPin.length() != 4) {
            return Completable.error(new Throwable("Invalid PIN"));
        }

        accessState.setPIN(passedPin);
        appUtil.applyPRNGFixes();

        return Completable.create(subscriber -> {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            String key = new String(Hex.encode(bytes), "UTF-8");
            random.nextBytes(bytes);
            String value = new String(Hex.encode(bytes), "UTF-8");

            walletService.setAccessKey(key, value, passedPin)
                    .subscribe(response -> {
                        if (response.isSuccessful()) {
                            String encryptionKey = Hex.toHexString(value.getBytes("UTF-8"));

                            String encryptedPassword = aesUtilWrapper.encrypt(
                                    password, encryptionKey, AESUtil.PIN_PBKDF2_ITERATIONS);

                            prefsUtil.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encryptedPassword);
                            prefsUtil.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, key);

                            if (!subscriber.isDisposed()) {
                                subscriber.onComplete();
                            }
                        } else {
                            throw Exceptions.propagate(new Throwable("Validate access failed: "
                                    + response.errorBody().string()));
                        }

                    }, throwable -> {
                        if (!subscriber.isDisposed()) {
                            subscriber.onError(throwable);
                            subscriber.onComplete();
                        }
                    });
        });
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid A user's GUID
     * @return {@link Observable<ResponseBody>} wrapping the pairing encryption password
     */
    public Observable<ResponseBody> getPairingEncryptionPassword(String guid) {
        return rxPinning.call(() -> walletService.getPairingEncryptionPassword(guid))
                .compose(RxUtil.applySchedulersToObservable());
    }
}
