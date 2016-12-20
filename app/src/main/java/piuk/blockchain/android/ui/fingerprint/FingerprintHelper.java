package piuk.blockchain.android.ui.fingerprint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.mtramin.rxfingerprint.RxFingerprint;

import info.blockchain.wallet.util.CharSequenceX;

import java.io.UnsupportedEncodingException;

import io.reactivex.disposables.CompositeDisposable;
import piuk.blockchain.android.data.fingerprint.FingerprintAuth;
import piuk.blockchain.android.util.PrefsUtil;

// This will likely be used in different packages soon
@SuppressWarnings("WeakerAccess")
public class FingerprintHelper {

    private Context applicationContext;
    private PrefsUtil prefsUtil;
    private FingerprintAuth fingerprintAuth;
    @VisibleForTesting CompositeDisposable compositeDisposable;

    public FingerprintHelper(Context applicationContext,
                             PrefsUtil prefsUtil,
                             FingerprintAuth fingerprintAuth) {

        this.applicationContext = applicationContext;
        this.prefsUtil = prefsUtil;
        this.fingerprintAuth = fingerprintAuth;
        compositeDisposable = new CompositeDisposable();
    }

    /**
     * Returns true only if there is appropriate hardware available && there are enrolled
     * fingerprints
     */
    public boolean isFingerprintAvailable() {
        return fingerprintAuth.isFingerprintAvailable(applicationContext);
    }

    /**
     * Returns true if the device has the appropriate hardware for fingerprint authentication
     */
    public boolean isHardwareDetected() {
        return fingerprintAuth.isHardwareDetected(applicationContext);
    }

    /**
     * Returns if any fingerprints are registered
     */
    public boolean areFingerprintsEnrolled() {
        return fingerprintAuth.areFingerprintsEnrolled(applicationContext);
    }

    /**
     * Returns true if the user has previously enabled fingerprint login
     */
    public boolean getIfFingerprintUnlockEnabled() {
        return isFingerprintAvailable() && prefsUtil.getValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, false);
    }

    /**
     * Store whether or not fingerprint login has been successfully set up
     */
    public void setFingerprintUnlockEnabled(boolean enabled) {
        prefsUtil.setValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, enabled);
    }

    /**
     * Allows you to store the encrypted result of fingerprint authentication. The data is converted
     * into a Base64 string and written to shared prefs with a key. Please note that this doesn't
     * encrypt the data in any way, just obfuscates it.
     *
     * @param key  The key to write/retrieve the data to/from
     * @param data The data to be stored, in the form of a {@link CharSequenceX}
     * @return Returns true if data stored successfully
     */
    public boolean storeEncryptedData(@NonNull String key, @NonNull CharSequenceX data) {
        try {
            String base64 = Base64.encodeToString(data.toString().getBytes("UTF-8"), Base64.DEFAULT);
            prefsUtil.setValue(key, base64);
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    /**
     * Retrieve previously saved encrypted data from shared preferences
     *
     * @param key The key of the item to be retrieved
     * @return A {@link CharSequenceX} wrapping the saved String, or null if not found
     */
    @Nullable
    public CharSequenceX getEncryptedData(@NonNull String key) {
        String encryptedData = prefsUtil.getValue(key, "");
        if (!encryptedData.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(encryptedData.getBytes("UTF-8"), Base64.DEFAULT);
                return new CharSequenceX(new String(bytes, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Deletes the data stored under the passed in key
     *
     * @param key The key of the data to be stored
     */
    public void clearEncryptedData(@NonNull String key) {
        prefsUtil.removeValue(key);
    }

    /**
     * Authenticates a user's fingerprint
     *
     * @param callback {@link AuthCallback}
     */
    public void authenticateFingerprint(AuthCallback callback) {
        compositeDisposable.add(
                fingerprintAuth.authenticate(applicationContext)
                        .subscribe(fingerprintAuthenticationResult -> {
                            switch (fingerprintAuthenticationResult.getResult()) {
                                case FAILED:
                                    callback.onFailure();
                                    break;
                                case HELP:
                                    callback.onHelp(fingerprintAuthenticationResult.getMessage());
                                    break;
                                case AUTHENTICATED:
                                    callback.onAuthenticated(null);
                                    break;
                            }
                        }, throwable -> {
                            callback.onFatalError();
                        }));
    }

    /**
     * Encrypts a String and stores its private key in the Android Keystore using a specific keyword
     *
     * @param key             The key to save/retrieve the object
     * @param stringToEncrypt The String to be encrypted
     * @param callback        {@link AuthCallback}
     */
    public void encryptString(String key, String stringToEncrypt, AuthCallback callback) {
        compositeDisposable.add(
                fingerprintAuth.encrypt(applicationContext, key, stringToEncrypt)
                        .subscribe(encryptionResult -> {
                            switch (encryptionResult.getResult()) {
                                case FAILED:
                                    callback.onFailure();
                                    break;
                                case HELP:
                                    callback.onHelp(encryptionResult.getMessage());
                                    break;
                                case AUTHENTICATED:
                                    String encrypted = encryptionResult.getEncrypted();
                                    callback.onAuthenticated(new CharSequenceX(encrypted));
                                    break;
                            }
                        }, throwable -> {
                            callback.onFatalError();
                        }));
    }

    /**
     * Decrypts a supplied String after authentication
     *
     * @param key             The key of the object to be retrieved
     * @param encryptedString The String to be decrypted
     * @param callback        {@link AuthCallback}
     */
    public void decryptString(String key, String encryptedString, AuthCallback callback) {
        compositeDisposable.add(
                fingerprintAuth.decrypt(applicationContext, key, encryptedString)
                        .subscribe(decryptionResult -> {
                            switch (decryptionResult.getResult()) {
                                case FAILED:
                                    callback.onFailure();
                                    break;
                                case HELP:
                                    callback.onHelp(decryptionResult.getMessage());
                                    break;
                                case AUTHENTICATED:
                                    String decrypted = decryptionResult.getDecrypted();
                                    callback.onAuthenticated(new CharSequenceX(decrypted));
                                    break;
                            }
                        }, throwable -> {
                            if (RxFingerprint.keyInvalidated(throwable)) {
                                // The keys you wanted to use are invalidated because the user has turned off his
                                // secure lock screen or changed the fingerprints stored on the device
                                // You have to re-encrypt the data to access it
                                callback.onKeyInvalidated();
                            } else {
                                callback.onFatalError();
                            }
                        }));
    }

    /**
     * This should be called when authentication completed or no longer required, otherwise the
     * fingerprint sensor will keep listening in the background for touch events and leak memory.
     */
    void releaseFingerprintReader() {
        compositeDisposable.clear();
    }

    public interface AuthCallback {

        void onFailure();

        void onHelp(String message);

        void onAuthenticated(@Nullable CharSequenceX data);

        void onKeyInvalidated();

        void onFatalError();
    }
}
