package piuk.blockchain.android.ui.account;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.databinding.ActivityAccountEditBinding;
import piuk.blockchain.android.databinding.AlertGenericWarningBinding;
import piuk.blockchain.android.databinding.AlertShowExtendedPublicKeyBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

public class AccountEditActivity extends BaseAuthActivity implements AccountEditViewModel.DataListener {

    private static final int ADDRESS_LABEL_MAX_LENGTH = 17;
    private static final int SCAN_PRIVX = 302;

    @Thunk AccountEditViewModel viewModel;
    @Thunk AlertDialog transactionSuccessDialog;
    private ActivityAccountEditBinding binding;
    private MaterialProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_edit);
        viewModel = new AccountEditViewModel(new AccountEditModel(this), this);
        binding.setViewModel(viewModel);

        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.edit));
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);

        binding.transferContainer.setOnClickListener(v -> {
            if (viewModel.transferFundsClickable()) {
                new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        viewModel.onClickTransferFunds();
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        viewModel.setSecondPassword(validateSecondPassword);
                        viewModel.onClickTransferFunds();
                    }
                });
            }
        });

        viewModel.onViewReady();
    }

    @Override
    public void promptAccountLabel(@Nullable String label) {
        final AppCompatEditText etLabel = new AppCompatEditText(this);
        etLabel.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});
        if (label != null && label.length() <= ADDRESS_LABEL_MAX_LENGTH) {
            etLabel.setText(label);
            etLabel.setSelection(label.length());
        }

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.name)
                .setMessage(R.string.assign_display_name)
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, etLabel))
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, (dialog, whichButton) -> {
                    if (!ConnectivityStatus.hasConnectivity(this)) {
                        onConnectivityLost();
                    } else {
                        viewModel.updateAccountLabel(etLabel.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onConnectivityLost() {
        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String type) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, type);
    }

    @Override
    public void setActivityResult(int resultCode) {
        setResult(resultCode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void sendBroadcast(String key, String data) {
        Intent intent = new Intent(WebSocketService.ACTION_INTENT);
        intent.putExtra(key, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        viewModel.destroy();
        super.onDestroy();
    }

    @Override
    public void startScanActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, this);
        } else {
            if (!new AppUtil(this).isCameraOpen()) {
                Intent intent = new Intent(this, CaptureActivity.class);
                startActivityForResult(intent, SCAN_PRIVX);
            } else {
                ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        }
    }

    @Override
    public void promptPrivateKey(String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.privx_required)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
                            @Override
                            public void onNoSecondPassword() {
                                startScanActivity();
                            }

                            @Override
                            public void onSecondPasswordValidated(String validateSecondPassword) {
                                viewModel.setSecondPassword(validateSecondPassword);
                                startScanActivity();
                            }
                        }))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void showPaymentDetails(PaymentConfirmationDetails details, PendingTransaction pendingTransaction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        FragmentSendConfirmBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.fragment_send_confirm, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.confirmFromLabel.setText(details.fromLabel);
        dialogBinding.confirmToLabel.setText(details.toLabel);
        dialogBinding.confirmAmountBtcUnit.setText(details.btcUnit);
        dialogBinding.confirmAmountFiatUnit.setText(details.fiatUnit);
        dialogBinding.confirmAmountBtc.setText(details.btcAmount);
        dialogBinding.confirmAmountFiat.setText(details.fiatAmount);
        dialogBinding.confirmFeeBtc.setText(details.btcFee);
        dialogBinding.confirmFeeFiat.setText(details.fiatFee);
        dialogBinding.confirmTotalBtc.setText(details.btcTotal);
        dialogBinding.confirmTotalFiat.setText(details.fiatTotal);

        String feeMessage = "";
        if (details.isSurge) {
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
            feeMessage += getString(R.string.transaction_surge);

        }

        if (details.hasConsumedAmounts) {
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);

            if (details.hasConsumedAmounts) {
                if (details.isSurge) feeMessage += "\n\n";
                feeMessage += getString(R.string.large_tx_high_fee_warning);
            }

        }

        final String finalFeeMessage = feeMessage;
        dialogBinding.ivFeeInfo.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_fee)
                .setMessage(finalFeeMessage)
                .setPositiveButton(android.R.string.ok, null).show());

        if (details.isSurge) {
            dialogBinding.confirmFeeBtc.setTextColor(ContextCompat.getColor(this, R.color.blockchain_send_red));
            dialogBinding.confirmFeeFiat.setTextColor(ContextCompat.getColor(this, R.color.blockchain_send_red));
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
        }

        dialogBinding.tvCustomizeFee.setVisibility(View.GONE);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            viewModel.submitPayment(pendingTransaction);
            alertDialog.dismiss();
        });

        alertDialog.show();

        if (details.isLargeTransaction) {
            onShowLargeTransactionWarning(alertDialog);
        }
    }

    private void onShowLargeTransactionWarning(AlertDialog alertDialog) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertGenericWarningBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_generic_warning, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialogFee = dialogBuilder.create();
        alertDialogFee.setCanceledOnTouchOutside(false);

        dialogBinding.tvBody.setText(R.string.large_tx_warning);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();
        });

        dialogBinding.confirmKeep.setText(getResources().getString(R.string.go_back));
        dialogBinding.confirmKeep.setOnClickListener(v -> {
            alertDialogFee.dismiss();
            alertDialog.dismiss();
        });

        dialogBinding.confirmChange.setText(getResources().getString(R.string.accept_higher_fee));
        dialogBinding.confirmChange.setOnClickListener(v -> alertDialogFee.dismiss());

        alertDialogFee.show();
    }

    @Override
    public void promptArchive(String title, String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    if (!ConnectivityStatus.hasConnectivity(this)) {
                        onConnectivityLost();
                    } else {
                        viewModel.archiveAccount();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void promptBIP38Password(final String data) {
        final AppCompatEditText password = new AppCompatEditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, password))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        viewModel.importBIP38Address(data, password.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void privateKeyImportMismatch() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.private_key_successfully_imported) + "\n\n" + getString(R.string.private_key_not_matching_address))
                .setPositiveButton(R.string.try_again, (dialog, whichButton) -> viewModel.onClickScanXpriv(null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void privateKeyImportSuccess() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.success)
                .setMessage(R.string.private_key_successfully_imported)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    @Override
    public void showXpubSharingWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.xpub_sharing_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, (dialog, whichButton) ->
                        viewModel.showAddressDetails()).setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    @Override
    public void showAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        AlertShowExtendedPublicKeyBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_show_extended_public_key, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.tvWarningHeading.setText(heading);
        dialogBinding.tvXpubNote.setText(note);
        dialogBinding.tvExtendedXpub.setText(copy);
        dialogBinding.tvExtendedXpub.setTextColor(ContextCompat.getColor(this, R.color.blockchain_blue));
        dialogBinding.ivQr.setImageBitmap(bitmap);

        dialogBinding.tvExtendedXpub.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = null;
            clip = android.content.ClipData.newPlainText("Send address", qrString);
            ToastCustom.makeText(this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
            clipboard.setPrimaryClip(clip);
        });

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK) {
            viewModel.handleIncomingScanIntent(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void showTransactionSuccess() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.modal_transaction_success, null);
        dialogBuilder.setView(dialogView)
                .setPositiveButton(getString(R.string.done), (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialogInterface -> finish())
                .create()
                .show();

        dialogView.postDelayed(dialogRunnable, 5 * 1000);
    }

    private final Runnable dialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (transactionSuccessDialog != null && transactionSuccessDialog.isShowing()) {
                transactionSuccessDialog.dismiss();
            }
        }
    };

    @Override
    public void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();

        progress = new MaterialProgressDialog(this);
        progress.setMessage(message);
        progress.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }
}
