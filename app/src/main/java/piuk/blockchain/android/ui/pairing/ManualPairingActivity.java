package piuk.blockchain.android.ui.pairing;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityManualPairingBinding;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ManualPairingActivity extends BaseAuthActivity implements ManualPairingViewModel.DataListener {

    private MaterialProgressDialog mProgressDialog;
    private ActivityManualPairingBinding mBinding;
    private ManualPairingViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_manual_pairing);
        mViewModel = new ManualPairingViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.manual_pairing));
        setSupportActionBar(toolbar);

        mBinding.commandNext.setOnClickListener(v -> mViewModel.onContinueClicked());

        mViewModel.onViewReady();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void goToPinPage() {
        startActivity(new Intent(this, PinEntryActivity.class));
    }

    @Override
    public void updateWaitingForAuthDialog(int secondsRemaining) {
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining);
        }
    }

    @Override
    public void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable) {
        dismissProgressDialog();
        mProgressDialog = new MaterialProgressDialog(this);
        mProgressDialog.setCancelable(cancellable);
        if (suffix != null) {
            mProgressDialog.setMessage(getString(messageId) + "\n\n" + suffix);
        } else {
            mProgressDialog.setMessage(getString(messageId));
        }
        mProgressDialog.setOnCancelListener(dialogInterface -> mViewModel.onProgressCancelled());

        if (!isFinishing()) mProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void resetPasswordField() {
        if (!isFinishing()) mBinding.walletPass.setText("");
    }

    @Override
    public String getGuid() {
        return mBinding.walletId.getText().toString();
    }

    @Override
    public String getPassword() {
        return mBinding.walletPass.getText().toString();
    }

    @Override
    public void onDestroy() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        mViewModel.destroy();
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user enters PIN
        return mViewModel.getAppUtil().detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }
}
