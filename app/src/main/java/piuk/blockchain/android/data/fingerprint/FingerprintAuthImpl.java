package piuk.blockchain.android.data.fingerprint;

import android.content.Context;

import com.mtramin.rxfingerprint.RxFingerprint;
import com.mtramin.rxfingerprint.data.FingerprintAuthenticationResult;
import com.mtramin.rxfingerprint.data.FingerprintDecryptionResult;
import com.mtramin.rxfingerprint.data.FingerprintEncryptionResult;

import io.reactivex.Observable;

public class FingerprintAuthImpl implements FingerprintAuth {

    /**
     * Returns true only if there is appropriate hardware available && there are enrolled
     * fingerprints
     */
    @Override
    public boolean isFingerprintAvailable(Context applicationContext) {
        return RxFingerprint.isAvailable(applicationContext);
    }

    /**
     * Returns true if the device has the appropriate hardware for fingerprint authentication
     */
    @Override
    public boolean isHardwareDetected(Context applicationContext) {
        return RxFingerprint.isHardwareDetected(applicationContext);
    }

    /**
     * Returns if any fingerprints are registered
     */
    @Override
    public boolean areFingerprintsEnrolled(Context applicationContext) {
        return RxFingerprint.hasEnrolledFingerprints(applicationContext);
    }

    /**
     * Authenticates a user's fingerprint
     */
    @Override
    public Observable<FingerprintAuthenticationResult> authenticate(Context applicationContext) {
        return RxFingerprint.authenticate(applicationContext);
    }

    /**
     * Encrypts a String and stores its private key in the Android Keystore using a specific keyword
     */
    @Override
    public Observable<FingerprintEncryptionResult> encrypt(Context applicationContext, String key, String stringToEncrypt) {
        return RxFingerprint.encrypt(applicationContext, key, stringToEncrypt);
    }

    /**
     * Decrypts a supplied String after authentication
     */
    @Override
    public Observable<FingerprintDecryptionResult> decrypt(Context applicationContext, String key, String stringToDecrypt) {
        return RxFingerprint.decrypt(applicationContext, key, stringToDecrypt);
    }


}
