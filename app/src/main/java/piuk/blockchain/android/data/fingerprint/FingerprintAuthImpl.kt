package piuk.blockchain.android.data.fingerprint

import android.content.Context

import com.mtramin.rxfingerprint.RxFingerprint
import com.mtramin.rxfingerprint.data.FingerprintAuthenticationResult
import com.mtramin.rxfingerprint.data.FingerprintDecryptionResult
import com.mtramin.rxfingerprint.data.FingerprintEncryptionResult

import io.reactivex.Observable

class FingerprintAuthImpl : FingerprintAuth {

    /**
     * Returns true only if there is appropriate hardware available && there are enrolled
     * fingerprints
     */
    override fun isFingerprintAvailable(applicationContext: Context): Boolean {
        return RxFingerprint.isAvailable(applicationContext)
    }

    /**
     * Returns true if the device has the appropriate hardware for fingerprint authentication
     */
    override fun isHardwareDetected(applicationContext: Context): Boolean {
        return RxFingerprint.isHardwareDetected(applicationContext)
    }

    /**
     * Returns if any fingerprints are registered
     */
    override fun areFingerprintsEnrolled(applicationContext: Context): Boolean {
        return RxFingerprint.hasEnrolledFingerprints(applicationContext)
    }

    /**
     * Authenticates a user's fingerprint
     */
    override fun authenticate(applicationContext: Context): Observable<FingerprintAuthenticationResult> {
        return RxFingerprint.authenticate(applicationContext)
    }

    /**
     * Encrypts a String and stores its private key in the Android Keystore using a specific keyword
     */
    override fun encrypt(
        applicationContext: Context,
        key: String,
        stringToEncrypt: String
    ): Observable<FingerprintEncryptionResult> {
        return RxFingerprint.encrypt(applicationContext, key, stringToEncrypt)
    }

    /**
     * Decrypts a supplied String after authentication
     */
    override fun decrypt(
        applicationContext: Context,
        key: String,
        stringToDecrypt: String
    ): Observable<FingerprintDecryptionResult> {
        return RxFingerprint.decrypt(applicationContext, key, stringToDecrypt)
    }
}
