package piuk.blockchain.android.ui.recover;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.EditorInfo;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityRecoverFundsBinding;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class RecoverFundsActivity extends BaseAuthActivity implements RecoverFundsViewModel.DataListener {

    private RecoverFundsViewModel mViewModel;
    private ActivityRecoverFundsBinding mBinding;
    private MaterialProgressDialog mProgressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_recover_funds);
        mViewModel = new RecoverFundsViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.recover_funds));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBinding.buttonContinue.setOnClickListener(view -> mViewModel.onContinueClicked());
        mBinding.fieldPassphrase.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                mViewModel.onContinueClicked();
            }
            return true;
        });

        mViewModel.onViewReady();
    }

    @Override
    public void goToPinEntryPage() {
        // Grabs intent from previous page containing Password and Email and then passes it
        // to the PIN entry page
        Intent intent = new Intent(this, PinEntryActivity.class);
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public String getRecoveryPhrase() {
        return mBinding.fieldPassphrase.getText().toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Take user back to landing page
        mViewModel.getAppUtil().restartApp();
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        mViewModel.destroy();
    }

    @Override
    public void showProgressDialog(@StringRes int messageId) {
        dismissProgressDialog();
        mProgressDialog = new MaterialProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(messageId));

        if (!isFinishing()) mProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

}
