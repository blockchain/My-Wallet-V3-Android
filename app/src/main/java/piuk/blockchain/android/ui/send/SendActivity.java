package piuk.blockchain.android.ui.send;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.databinding.ActivitySendBinding;
import piuk.blockchain.android.databinding.AlertGenericWarningBinding;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.CustomKeypad;
import piuk.blockchain.android.ui.customviews.CustomKeypadCallback;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AppRate;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_SELECTED_ACCOUNT_POSITION;

public class SendActivity extends BaseAuthActivity implements SendViewModel.DataListener, CustomKeypadCallback {

    private final int SCAN_URI = 2007;
    private final int SCAN_PRIVX = 2008;

    @Thunk ActivitySendBinding binding;
    @Thunk SendViewModel viewModel;
    @Thunk AlertDialog transactionSuccessDialog;

    private CustomKeypad customKeypad;

    private TextWatcher btcTextWatcher = null;
    private TextWatcher fiatTextWatcher = null;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                ((AddressAdapter) binding.accounts.spinner.getAdapter()).updateData(viewModel.getAddressList(false));
                ((AddressAdapter) binding.spDestination.getAdapter()).updateData(viewModel.getAddressList(true));
            }
        }
    };

    private Activity getActivity() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_send);
        viewModel = new SendViewModel(this, this);
        binding.setViewModel(viewModel);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setupToolbar();

        setCustomKeypad();

        setupViews();

        String scanData = getIntent().getStringExtra("scan_data");
        if (scanData != null) viewModel.handleIncomingQRScan(scanData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    private void setupToolbar() {
        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.send_bitcoin));
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
                } else {
                    startScanActivity(SCAN_URI);
                }
                return true;
            case R.id.action_send:
                customKeypad.setNumpadVisibility(View.GONE);

                if (ConnectivityStatus.hasConnectivity(this)) {
                    ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                    viewModel.setSendingAddress(selectedItem);
                    viewModel.calculateTransactionAmounts(selectedItem,
                            binding.amountRow.amountBtc.getText().toString(),
                            binding.customFee.getText().toString(),
                            () -> viewModel.sendClicked(false, binding.destination.getText().toString()));
                } else {
                    ToastCustom.makeText(this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity(int code) {
        if (!new AppUtil(this).isCameraOpen()) {
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivityForResult(intent, code);
        } else {
            ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            viewModel.handleIncomingQRScan(data.getStringExtra(CaptureActivity.SCAN_RESULT));

        } else if (requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK) {
            final String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            viewModel.handleScannedDataForWatchOnlySpend(scanData);
        }
    }

    @Override
    public void onBackPressed() {
        if (customKeypad.isVisible()) {
            onKeypadClose();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity(SCAN_URI);
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onKeypadClose() {
        customKeypad.setNumpadVisibility(View.GONE);
    }

    private void setCustomKeypad() {

        customKeypad = new CustomKeypad(this, (binding.keypad.numericPad));
        customKeypad.setDecimalSeparator(viewModel.getDefaultSeparator());

        //Enable custom keypad and disables default keyboard from popping up
        customKeypad.enableOnView(binding.amountRow.amountBtc);
        customKeypad.enableOnView(binding.amountRow.amountFiat);
        customKeypad.enableOnView(binding.customFee);

        binding.amountRow.amountBtc.setText("");
        binding.amountRow.amountBtc.requestFocus();
    }

    private void setupViews() {

        setupDestinationView();
        setupSendFromView();
        setupReceiveToView();

        setBtcTextWatcher();
        setFiatTextWatcher();

        binding.amountRow.amountBtc.addTextChangedListener(btcTextWatcher);
        binding.amountRow.amountBtc.setSelectAllOnFocus(true);

        binding.amountRow.amountFiat.setHint("0" + viewModel.getDefaultSeparator() + "00");
        binding.amountRow.amountFiat.addTextChangedListener(fiatTextWatcher);
        binding.amountRow.amountFiat.setSelectAllOnFocus(true);

        binding.amountRow.amountBtc.setHint("0" + viewModel.getDefaultSeparator() + "00");

        binding.customFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable customizedFee) {
                ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                viewModel.calculateTransactionAmounts(selectedItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        customizedFee.toString(),
                        null);
            }
        });

        binding.max.setOnClickListener(view -> {
            ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
            viewModel.spendAllClicked(selectedItem, binding.customFee.getText().toString());
        });
    }

    private void setupDestinationView() {
        binding.destination.setHorizontallyScrolling(false);
        binding.destination.setLines(3);
        binding.destination.setOnTouchListener((view, motionEvent) -> {
            binding.destination.setText("");
            viewModel.setReceivingAddress(null);
            return false;
        });
        binding.destination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && customKeypad != null)
                customKeypad.setNumpadVisibility(View.GONE);
        });
    }

    private void setupSendFromView() {

        binding.accounts.spinner.setAdapter(new AddressAdapter(this, R.layout.spinner_item, viewModel.getAddressList(false), true));

        // Set drop down width equal to clickable view
        binding.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    binding.accounts.spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.setDropDownWidth(binding.accounts.spinner.getWidth());
                }
            }
        });
        binding.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                viewModel.setSendingAddress(selectedItem);
                viewModel.calculateTransactionAmounts(selectedItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        binding.customFee.getText().toString(), null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (getIntent().hasExtra(KEY_SELECTED_ACCOUNT_POSITION)
                && getIntent().getIntExtra(KEY_SELECTED_ACCOUNT_POSITION, -1) != -1) {
            binding.accounts.spinner.setSelection(getIntent().getIntExtra(KEY_SELECTED_ACCOUNT_POSITION, -1));
        } else {
            binding.accounts.spinner.setSelection(viewModel.getDefaultAccount());
        }
    }

    private void setupReceiveToView() {
        binding.spDestination.setAdapter(new AddressAdapter(this, R.layout.spinner_item, viewModel.getAddressList(true), false));

        // Set drop down width equal to clickable view
        binding.spDestination.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.spDestination.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    binding.spDestination.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (binding.accounts.spinner.getWidth() > 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        binding.spDestination.setDropDownWidth(binding.accounts.spinner.getWidth());
                    }
            }
        });

        binding.spDestination.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        ItemAccount selectedItem = (ItemAccount) binding.spDestination.getSelectedItem();
                        binding.destination.setText(selectedItem.label);
                        viewModel.setReceivingAddress(selectedItem);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    @Override
    public void onHideSendingAddressField() {
        binding.fromRow.setVisibility(View.GONE);
    }

    @Override
    public void onHideReceivingAddressField() {
        binding.spDestination.setVisibility(View.GONE);
        binding.destination.setHint(R.string.to_field_helper_no_dropdown);
    }

    @Override
    public void onShowInvalidAmount() {
        ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
    }

    @Override
    public void onUpdateBtcAmount(String amount) {
        binding.amountRow.amountBtc.setText(amount);
        binding.amountRow.amountBtc.setSelection(binding.amountRow.amountBtc.getText().length());
    }

    @Override
    public void onRemoveBtcTextChangeListener() {
        binding.amountRow.amountBtc.removeTextChangedListener(btcTextWatcher);
    }

    @Override
    public void onAddBtcTextChangeListener() {
        binding.amountRow.amountBtc.addTextChangedListener(btcTextWatcher);
    }

    @Override
    public void onUpdateFiatAmount(String amount) {
        binding.amountRow.amountFiat.setText(amount);
        binding.amountRow.amountFiat.setSelection(binding.amountRow.amountFiat.getText().length());
    }

    @Override
    public void onRemoveFiatTextChangeListener() {
        binding.amountRow.amountFiat.removeTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void onAddFiatTextChangeListener() {
        binding.amountRow.amountFiat.addTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onShowTransactionSuccess() {

        runOnUiThread(() -> {

            playAudio();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.modal_transaction_success, null);
            transactionSuccessDialog = dialogBuilder.setView(dialogView)
                    .setPositiveButton(getString(R.string.done), null)
                    .create();
            transactionSuccessDialog.setTitle(R.string.transaction_submitted);

            AppRate appRate = new AppRate(getActivity())
                    .setMinTransactionsUntilPrompt(3)
                    .incrementTransactionCount();

            // If should show app rate, success dialog shows first and launches
            // rate dialog on dismiss. Dismissing rate dialog then closes the page. This will
            // happen if the user choses to rate the app - they'll return to the main page.
            if (appRate.shouldShowDialog()) {
                AlertDialog ratingDialog = appRate.getRateDialog();
                ratingDialog.setOnDismissListener(d -> finish());
                transactionSuccessDialog.show();
                transactionSuccessDialog.setOnDismissListener(d -> ratingDialog.show());
            } else {
                transactionSuccessDialog.show();
                transactionSuccessDialog.setOnDismissListener(dialogInterface -> finish());
            }

            dialogHandler.postDelayed(dialogRunnable, 5 * 1000);
        });
    }

    private final Handler dialogHandler = new Handler();
    private final Runnable dialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (transactionSuccessDialog != null && transactionSuccessDialog.isShowing()) {
                transactionSuccessDialog.dismiss();
            }
        }
    };

    @Override
    public void onShowBIP38PassphrasePrompt(String scanData) {

        runOnUiThread(() -> {
            final EditText password = new EditText(getActivity());
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.bip38_password_entry)
                    .setView(password)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                            viewModel.spendFromWatchOnlyBIP38(password.getText().toString(), scanData))
                    .setNegativeButton(android.R.string.cancel, null).show();
        });
    }

    private void onShowLargeTransactionWarning(AlertDialog alertDialog) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        AlertGenericWarningBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
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
        dialogBinding.confirmChange.setOnClickListener(v -> {
            alertDialogFee.dismiss();
        });

        alertDialogFee.show();
    }

    @Override
    public void onUpdateBtcUnit(String unit) {
        binding.amountRow.currencyBtc.setText(unit);
    }

    @Override
    public void onUpdateFiatUnit(String unit) {
        binding.amountRow.currencyFiat.setText(unit);
    }

    @Override
    public void onSetSpendAllAmount(String textFromSatoshis) {
        runOnUiThread(() -> binding.amountRow.amountBtc.setText(textFromSatoshis));
    }

    @Override
    public void onShowSpendFromWatchOnly(String address) {

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.privx_required)
                .setMessage(String.format(getString(R.string.watch_only_spend_instructionss), address))
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, (dialog, whichButton) -> {

                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), getActivity());
                    } else {
                        startScanActivity(SCAN_PRIVX);
                    }

                }).setNegativeButton(android.R.string.cancel, null).show();

    }

    private void setBtcTextWatcher() {

        btcTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                viewModel.afterBtcTextChanged(editable.toString());
                setKeyListener(editable, binding.amountRow.amountBtc);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

    }

    private void setFiatTextWatcher() {

        fiatTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                viewModel.afterFiatTextChanged(editable.toString());
                setKeyListener(editable, binding.amountRow.amountFiat);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

    }

    void setKeyListener(Editable s, EditText editText) {
        if (s.toString().contains(viewModel.getDefaultDecimalSeparator())) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        } else {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789" + viewModel.getDefaultDecimalSeparator()));
        }
    }

    @Override
    public void onShowPaymentDetails(PaymentConfirmationDetails details) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SendActivity.this);
        FragmentSendConfirmBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(SendActivity.this),
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
        dialogBinding.ivFeeInfo.setOnClickListener(view -> new AlertDialog.Builder(SendActivity.this)
                .setTitle(R.string.transaction_fee)
                .setMessage(finalFeeMessage)
                .setPositiveButton(android.R.string.ok, null).show());

        if (details.isSurge) {
            dialogBinding.confirmFeeBtc.setTextColor(ContextCompat.getColor(SendActivity.this, R.color.blockchain_send_red));
            dialogBinding.confirmFeeFiat.setTextColor(ContextCompat.getColor(SendActivity.this, R.color.blockchain_send_red));
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
        }

        dialogBinding.tvCustomizeFee.setOnClickListener(v -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.cancel();
            }

            runOnUiThread(() -> {
                binding.customFeeContainer.setVisibility(View.VISIBLE);

                binding.customFee.setText(details.btcFee);
                binding.customFee.setHint(details.btcSuggestedFee);
                binding.customFee.requestFocus();
                binding.customFee.setSelection(binding.customFee.getText().length());
                customKeypad.setNumpadVisibility(View.GONE);
            });

            alertCustomSpend(details.btcSuggestedFee, details.btcUnit);

        });

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            if (ConnectivityStatus.hasConnectivity(this)) {
                viewModel.submitPayment(alertDialog);
            } else {
                ToastCustom.makeText(this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                // Queue tx here
            }
        });

        alertDialog.show();
        // To prevent the dialog from appearing too large on Android N
        alertDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        if (viewModel.isLargeTransaction()) {
            onShowLargeTransactionWarning(alertDialog);
        }
    }

    @Override
    public void onShowReceiveToWatchOnlyWarning(String address) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
                R.layout.alert_watch_only_spend, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());
        dialogBuilder.setCancelable(false);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            binding.destination.setText("");
            if (dialogBinding.confirmDontAskAgain.isChecked())
                viewModel.setWatchOnlySpendWarning(false);
            alertDialog.dismiss();
        });

        dialogBinding.confirmContinue.setOnClickListener(v -> {
            binding.destination.setText(address);
            if (dialogBinding.confirmDontAskAgain.isChecked())
                viewModel.setWatchOnlySpendWarning(false);
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    @Override
    public void onShowAlterFee(String absoluteFeeSuggested,
                               String body,
                               int positiveAction,
                               int negativeAction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertGenericWarningBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(SendActivity.this),
                R.layout.alert_generic_warning, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialogFee = dialogBuilder.create();
        alertDialogFee.setCanceledOnTouchOutside(false);

        dialogBinding.tvBody.setText(body);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();
        });

        dialogBinding.confirmKeep.setText(getResources().getString(negativeAction));
        dialogBinding.confirmKeep.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();

            ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
            viewModel.setSendingAddress(selectedItem);
            viewModel.calculateTransactionAmounts(selectedItem,
                    binding.amountRow.amountBtc.getText().toString(),
                    binding.customFee.getText().toString(),
                    () -> viewModel.sendClicked(true, binding.destination.getText().toString()));
        });

        dialogBinding.confirmChange.setText(getResources().getString(positiveAction));
        dialogBinding.confirmChange.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();

            ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
            viewModel.setSendingAddress(selectedItem);
            viewModel.calculateTransactionAmounts(selectedItem,
                    binding.amountRow.amountBtc.getText().toString(),
                    absoluteFeeSuggested,
                    () -> viewModel.sendClicked(true, binding.destination.getText().toString()));

        });
        alertDialogFee.show();
    }

    private void alertCustomSpend(String btcFee, String btcFeeUnit) {

        String message = getResources().getString(R.string.recommended_fee)
                + "\n\n"
                + btcFee
                + " " + btcFeeUnit;

        new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_fee)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void playAudio() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getApplicationContext(), R.raw.beep);
            mp.setOnCompletionListener(mp1 -> {
                mp1.reset();
                mp1.release();
            });
            mp.start();
        }
    }
}