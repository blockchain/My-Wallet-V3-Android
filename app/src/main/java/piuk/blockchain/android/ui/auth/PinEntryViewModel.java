package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.TextView;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;
import rx.exceptions.Exceptions;

import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;
import static piuk.blockchain.android.ui.auth.LandingActivity.KEY_INTENT_RECOVERING_FUNDS;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.KEY_VALIDATING_PIN_FOR_RESULT;

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

    private String mEmail;
    private CharSequenceX mPassword;
    private boolean mRecoveringFunds = false;
    @VisibleForTesting boolean mValidatingPinForResult = false;
    @VisibleForTesting String mUserEnteredPin = "";
    @VisibleForTesting String mUserEnteredConfirmationPin;
    @VisibleForTesting boolean bAllowExit = true;

    public interface DataListener {

        Intent getPageIntent();

        TextView[] getPinBoxArray();

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

    }

    public PinEntryViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
    }

    @Override
    public void onViewReady() {
        mSSLVerifyUtil.validateSSL();

        if (mDataListener.getPageIntent() != null) {
            Bundle extras = mDataListener.getPageIntent().getExtras();
            if (extras != null) {
                if (extras.containsKey(KEY_INTENT_EMAIL)) {
                    mEmail = extras.getString(KEY_INTENT_EMAIL);
                }

                if (extras.containsKey(KEY_INTENT_PASSWORD)) {
                    //noinspection ConstantConditions
                    mPassword = new CharSequenceX(extras.getString(KEY_INTENT_PASSWORD));
                }

                if (extras.containsKey(KEY_INTENT_RECOVERING_FUNDS)) {
                    mRecoveringFunds = extras.getBoolean(KEY_INTENT_RECOVERING_FUNDS);
                }

                if (extras.containsKey(KEY_VALIDATING_PIN_FOR_RESULT)) {
                    mValidatingPinForResult = extras.getBoolean(KEY_VALIDATING_PIN_FOR_RESULT);
                }

                if (mPassword != null && mPassword.length() > 0 && mEmail != null && !mEmail.isEmpty()) {
                    // Previous page was CreateWalletFragment
                    bAllowExit = false;
                    saveLoginAndPassword();
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
    }

    public void onDeleteClicked() {
        if (mUserEnteredPin.length() > 0) {
            // Remove last char from pin string
            mUserEnteredPin = mUserEnteredPin.substring(0, mUserEnteredPin.length() - 1);

            // Clear last box
            mDataListener.getPinBoxArray()[mUserEnteredPin.length()].setBackgroundResource(R.drawable.rounded_view_blue_white_border);
        }
    }

    public void padClicked(View view) {
        if (mUserEnteredPin.length() == PIN_LENGTH) {
            return;
        }

        // Append tapped #
        mUserEnteredPin = mUserEnteredPin + view.getTag().toString().substring(0, 1);
        mDataListener.getPinBoxArray()[mUserEnteredPin.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

        // Perform appropriate action if PIN_LENGTH has been reached
        if (mUserEnteredPin.length() == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (mUserEnteredPin.equals("0000")) {
                showErrorToast(R.string.zero_pin);
                clearPinViewAndReset();
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

    @Thunk
    void clearPinViewAndReset() {
        clearPinBoxes();
        mUserEnteredConfirmationPin = null;
    }

    public void clearPinBoxes() {
        mUserEnteredPin = "";
        mDataListener.clearPinBoxes();
    }

    @VisibleForTesting
    void updatePayload(CharSequenceX password) {
        mDataListener.showProgressDialog(R.string.decrypting_wallet, null);

        mCompositeSubscription.add(
                mAuthDataManager.updatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doOnTerminate(() -> mDataListener.dismissProgressDialog())
                        .subscribe(aVoid -> {
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
                            }

                        }));
    }

    public boolean isForValidatingPinForResult() {
        return mValidatingPinForResult;
    }

    public void validatePassword(CharSequenceX password) {
        mDataListener.showProgressDialog(R.string.validating_password, null);

        mCompositeSubscription.add(
                mAuthDataManager.updatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doOnSubscribe(() -> mPayloadManager.setTempPassword(new CharSequenceX("")))
                        .subscribe(o -> {
                            mDataListener.showToast(R.string.pin_4_strikes_password_accepted, ToastCustom.TYPE_OK);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);
                            mDataListener.restartPageAndClearTop();
                            mDataListener.dismissProgressDialog();
                        }, throwable -> {

                            if (throwable instanceof ServerConnectionException) {
                                mDataListener.dismissProgressDialog();
                                mDataListener.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);

                            } else {
                                showErrorToast(R.string.invalid_password);
                                mDataListener.showValidationDialog();
                            }

                        }));
    }

    private void createNewPin(String pin) {
        mDataListener.showProgressDialog(R.string.creating_pin, null);

        mCompositeSubscription.add(
                mAuthDataManager.createPin(mPayloadManager.getTempPassword(), pin)
                        .subscribe(createSuccessful -> {
                            mDataListener.dismissProgressDialog();
                            if (createSuccessful) {
                                mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                                updatePayload(mPayloadManager.getTempPassword());
                            } else {
                                throw Exceptions.propagate(new Throwable("Pin create failed"));
                            }
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
                        if (mValidatingPinForResult) {
                            incrementFailureCount();
                        } else {
                            incrementFailureCountAndRestart();
                        }
                    }
                }, throwable -> {
                    showErrorToast(R.string.unexpected_error);
                    mDataListener.restartPageAndClearTop();
                });
    }

    private void incrementFailureCount() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        showErrorToast(R.string.invalid_pin);
        mUserEnteredPin = "";
        for (TextView textView : mDataListener.getPinBoxArray()) {
            textView.setBackgroundResource(R.drawable.rounded_view_blue_white_border);
        }
        mDataListener.setTitleVisibility(View.VISIBLE);
        mDataListener.setTitleString(R.string.confirm_pin);
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

    private void saveLoginAndPassword() {
        mPrefsUtil.setValue(PrefsUtil.KEY_EMAIL, mEmail);
        mPayloadManager.setEmail(mEmail);
        mPayloadManager.setTempPassword(mPassword);
    }

    private void setAccountLabelIfNecessary() {
        if (mAppUtil.isNewlyCreated()
                && mPayloadManager.getPayload().getHdWallet() != null
                && (mPayloadManager.getPayload().getHdWallet().getAccounts().get(0).getLabel() == null
                || mPayloadManager.getPayload().getHdWallet().getAccounts().get(0).getLabel().isEmpty())) {

            mPayloadManager.getPayload().getHdWallet().getAccounts().get(0).setLabel(mStringUtils.getString(R.string.default_wallet_name));
        }
    }

    private void createWallet() {
        mCompositeSubscription.add(
                mAuthDataManager.createHdWallet(mPassword.toString(), mStringUtils.getString(R.string.default_wallet_name))
                        .doAfterTerminate(() -> mDataListener.dismissProgressDialog())
                        .subscribe(payload -> {
                            if (payload == null) {
                                showErrorToast(R.string.remote_save_ko);
                            }
                        }, throwable -> showErrorToastAndRestartApp(R.string.hd_error)));
    }

    private boolean isPinCommon(String pin) {
        List<String> commonPins = new ArrayList<String>() {{
            add("1234");
            add("1111");
            add("1212");
            add("7777");
            add("1004");
        }};
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
