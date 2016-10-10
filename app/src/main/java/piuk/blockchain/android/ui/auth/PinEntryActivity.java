package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import info.blockchain.wallet.util.CharSequenceX;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.databinding.ActivityPinEntryBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.ViewUtils;

public class PinEntryActivity extends BaseAuthActivity implements PinEntryViewModel.DataListener {

    public static final String KEY_VALIDATING_PIN_FOR_RESULT = "validating_pin";
    public static final String KEY_VALIDATED_PIN = "validated_pin";
    public static final int REQUEST_CODE_VALIDATE_PIN = 88;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;
    private static final int PIN_LENGTH = 4;
    private static final Handler mDelayHandler = new Handler();

    private TextView[] mPinBoxArray = null;
    private MaterialProgressDialog mProgressDialog = null;
    private ActivityPinEntryBinding mBinding;
    private PinEntryViewModel mViewModel;

    private long mBackPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_pin_entry);
        mViewModel = new PinEntryViewModel(this);

        // Set title state
        if (mViewModel.isCreatingNewPin()) {
            mBinding.titleBox.setText(R.string.create_pin);
        } else {
            mBinding.titleBox.setText(R.string.pin_entry);
        }

        mPinBoxArray = new TextView[PIN_LENGTH];
        mPinBoxArray[0] = mBinding.pinBox0;
        mPinBoxArray[1] = mBinding.pinBox1;
        mPinBoxArray[2] = mBinding.pinBox2;
        mPinBoxArray[3] = mBinding.pinBox3;

        showConnectionDialogIfNeeded();

        mViewModel.onViewReady();
    }

    private void showConnectionDialogIfNeeded() {
        if (!ConnectivityStatus.hasConnectivity(this)) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setMessage(getString(R.string.check_connectivity_exit))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue, (dialog, id) -> restartPageAndClearTop())
                    .create()
                    .show();
        }
    }

    @Override
    public void showMaxAttemptsDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.password_or_wipe)
                .setCancelable(false)
                .setPositiveButton(R.string.use_password, (dialog, whichButton) -> showValidationDialog())
                .setNegativeButton(R.string.wipe_wallet, (dialog, whichButton) -> mViewModel.resetApp())
                .show();
    }

    @Override
    public void onBackPressed() {
        if (mViewModel.isForValidatingPinForResult()) {
            finishWithResultCanceled();

        } else if (mViewModel.allowExit()) {
            if (mBackPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
                AccessState.getInstance().logout(this);
                return;
            } else {
                ToastCustom.makeText(this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
            }

            mBackPressed = System.currentTimeMillis();
        }
    }

    @Override
    public void showWalletVersionNotSupportedDialog(String walletVersion) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(String.format(getString(R.string.unsupported_encryption_version), walletVersion))
                .setCancelable(false)
                .setPositiveButton(R.string.exit, (dialog, whichButton) -> AccessState.getInstance().logout(this))
                .setNegativeButton(R.string.logout, (dialog, which) -> {
                    mViewModel.getAppUtil().clearCredentialsAndRestart();
                    mViewModel.getAppUtil().restartApp();
                })
                .show();
    }

    @Override
    public void clearPinBoxes() {
        mDelayHandler.postDelayed(new ClearPinNumberRunnable(), 200);
    }

    @Override
    public void goToPasswordRequiredActivity() {
        Intent intent = new Intent(this, PasswordRequiredActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class ClearPinNumberRunnable implements Runnable {
        @Override
        public void run() {
            for (TextView pinBox : getPinBoxArray()) {
                // Reset PIN buttons to blank
                pinBox.setBackgroundResource(R.drawable.rounded_view_blue_white_border);
            }
        }
    }

    @Override
    public void goToUpgradeWalletActivity() {
        Intent intent = new Intent(this, UpgradeWalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @SuppressWarnings("unused") // DataBindingMethod
    public void padClicked(View view) {
        mViewModel.padClicked(view);
    }

    @Override
    public void setTitleString(@StringRes int title) {
        mDelayHandler.postDelayed(() -> mBinding.titleBox.setText(title), 200);
    }

    @Override
    public void setTitleVisibility(@ViewUtils.Visibility int visibility) {
        mBinding.titleBox.setVisibility(visibility);
    }

    @SuppressWarnings("unused") // DataBindingMethod
    public void deleteClicked(View view) {
        mViewModel.onDeleteClicked();
    }

    @Override
    public TextView[] getPinBoxArray() {
        return mPinBoxArray;
    }

    @Override
    public void restartPageAndClearTop() {
        Intent intent = new Intent(this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void showCommonPinWarning(DialogButtonCallback callback) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.common_pin_dialog_title)
                .setMessage(R.string.common_pin_dialog_message)
                .setPositiveButton(R.string.common_pin_dialog_try_again, (dialogInterface, i) -> callback.onPositiveClicked())
                .setNegativeButton(R.string.common_pin_dialog_continue, (dialogInterface, i) -> callback.onNegativeClicked())
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void showValidationDialog() {
        final AppCompatEditText password = new AppCompatEditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(this.getString(R.string.password_entry))
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, password))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> mViewModel.getAppUtil().restartApp())
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    final String pw = password.getText().toString();

                    if (pw.length() > 0) {
                        mViewModel.validatePassword(new CharSequenceX(pw));
                    } else {
                        mViewModel.incrementFailureCountAndRestart();
                    }

                }).show();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showProgressDialog(@StringRes int messageId, @Nullable String suffix) {
        dismissProgressDialog();
        mProgressDialog = new MaterialProgressDialog(this);
        mProgressDialog.setCancelable(false);
        if (suffix != null) {
            mProgressDialog.setMessage(getString(messageId) + suffix);
        } else {
            mProgressDialog.setMessage(getString(messageId));
        }

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
    protected void onResume() {
        super.onResume();
        mViewModel.clearPinBoxes();
    }

    @Override
    public void finishWithResultOk(String pin) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_VALIDATED_PIN, pin);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishWithResultCanceled() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user enters PIN
        // consume event
        return mViewModel.getAppUtil().detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    protected void onDestroy() {
        mViewModel.destroy();
        super.onDestroy();
    }
}