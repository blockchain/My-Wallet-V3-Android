package piuk.blockchain.android.ui.fingerprint

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.Base64
import com.mtramin.rxfingerprint.RxFingerprint
import com.mtramin.rxfingerprint.data.FingerprintResult
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.data.fingerprint.FingerprintAuth
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.io.UnsupportedEncodingException
import javax.inject.Inject

@PresenterScope
class FingerprintHelper @Inject constructor(
    private val applicationContext: Context,
    private val prefsUtil: PrefsUtil,
    private val fingerprintAuth: FingerprintAuth
) {

    @VisibleForTesting
    var compositeDisposable: CompositeDisposable = CompositeDisposable()

    /**
     * Returns true only if there is appropriate hardware available && there are enrolled
     * fingerprints
     */
    fun isFingerprintAvailable(): Boolean =
        fingerprintAuth.isFingerprintAvailable(applicationContext)

    /**
     * Returns true if the device has the appropriate hardware for fingerprint authentication
     */
    fun isHardwareDetected(): Boolean = fingerprintAuth.isHardwareDetected(applicationContext)

    /**
     * Returns if any fingerprints are registered
     */
    fun areFingerprintsEnrolled(): Boolean =
        fingerprintAuth.areFingerprintsEnrolled(applicationContext)

    /**
     * Returns true if the user has previously enabled fingerprint login
     */
    fun isFingerprintUnlockEnabled(): Boolean =
        isFingerprintAvailable() && prefsUtil.getValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, false)

    /**
     * Store whether or not fingerprint login has been successfully set up
     */
    fun setFingerprintUnlockEnabled(enabled: Boolean) {
        prefsUtil.setValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, enabled)
    }

    /**
     * Allows you to store the encrypted result of fingerprint authentication. The data is converted
     * into a Base64 string and written to shared prefs with a key. Please note that this doesn't
     * encrypt the data in any way, just obfuscates it.

     * @param key The key to write/retrieve the data to/from
     * *
     * @param data The data to be stored
     * *
     * @return Returns true if data stored successfully
     */
    fun storeEncryptedData(key: String, data: String) = try {
        val base64 = Base64.encodeToString(data.toByteArray(charset("UTF-8")), Base64.DEFAULT)
        prefsUtil.setValue(key, base64)
        true
    } catch (e: UnsupportedEncodingException) {
        false
    }

    /**
     * Retrieve previously saved encrypted data from shared preferences

     * @param key The key of the item to be retrieved
     * *
     * @return A [String] wrapping the saved String, or null if not found
     */
    fun getEncryptedData(key: String): String? {
        val encryptedData = prefsUtil.getValue(key, "")
        if (!encryptedData.isEmpty()) {
            return try {
                String(Base64.decode(encryptedData.toByteArray(charset("UTF-8")), Base64.DEFAULT))
            } catch (e: UnsupportedEncodingException) {
                null
            }
        }

        return null
    }

    /**
     * Deletes the data stored under the passed in key

     * @param key The key of the data to be stored
     */
    fun clearEncryptedData(key: String) {
        prefsUtil.removeValue(key)
    }

    /**
     * Authenticates a user's fingerprint

     * @param callback [AuthCallback]
     */
    fun authenticateFingerprint(callback: AuthCallback) {
        compositeDisposable.add(
            fingerprintAuth.authenticate(applicationContext)
                .subscribe(
                    {
                        when (it.result) {
                            FingerprintResult.FAILED -> callback.onFailure()
                            FingerprintResult.HELP -> callback.onHelp(it.message ?: "")
                            FingerprintResult.AUTHENTICATED -> callback.onAuthenticated(
                                null
                            )
                            else -> throw RuntimeException("$it.result was null")
                        }
                    }, { _ -> callback.onFatalError() })
        )
    }

    /**
     * Encrypts a String and stores its private key in the Android Keystore using a specific keyword

     * @param key The key to save/retrieve the object
     * *
     * @param stringToEncrypt The String to be encrypted
     * *
     * @param callback [AuthCallback]
     */
    fun encryptString(key: String, stringToEncrypt: String, callback: AuthCallback) {
        compositeDisposable.add(
            fingerprintAuth.encrypt(applicationContext, key, stringToEncrypt)
                .subscribe(
                    {
                        when (it.result) {
                            FingerprintResult.FAILED -> callback.onFailure()
                            FingerprintResult.HELP -> callback.onHelp(it.message ?: "")
                            FingerprintResult.AUTHENTICATED -> callback.onAuthenticated(
                                it.encrypted
                            )
                            else -> throw RuntimeException("$it.result was null")
                        }
                    }, { _ -> callback.onFatalError() })
        )
    }

    /**
     * Decrypts a supplied String after authentication

     * @param key The key of the object to be retrieved
     * *
     * @param encryptedString The String to be decrypted
     * *
     * @param callback [AuthCallback]
     */
    fun decryptString(key: String, encryptedString: String, callback: AuthCallback) {
        compositeDisposable.add(
            fingerprintAuth.decrypt(applicationContext, key, encryptedString)
                .subscribe(
                    {
                        when (it.result) {
                            FingerprintResult.FAILED -> callback.onFailure()
                            FingerprintResult.HELP -> callback.onHelp(it.message ?: "")
                            FingerprintResult.AUTHENTICATED -> callback.onAuthenticated(
                                it.decrypted
                            )
                            else -> throw RuntimeException("$it.result was null")
                        }
                    }, { throwable ->
                        if (RxFingerprint.keyInvalidated(throwable)) {
                            // The keys you wanted to use are invalidated because the user has turned off his
                            // secure lock screen or changed the fingerprints stored on the device
                            // You have to re-encrypt the data to access it
                            callback.onKeyInvalidated()
                        } else {
                            callback.onFatalError()
                        }
                    })
        )
    }

    /**
     * This should be called when authentication completed or no longer required, otherwise the
     * fingerprint sensor will keep listening in the background for touch events and leak memory.
     */
    fun releaseFingerprintReader() {
        compositeDisposable.clear()
    }

    interface AuthCallback {

        fun onFailure()

        fun onHelp(message: String)

        fun onAuthenticated(data: String?)

        fun onKeyInvalidated()

        fun onFatalError()
    }
}
