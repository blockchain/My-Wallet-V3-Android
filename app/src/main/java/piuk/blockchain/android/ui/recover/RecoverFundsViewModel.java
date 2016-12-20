package piuk.blockchain.android.ui.recover;

import android.content.Intent;
import android.support.annotation.StringRes;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;

public class RecoverFundsViewModel extends BaseViewModel {

    private DataListener mDataListener;
    @Inject protected AuthDataManager mAuthDataManager;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected AppUtil mAppUtil;

    public interface DataListener {

        String getRecoveryPhrase();

        Intent getPageIntent();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog(@StringRes int messageId);

        void dismissProgressDialog();

        void goToPinEntryPage();

    }

    public RecoverFundsViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        mDataListener = listener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    public void onContinueClicked() {
        String recoveryPhrase = mDataListener.getRecoveryPhrase();
        if (recoveryPhrase == null || recoveryPhrase.isEmpty()) {
            mDataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        String trimmed = recoveryPhrase.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        if (words != 12) {
            mDataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        String password = mDataListener.getPageIntent().getStringExtra(KEY_INTENT_PASSWORD);

        if (password == null || password.isEmpty()) {
            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
            mAppUtil.clearCredentialsAndRestart();
            return;
        }

        String email = mDataListener.getPageIntent().getStringExtra(KEY_INTENT_EMAIL);

        if (email == null || email.isEmpty()) {
            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
            mAppUtil.clearCredentialsAndRestart();
            return;
        }

        mDataListener.showProgressDialog(R.string.creating_wallet);

        mAuthDataManager.restoreHdWallet(email, password, recoveryPhrase)
                .doAfterTerminate(() -> mDataListener.dismissProgressDialog())
                .subscribe(payload -> {
                    mDataListener.goToPinEntryPage();
                }, throwable -> {
                    mDataListener.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR);
                });
    }

    public AppUtil getAppUtil() {
        return mAppUtil;
    }

}
