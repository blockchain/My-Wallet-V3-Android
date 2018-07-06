package piuk.blockchain.android.data.fingerprint

import android.content.Context

import com.mtramin.rxfingerprint.data.FingerprintAuthenticationResult
import com.mtramin.rxfingerprint.data.FingerprintDecryptionResult
import com.mtramin.rxfingerprint.data.FingerprintEncryptionResult

import io.reactivex.Observable

interface FingerprintAuth {

    fun isFingerprintAvailable(applicationContext: Context): Boolean

    fun isHardwareDetected(applicationContext: Context): Boolean

    fun areFingerprintsEnrolled(applicationContext: Context): Boolean

    fun authenticate(applicationContext: Context): Observable<FingerprintAuthenticationResult>

    fun encrypt(
        applicationContext: Context,
        key: String,
        stringToEncrypt: String
    ): Observable<FingerprintEncryptionResult>

    fun decrypt(
        applicationContext: Context,
        key: String,
        stringToDecrypt: String
    ): Observable<FingerprintDecryptionResult>
}
