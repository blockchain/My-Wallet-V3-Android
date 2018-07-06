package piuk.blockchain.android.ui.fingerprint

import android.support.annotation.StringRes
import android.support.annotation.VisibleForTesting
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog.Companion.KEY_BUNDLE_PIN_CODE
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog.Companion.KEY_BUNDLE_STAGE
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate")
class FingerprintPresenter @Inject constructor(
    private val fingerprintHelper: FingerprintHelper
) : BasePresenter<FingerprintView>(), FingerprintHelper.AuthCallback {

    @VisibleForTesting
    var currentStage: FingerprintStage? = null
    @VisibleForTesting
    var pincode: String? = null

    override fun onViewReady() {
        val stageString = view.getBundle()?.getString(KEY_BUNDLE_STAGE)
        pincode = view.getBundle()?.getString(KEY_BUNDLE_PIN_CODE)

        if (!stageString.isNullOrEmpty() && !pincode.isNullOrEmpty()) {
            currentStage = FingerprintStage.valueOf(stageString!!)
        } else {
            view.onCanceled()
            return
        }

        when (currentStage) {
            FingerprintStage.REGISTER_FINGERPRINT -> registerFingerprint()
            FingerprintStage.AUTHENTICATE -> authenticateFingerprint()
            else -> throw RuntimeException("Unknown stage passed to Presenter")
        }
    }

    // Fingerprint not recognised
    override fun onFailure() {
        setFailureState(R.string.fingerprint_not_recognized, null)
        view.onRecoverableError()
    }

    // Some error occurred
    override fun onHelp(message: String) {
        setFailureState(null, null)
        view.setStatusText(message)
        view.onRecoverableError()
    }

    override fun onAuthenticated(data: String?) {
        view.setIcon(R.drawable.vector_fingerprint_success)
        view.setStatusTextColor(R.color.primary_blue_accent)
        view.setStatusText(R.string.fingerprint_success)
        view.onAuthenticated(data)

        if (currentStage == FingerprintStage.REGISTER_FINGERPRINT && data != null) {
            fingerprintHelper.storeEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, data)
        }
    }

    /**
     * Recently changed PIN on device or added another fingerprint, must re-register. Note this
     * won't ever be called when registering a fingerprint, see [FingerprintHelper.encryptString]
     */
    override fun onKeyInvalidated() {
        setFailureState(
            R.string.fingerprint_key_invalidated_brief,
            R.string.fingerprint_key_invalidated_description
        )
        view.setCancelButtonText(R.string.fingerprint_use_pin)
        view.onFatalError()

        fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)
        fingerprintHelper.setFingerprintUnlockEnabled(false)
    }

    // Too many attempts - show message appropriate to stage
    override fun onFatalError() {
        when (currentStage) {
            FingerprintStage.REGISTER_FINGERPRINT -> setFailureState(
                R.string.fingerprint_fatal_error_brief,
                R.string.fingerprint_fatal_error_register_description
            )
            else -> setFailureState(
                R.string.fingerprint_fatal_error_brief,
                R.string.fingerprint_fatal_error_authenticate_description
            )
        }
        view.onFatalError()

        fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)
        fingerprintHelper.setFingerprintUnlockEnabled(false)
    }

    private fun setFailureState(@StringRes status: Int?, @StringRes description: Int?) {
        view.setIcon(R.drawable.vector_fingerprint_error)
        view.setStatusTextColor(R.color.product_red_medium)
        if (status != null) view.setStatusText(status)
        if (description != null) view.setDescriptionText(description)
    }

    private fun authenticateFingerprint() {
        view.setCancelButtonText(R.string.fingerprint_use_pin)
        fingerprintHelper.decryptString(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, pincode!!, this)
    }

    private fun registerFingerprint() {
        view.setCancelButtonText(android.R.string.cancel)
        view.setDescriptionText(R.string.fingerprint_prompt)
        fingerprintHelper.encryptString(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, pincode!!, this)
    }

    override fun onViewDestroyed() {
        super.onViewDestroyed()
        fingerprintHelper.releaseFingerprintReader()
    }
}
