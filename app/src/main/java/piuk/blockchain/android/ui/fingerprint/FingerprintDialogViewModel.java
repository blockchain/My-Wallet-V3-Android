package piuk.blockchain.android.ui.fingerprint;

import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.util.CharSequenceX;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.fingerprint.FingerprintDialog.KEY_BUNDLE_PIN_CODE;
import static piuk.blockchain.android.ui.fingerprint.FingerprintDialog.KEY_BUNDLE_STAGE;

@SuppressWarnings("WeakerAccess")
public class FingerprintDialogViewModel extends BaseViewModel implements FingerprintHelper.AuthCallback {

    private DataListener dataListener;
    @VisibleForTesting String currentStage;
    @VisibleForTesting String pincode;
    @Inject FingerprintHelper fingerprintHelper;

    interface DataListener {

        Bundle getBundle();

        void setCancelButtonText(@StringRes int text);

        void setDescriptionText(@StringRes int text);

        void setStatusText(@StringRes int text);

        void setStatusText(String text);

        void setStatusTextColor(@ColorRes int color);

        void setIcon(@DrawableRes int drawable);

        void onFatalError();

        void onAuthenticated(@Nullable CharSequenceX data);

        void onRecoverableError();

        void onCanceled();

    }

    FingerprintDialogViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        currentStage = dataListener.getBundle().getString(KEY_BUNDLE_STAGE);
        pincode = dataListener.getBundle().getString(KEY_BUNDLE_PIN_CODE);

        if (currentStage == null || pincode == null) {
            dataListener.onCanceled();
            return;
        }

        if (currentStage.equals(FingerprintDialog.Stage.REGISTER_FINGERPRINT)) {
            // Enable fingerprint login
            dataListener.setCancelButtonText(android.R.string.cancel);
            dataListener.setDescriptionText(R.string.fingerprint_prompt);

            fingerprintHelper.encryptString(
                    PrefsUtil.KEY_ENCRYPTED_PIN_CODE,
                    pincode,
                    this);

        } else if (currentStage.equals(FingerprintDialog.Stage.AUTHENTICATE)) {
            // Authenticate previously enabled fingerprint
            dataListener.setCancelButtonText(R.string.fingerprint_use_pin);

            fingerprintHelper.decryptString(
                    PrefsUtil.KEY_ENCRYPTED_PIN_CODE,
                    pincode,
                    this);
        }
    }

    // Fingerprint not recognised
    @Override
    public void onFailure() {
        setFailureState(R.string.fingerprint_not_recognized, null);
        dataListener.onRecoverableError();
    }

    // Some error occurred
    @Override
    public void onHelp(String message) {
        setFailureState(null, null);
        dataListener.setStatusText(message);
        dataListener.onRecoverableError();
    }

    @Override
    public void onAuthenticated(@Nullable CharSequenceX data) {
        dataListener.setIcon(R.drawable.ic_fingerprint_success);
        dataListener.setStatusTextColor(R.color.blockchain_blue);
        dataListener.setStatusText(R.string.fingerprint_success);
        dataListener.onAuthenticated(data);

        if (currentStage.equals(FingerprintDialog.Stage.REGISTER_FINGERPRINT) && data != null) {
            fingerprintHelper.storeEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, data);
        }
    }

    /**
     * Recently changed PIN on device or added another fingerprint, must re-register. Note this
     * won't ever be called when registering a fingerprint, see {@link
     * FingerprintHelper#encryptString(String, String, FingerprintHelper.AuthCallback)}
     */
    @Override
    public void onKeyInvalidated() {
        setFailureState(R.string.fingerprint_key_invalidated_brief, R.string.fingerprint_key_invalidated_description);
        dataListener.setCancelButtonText(R.string.fingerprint_use_pin);
        dataListener.onFatalError();

        fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        fingerprintHelper.setFingerprintUnlockEnabled(false);
    }

    // Too many attempts - show message appropriate to stage
    @Override
    public void onFatalError() {
        if (currentStage.equals(FingerprintDialog.Stage.REGISTER_FINGERPRINT)) {
            setFailureState(R.string.fingerprint_fatal_error_brief, R.string.fingerprint_fatal_error_register_description);
        } else {
            setFailureState(R.string.fingerprint_fatal_error_brief, R.string.fingerprint_fatal_error_authenticate_description);
        }
        dataListener.onFatalError();

        fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        fingerprintHelper.setFingerprintUnlockEnabled(false);
    }

    private void setFailureState(@StringRes Integer status, @StringRes Integer description) {
        dataListener.setIcon(R.drawable.ic_fingerprint_error);
        dataListener.setStatusTextColor(R.color.warning_color);
        if (status != null) dataListener.setStatusText(status);
        if (description != null) dataListener.setDescriptionText(description);
    }

    @Override
    public void destroy() {
        super.destroy();
        fingerprintHelper.releaseFingerprintReader();
    }
}
