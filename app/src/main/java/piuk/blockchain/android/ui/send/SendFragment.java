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
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RelativeLayout;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.databinding.AlertGenericWarningBinding;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentSendBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.CustomKeypad;
import piuk.blockchain.android.ui.customviews.CustomKeypadCallback;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AppRate;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static android.databinding.DataBindingUtil.inflate;


public class SendFragment extends Fragment implements SendViewModel.DataListener, CustomKeypadCallback {

    private static final String ARG_SCAN_DATA = "scan_data";
    private static final String ARG_IS_BTC = "is_btc";
    private static final String ARG_SELECTED_ACCOUNT_POSITION = "selected_account_position";
    private static final int SCAN_URI = 2007;
    private static final int SCAN_PRIVX = 2008;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk FragmentSendBinding binding;
    @Thunk SendViewModel viewModel;
    @Thunk AlertDialog transactionSuccessDialog;
    private OnSendFragmentInteractionListener listener;
    private CustomKeypad customKeypad;
    private TextWatcher btcTextWatcher;
    private TextWatcher fiatTextWatcher;

    private String scanData;
    private boolean isBtc;
    private int selectedAccountPosition = -1;
    private long backPressed;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                ((AddressAdapter) binding.accounts.spinner.getAdapter()).updateData(viewModel.getAddressList(false));
                ((AddressAdapter) binding.spDestination.getAdapter()).updateData(viewModel.getAddressList(true));
            }
        }
    };

    public SendFragment() {
        // Required empty public constructor
    }

    public static SendFragment newInstance(String scanData, boolean isBtc, int selectedAccountPosition) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SCAN_DATA, scanData);
        args.putBoolean(ARG_IS_BTC, isBtc);
        args.putInt(ARG_SELECTED_ACCOUNT_POSITION, selectedAccountPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            scanData = getArguments().getString(ARG_SCAN_DATA);
            isBtc = getArguments().getBoolean(ARG_IS_BTC, true);
            selectedAccountPosition = getArguments().getInt(ARG_SELECTED_ACCOUNT_POSITION);
        }

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_send, container, false);
        viewModel = new SendViewModel(getContext(), this);
        binding.setViewModel(viewModel);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setupToolbar();

        setCustomKeypad();

        setupViews();

        if (scanData != null) viewModel.handleIncomingQRScan(scanData);

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

        if (listener != null) {
            listener.onSendFragmentStart();
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    private void setupToolbar() {
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.send_bitcoin);

            ViewUtils.setElevation(
                    getActivity().findViewById(R.id.appbar_layout),
                    ViewUtils.convertDpToPixel(5F, getContext()));
        } else {
            finishPage();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.send_activity_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_qr_main);
        menuItem.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), getActivity());
                } else {
                    startScanActivity(SCAN_URI);
                }
                return true;
            case R.id.action_send:
                customKeypad.setNumpadVisibility(View.GONE);

                if (ConnectivityStatus.hasConnectivity(getActivity())) {
                    ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                    viewModel.setSendingAddress(selectedItem);
                    viewModel.calculateTransactionAmounts(selectedItem,
                            binding.amountRow.amountBtc.getText().toString(),
                            binding.customFee.getText().toString(),
                            () -> viewModel.sendClicked(false, binding.destination.getText().toString()));
                } else {
                    ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity(int code) {
        if (!new AppUtil(getActivity()).isCameraOpen()) {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, code);
        } else {
            ToastCustom.makeText(getActivity(), getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
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

    public void onBackPressed() {
        if (customKeypad.isVisible()) {
            closeKeypad();
        } else {
            handleBackPressed();
        }
    }

    public void handleBackPressed() {
        if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(getContext());
            return;
        } else {
            onExitConfirmToast();
        }

        backPressed = System.currentTimeMillis();
    }

    public void onExitConfirmToast() {
        ToastCustom.makeText(getActivity(), getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
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

    private void closeKeypad() {
        customKeypad.setNumpadVisibility(View.GONE);
    }

    @Override
    public void onKeypadClose() {
        // Show bottom nav
        ((MainActivity) getActivity()).getBottomNavigationView().restoreBottomNavigation();
        // Resize activity back to initial state
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.addRule(RelativeLayout.ABOVE, 0);
        binding.scrollView.setLayoutParams(layoutParams);
    }

    @Override
    public void onKeypadOpen() {
        // Hide bottom nav
        ((MainActivity) getActivity()).getBottomNavigationView().hideBottomNavigation();
    }

    @Override
    public void onKeypadOpenCompleted() {
        // Resize activity around keyboard view
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.addRule(RelativeLayout.ABOVE, R.id.keyboard);

        binding.scrollView.setLayoutParams(layoutParams);
    }

    private void setCustomKeypad() {
        customKeypad = binding.keyboard;
        customKeypad.setCallback(this);
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
        binding.destination.setOnClickListener(view -> {
            binding.destination.setText("");
            viewModel.setReceivingAddress(null);
        });
        binding.destination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && customKeypad != null)
                customKeypad.setNumpadVisibility(View.GONE);
        });
    }

    private void setupSendFromView() {

        String fiat = viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        binding.accounts.spinner.setAdapter(new AddressAdapter(
                getContext(),
                R.layout.spinner_item,
                viewModel.getAddressList(false),
                true,
                isBtc,
                new MonetaryUtil(viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)),
                fiat,
                ExchangeRateFactory.getInstance().getLastPrice(fiat)));

        // Set drop down width equal to clickable view
        binding.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
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

        if (selectedAccountPosition != -1) {
            binding.accounts.spinner.setSelection(selectedAccountPosition);
        } else {
            binding.accounts.spinner.setSelection(viewModel.getDefaultAccount());
        }
    }

    private void setupReceiveToView() {
        String fiat = viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        binding.spDestination.setAdapter(new AddressAdapter(
                getContext(),
                R.layout.spinner_item,
                viewModel.getAddressList(true),
                false,
                isBtc,
                new MonetaryUtil(viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)),
                fiat,
                ExchangeRateFactory.getInstance().getLastPrice(fiat)));

        // Set drop down width equal to clickable view
        binding.spDestination.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.spDestination.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
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
        ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onShowTransactionSuccess() {
        getActivity().runOnUiThread(() -> {
            playAudio();
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(new Intent(BalanceFragment.ACTION_INTENT));

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
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
                ratingDialog.setOnDismissListener(d -> finishPage());
                transactionSuccessDialog.show();
                transactionSuccessDialog.setOnDismissListener(d -> ratingDialog.show());
            } else {
                transactionSuccessDialog.show();
                transactionSuccessDialog.setOnDismissListener(dialogInterface -> finishPage());
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
        getActivity().runOnUiThread(() -> {
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
        AlertGenericWarningBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
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

        if (getActivity() != null && !getActivity().isFinishing()) {
            alertDialogFee.show();
        }
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
        getActivity().runOnUiThread(() -> binding.amountRow.amountBtc.setText(textFromSatoshis));
    }

    @Override
    public void onShowSpendFromWatchOnly(String address) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
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
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
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

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        FragmentSendConfirmBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
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
        dialogBinding.ivFeeInfo.setOnClickListener(view -> new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.transaction_fee)
                .setMessage(finalFeeMessage)
                .setPositiveButton(android.R.string.ok, null).show());

        if (details.isSurge) {
            dialogBinding.confirmFeeBtc.setTextColor(ContextCompat.getColor(getContext(), R.color.blockchain_send_red));
            dialogBinding.confirmFeeFiat.setTextColor(ContextCompat.getColor(getContext(), R.color.blockchain_send_red));
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
        }

        dialogBinding.tvCustomizeFee.setOnClickListener(v -> {
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }

            getActivity().runOnUiThread(() -> {
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
            if (alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            if (ConnectivityStatus.hasConnectivity(getActivity())) {
                dialogBinding.confirmSend.setClickable(false);
                viewModel.submitPayment(alertDialog);
            } else {
                ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                // Queue tx here
            }
        });

        if (getActivity() != null && !getActivity().isFinishing()) {
            alertDialog.show();
        }

        // To prevent the dialog from appearing too large on Android N
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        if (viewModel.isLargeTransaction()) {
            onShowLargeTransactionWarning(alertDialog);
        }
    }

    @Override
    public void onShowReceiveToWatchOnlyWarning(String address) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        AlertWatchOnlySpendBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
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

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        AlertGenericWarningBinding dialogBinding = inflate(LayoutInflater.from(getActivity()),
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

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.transaction_fee)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void playAudio() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.beep);
            mp.setOnCompletionListener(mp1 -> {
                mp1.reset();
                mp1.release();
            });
            mp.start();
        }
    }

    @Override
    public void finishPage() {
        if (listener != null) {
            listener.onSendFragmentClose();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSendFragmentInteractionListener) {
            listener = (OnSendFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnSendFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnSendFragmentInteractionListener {

        void onSendFragmentClose();

        void onSendFragmentStart();
    }
}
