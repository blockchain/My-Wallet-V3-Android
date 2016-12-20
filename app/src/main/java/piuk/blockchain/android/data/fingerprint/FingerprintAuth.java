package piuk.blockchain.android.data.fingerprint;

import android.content.Context;

import com.mtramin.rxfingerprint.data.FingerprintAuthenticationResult;
import com.mtramin.rxfingerprint.data.FingerprintDecryptionResult;
import com.mtramin.rxfingerprint.data.FingerprintEncryptionResult;

import io.reactivex.Observable;

public interface FingerprintAuth {

    boolean isFingerprintAvailable(Context applicationContext);

    boolean isHardwareDetected(Context applicationContext);

    boolean areFingerprintsEnrolled(Context applicationContext);

    Observable<FingerprintAuthenticationResult> authenticate(Context applicationContext);

    Observable<FingerprintEncryptionResult> encrypt(Context applicationContext, String key, String stringToEncrypt);

    Observable<FingerprintDecryptionResult> decrypt(Context applicationContext, String key, String stringToDecrypt);
}
