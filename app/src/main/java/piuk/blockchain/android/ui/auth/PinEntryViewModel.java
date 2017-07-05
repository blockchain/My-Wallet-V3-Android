package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import info.blockchain.wallet.exceptions.AccountLockedException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.PayloadManager;

import org.spongycastle.crypto.InvalidCipherTextException;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;
import static piuk.blockchain.android.ui.auth.LandingActivity.KEY_INTENT_RECOVERING_FUNDS;
import static piuk.blockchain.android.ui.auth.PinEntryFragment.KEY_VALIDATING_PIN_FOR_RESULT;

@SuppressWarnings("WeakerAccess")
public class PinEntryViewModel extends BaseViewModel {

    private static final int PIN_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 4;

    private DataListener mDataListener;
    @Inject protected AuthDataManager mAuthDataManager;
    @Inject protected AppUtil mAppUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected StringUtils mStringUtils;
    @Inject protected SSLVerifyUtil mSSLVerifyUtil;
    @Inject protected FingerprintHelper mFingerprintHelper;
    @Inject protected AccessState mAccessState;

    private String mEmail;
    private String mPassword;
    @VisibleForTesting boolean mRecoveringFunds = false;
    @VisibleForTesting boolean mCanShowFingerprintDialog = true;
    @VisibleForTesting boolean mValidatingPinForResult = false;
    @VisibleForTesting String mUserEnteredPin = "";
    @VisibleForTesting String mUserEnteredConfirmationPin;
    @VisibleForTesting boolean bAllowExit = true;

    public interface DataListener {

        Intent getPageIntent();

        ImageView[] getPinBoxArray();

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void dismissProgressDialog();

        void showMaxAttemptsDialog();

        void showValidationDialog();

        void showCommonPinWarning(DialogButtonCallback callback);

        void showWalletVersionNotSupportedDialog(String walletVersion);

        void goToUpgradeWalletActivity();

        void restartPageAndClearTop();

        void setTitleString(@StringRes int title);

        void setTitleVisibility(@ViewUtils.Visibility int visibility);

        void clearPinBoxes();

        void goToPasswordRequiredActivity();

        void finishWithResultOk(String pin);

        void showFingerprintDialog(String pincode);

        void showKeyboard();

        void showAccountLockedDialog();

    }

    public PinEntryViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        mDataListener = listener;
    }

    @Override
    public void onViewReady() {
        mSSLVerifyUtil.validateSSL();
        mAppUtil.applyPRNGFixes();

        if (mDataListener.getPageIntent() != null) {
            Bundle extras = mDataListener.getPageIntent().getExtras();
            if (extras != null) {
                if (extras.containsKey(KEY_INTENT_EMAIL)) {
                    mEmail = extras.getString(KEY_INTENT_EMAIL);
                }

                if (extras.containsKey(KEY_INTENT_PASSWORD)) {
                    //noinspection ConstantConditions
                    mPassword = extras.getString(KEY_INTENT_PASSWORD);
                }

                if (extras.containsKey(KEY_INTENT_RECOVERING_FUNDS)) {
                    mRecoveringFunds = extras.getBoolean(KEY_INTENT_RECOVERING_FUNDS);
                }

                if (extras.containsKey(KEY_VALIDATING_PIN_FOR_RESULT)) {
                    mValidatingPinForResult = extras.getBoolean(KEY_VALIDATING_PIN_FOR_RESULT);
                }

                if (mPassword != null && !mPassword.isEmpty() && mEmail != null && !mEmail.isEmpty()) {
                    // Previous page was CreateWalletFragment
                    bAllowExit = false;

                    mPrefsUtil.setValue(PrefsUtil.KEY_EMAIL, mEmail);

                    if (!mRecoveringFunds) {
                        // If funds recovered, wallet already restored, no need to overwrite payload
                        // with another new wallet
                        mDataListener.showProgressDialog(R.string.create_wallet, "...");
                        createWallet();
                    }
                }
            }
        }

        checkPinFails();
        checkFingerprintStatus();
    }

    public void checkFingerprintStatus() {
        if (getIfShouldShowFingerprintLogin()) {
            mDataListener.showFingerprintDialog(
                    mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE));
        } else {
            mDataListener.showKeyboard();
        }
    }

    public boolean canShowFingerprintDialog() {
        return mCanShowFingerprintDialog;
    }

    public boolean getIfShouldShowFingerprintLogin() {
        return !(mValidatingPinForResult || mRecoveringFunds || isCreatingNewPin())
                && mFingerprintHelper.isFingerprintUnlockEnabled()
                && mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE) != null;
    }

    public void loginWithDecryptedPin(String pincode) {
        mCanShowFingerprintDialog = false;
        for (ImageView view : mDataListener.getPinBoxArray()) {
            view.setImageResource(R.drawable.rounded_view_dark_blue);
        }
        validatePIN(pincode);
    }

    public void onDeleteClicked() {
        if (!mUserEnteredPin.isEmpty()) {
            // Remove last char from pin string
            mUserEnteredPin = mUserEnteredPin.substring(0, mUserEnteredPin.length() - 1);

            // Clear last box
            mDataListener.getPinBoxArray()[mUserEnteredPin.length()].setImageResource(R.drawable.rounded_view_blue_white_border);
        }
    }

    public void onPadClicked(String string) {
        if (mUserEnteredPin.length() == PIN_LENGTH) {
            return;
        }

        // Append tapped #
        mUserEnteredPin = mUserEnteredPin + string;
        mDataListener.getPinBoxArray()[mUserEnteredPin.length() - 1].setImageResource(R.drawable.rounded_view_dark_blue);

        // Perform appropriate action if PIN_LENGTH has been reached
        if (mUserEnteredPin.length() == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (mUserEnteredPin.equals("0000")) {
                showErrorToast(R.string.zero_pin);
                clearPinViewAndReset();
                if (isCreatingNewPin()) {
                    mDataListener.setTitleString(R.string.create_pin);
                }
                return;
            }

            // Only show warning on first entry and if user is creating a new PIN
            if (isCreatingNewPin() && isPinCommon(mUserEnteredPin) && mUserEnteredConfirmationPin == null) {
                mDataListener.showCommonPinWarning(new DialogButtonCallback() {
                    @Override
                    public void onPositiveClicked() {
                        clearPinViewAndReset();
                    }

                    @Override
                    public void onNegativeClicked() {
                        validateAndConfirmPin();
                    }
                });

                // If user is changing their PIN and it matches their old one, disallow it
            } else if (isChangingPin()
                    && mUserEnteredConfirmationPin == null
                    && mAccessState.getPIN().equals(mUserEnteredPin)) {
                showErrorToast(R.string.change_pin_new_matches_current);
                clearPinViewAndReset();
            } else {
                validateAndConfirmPin();
            }
        }
    }

    @Thunk
    void validateAndConfirmPin() {
        // Validate
        if (!mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            mDataListener.setTitleVisibility(View.INVISIBLE);
            validatePIN(mUserEnteredPin);
        } else if (mUserEnteredConfirmationPin == null) {
            // End of Create -  Change to Confirm
            mUserEnteredConfirmationPin = mUserEnteredPin;
            mUserEnteredPin = "";
            mDataListener.setTitleString(R.string.confirm_pin);
            clearPinBoxes();
        } else if (mUserEnteredConfirmationPin.equals(mUserEnteredPin)) {
            // End of Confirm - Pin is confirmed
            createNewPin(mUserEnteredPin);
        } else {
            // End of Confirm - Pin Mismatch
            showErrorToast(R.string.pin_mismatch_error);
            mDataListener.setTitleString(R.string.create_pin);
            clearPinViewAndReset();
        }
    }

    /**
     * Resets the view without restarting the page
     */
    @Thunk
    void clearPinViewAndReset() {
        clearPinBoxes();
        mUserEnteredConfirmationPin = null;
        checkFingerprintStatus();
    }

    public void clearPinBoxes() {
        mUserEnteredPin = "";
        mDataListener.clearPinBoxes();
    }

    @VisibleForTesting
    void updatePayload(String password) {
        mDataListener.showProgressDialog(R.string.decrypting_wallet, null);

        compositeDisposable.add(
                mAuthDataManager.updatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doAfterTerminate(() -> {
                            mDataListener.dismissProgressDialog();
                            mCanShowFingerprintDialog = true;
                        })
                        .subscribe(() -> {
                            mAppUtil.setSharedKey(mPayloadManager.getPayload().getSharedKey());

                            setAccountLabelIfNecessary();

                            if (!mPayloadManager.getPayload().isUpgraded()) {
                                mDataListener.goToUpgradeWalletActivity();
                            } else {
                                mAppUtil.restartAppWithVerifiedPin();
                            }

                        }, throwable -> {

                            if (throwable instanceof InvalidCredentialsException) {
                                mDataListener.goToPasswordRequiredActivity();

                            } else if (throwable instanceof ServerConnectionException) {
                                mDataListener.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof UnsupportedVersionException) {
                                mDataListener.showWalletVersionNotSupportedDialog(throwable.getMessage());

                            } else if (throwable instanceof DecryptionException) {
                                mDataListener.goToPasswordRequiredActivity();

                            } else if (throwable instanceof PayloadException) {
                                //This shouldn't happen - Payload retrieved from server couldn't be parsed
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof HDWalletException) {
                                //This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof InvalidCipherTextException) {
                                // Password changed on web, needs re-pairing
                                mDataListener.showToast(R.string.password_changed_explanation, ToastCustom.TYPE_ERROR);
                                mAccessState.setPIN(null);
                                mAppUtil.clearCredentialsAndRestart();

                            } else if (throwable instanceof AccountLockedException) {
                                mDataListener.showAccountLockedDialog();

                            } else {
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.clearCredentialsAndRestart();
                            }

                        }));
    }

    public boolean isForValidatingPinForResult() {
        return mValidatingPinForResult;
    }

    public void validatePassword(String password) {
        mDataListener.showProgressDialog(R.string.validating_password, null);

        compositeDisposable.add(
                mAuthDataManager.updatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doAfterTerminate(() -> mDataListener.dismissProgressDialog())
                        .subscribe(() -> {
                            mDataListener.showToast(R.string.pin_4_strikes_password_accepted, ToastCustom.TYPE_OK);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);
                            mDataListener.restartPageAndClearTop();
                        }, throwable -> {

                            if (throwable instanceof ServerConnectionException) {
                                mDataListener.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);
                            } else if (throwable instanceof PayloadException) {
                                //This shouldn't happen - Payload retrieved from server couldn't be parsed
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof HDWalletException) {
                                //This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof AccountLockedException) {
                                mDataListener.showAccountLockedDialog();

                            } else {
                                showErrorToast(R.string.invalid_password);
                                mDataListener.showValidationDialog();
                            }

                        }));
    }

    private void createNewPin(String pin) {
        mDataListener.showProgressDialog(R.string.creating_pin, null);

        compositeDisposable.add(
                mAuthDataManager.createPin(mPayloadManager.getTempPassword(), pin)
                        .subscribe(() -> {
                            mDataListener.dismissProgressDialog();
                            mFingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
                            mFingerprintHelper.setFingerprintUnlockEnabled(false);
                            mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                            updatePayload(mPayloadManager.getTempPassword());
                        }, throwable -> {
                            showErrorToast(R.string.create_pin_failed);
                            mPrefsUtil.clear();
                            mAppUtil.restartApp();
                        }));
    }

    private void validatePIN(String pin) {
        mDataListener.showProgressDialog(R.string.validating_pin, null);

        mAuthDataManager.validatePin(pin)
                .subscribe(password -> {
                    mDataListener.dismissProgressDialog();
                    if (password != null) {
                        if (mValidatingPinForResult) {
                            mDataListener.finishWithResultOk(pin);
                        } else {
                            updatePayload(password);
                        }
                        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    } else {
                        handleValidateFailure();
                    }
                }, throwable -> {
                    showErrorToast(R.string.unexpected_error);
                    mDataListener.restartPageAndClearTop();
                });
    }

    private void handleValidateFailure() {
        if (mValidatingPinForResult) {
            incrementFailureCount();
        } else {
            incrementFailureCountAndRestart();
        }
    }

    private void incrementFailureCount() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        showErrorToast(R.string.invalid_pin);
        mUserEnteredPin = "";
        for (ImageView textView : mDataListener.getPinBoxArray()) {
            textView.setImageResource(R.drawable.rounded_view_blue_white_border);
        }
        mDataListener.setTitleVisibility(View.VISIBLE);
        mDataListener.setTitleString(R.string.pin_entry);
    }

    public void incrementFailureCountAndRestart() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        showErrorToast(R.string.invalid_pin);
        mDataListener.restartPageAndClearTop();
    }

    // Check user's password if PIN fails >= 4
    private void checkPinFails() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        if (fails >= MAX_ATTEMPTS) {
            showErrorToast(R.string.pin_4_strikes);
            mDataListener.showMaxAttemptsDialog();
        }
    }

    private void setAccountLabelIfNecessary() {
        if (mAppUtil.isNewlyCreated()
                && mPayloadManager.getPayload().getHdWallets() != null
                && (mPayloadManager.getPayload().getHdWallets().get(0).getAccounts().get(0).getLabel() == null
                || mPayloadManager.getPayload().getHdWallets().get(0).getAccounts().get(0).getLabel().isEmpty())) {

            mPayloadManager.getPayload().getHdWallets().get(0).getAccounts().get(0).setLabel(mStringUtils.getString(R.string.default_wallet_name));
        }
    }

    private void createWallet() {
        mAppUtil.applyPRNGFixes();
        compositeDisposable.add(
                mAuthDataManager.createHdWallet(mPassword, mStringUtils.getString(R.string.default_wallet_name), mEmail)
                        .doAfterTerminate(() -> mDataListener.dismissProgressDialog())
                        .subscribe(payload -> {
                            // No-op
                        }, throwable -> showErrorToastAndRestartApp(R.string.hd_error)));
    }

    private boolean isPinCommon(String pin) {
        List<String> commonPins = Arrays.asList("1234", "1111", "1212", "7777", "1004");
        return commonPins.contains(pin);
    }

    public void resetApp() {
        mAppUtil.clearCredentialsAndRestart();
    }

    public boolean allowExit() {
        return bAllowExit;
    }

    public boolean isCreatingNewPin() {
        return mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty();
    }

    private boolean isChangingPin() {
        return isCreatingNewPin()
                && mAccessState.getPIN() != null
                && !mAccessState.getPIN().isEmpty();
    }

    @UiThread
    private void showErrorToast(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
    }

    @UiThread
    private void showErrorToastAndRestartApp(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
        resetApp();
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }
}
