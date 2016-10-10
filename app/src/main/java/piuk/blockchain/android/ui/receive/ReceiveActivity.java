package piuk.blockchain.android.ui.receive;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityReceiveBinding;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.CustomKeypad;
import piuk.blockchain.android.ui.customviews.CustomKeypadCallback;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.AddressAdapter;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_SELECTED_ACCOUNT_POSITION;

public class ReceiveActivity extends BaseAuthActivity implements ReceiveViewModel.DataListener, CustomKeypadCallback {

    private static final String TAG = ReceiveActivity.class.getSimpleName();
    private static final String LINK_ADDRESS_INFO = "https://support.blockchain.com/hc/en-us/articles/210353663-Why-is-my-bitcoin-address-changing-";

    @Thunk ReceiveViewModel mViewModel;
    @Thunk ActivityReceiveBinding mBinding;
    private CustomKeypad mCustomKeypad;
    private BottomSheetBehavior mBottomSheetBehavior;
    private AddressAdapter mReceiveToAdapter;

    @Thunk boolean mTextChangeAllowed = true;
    private boolean mIsBTC = true;
    private boolean mShowInfoButton = false;
    private String mUri;

    private IntentFilter mFilter = new IntentFilter(BalanceFragment.ACTION_INTENT);
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(BalanceFragment.ACTION_INTENT)) {
                if (mViewModel != null) {
                    // Update UI with new Address + QR
                    mViewModel.updateSpinnerList();
                    displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_receive);
        mViewModel = new ReceiveViewModel(this, Locale.getDefault());

        Toolbar toolbar = mBinding.toolbar.toolbarGeneral;
        toolbar.setTitle(getResources().getString(R.string.receive_bitcoin));
        setSupportActionBar(toolbar);

        mViewModel.onViewReady();

        setupLayout();

        if (getIntent().hasExtra(KEY_SELECTED_ACCOUNT_POSITION)
                && getIntent().getIntExtra(KEY_SELECTED_ACCOUNT_POSITION, -1) != -1) {
            selectAccount(getIntent().getIntExtra(KEY_SELECTED_ACCOUNT_POSITION, -1));
        } else {
            selectAccount(mViewModel.getDefaultSpinnerPosition());
        }
    }

    private void setupLayout() {
        setCustomKeypad();

        // Bottom Sheet
        mBottomSheetBehavior = BottomSheetBehavior.from(mBinding.bottomSheet.bottomSheet);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // No-op
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mBinding.content.receiveMainContentShadow.setAlpha(slideOffset / 2f);
                if (slideOffset > 0) {
                    mBinding.content.receiveMainContentShadow.setVisibility(View.VISIBLE);
                    mBinding.content.receiveMainContentShadow.bringToFront();
                } else {
                    mBinding.content.receiveMainContentShadow.setVisibility(View.GONE);
                }
            }
        });

        mBinding.content.receiveMainContentShadow.setOnClickListener(view ->
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        if (mViewModel.getReceiveToList().size() == 1) {
            mBinding.content.fromRow.setVisibility(View.GONE);
        }

        // BTC Field
        mBinding.content.amountContainer.amountBtc.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        mBinding.content.amountContainer.amountBtc.setHint("0" + getDefaultDecimalSeparator() + "00");
        mBinding.content.amountContainer.amountBtc.addTextChangedListener(mBtcTextWatcher);

        // Fiat Field
        mBinding.content.amountContainer.amountFiat.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        mBinding.content.amountContainer.amountFiat.setHint("0" + getDefaultDecimalSeparator() + "00");
        mBinding.content.amountContainer.amountFiat.setText("0" + getDefaultDecimalSeparator() + "00");
        mBinding.content.amountContainer.amountFiat.addTextChangedListener(mFiatTextWatcher);

        // Units
        mBinding.content.amountContainer.currencyBtc.setText(mViewModel.getCurrencyHelper().getBtcUnit());
        mBinding.content.amountContainer.currencyFiat.setText(mViewModel.getCurrencyHelper().getFiatUnit());

        // Spinner
        mReceiveToAdapter = new AddressAdapter(this, R.layout.spinner_item, mViewModel.getReceiveToList(), true);
        mReceiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        mBinding.content.accounts.spinner.setAdapter(mReceiveToAdapter);
        mBinding.content.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mBinding.content.accounts.spinner.setSelection(mBinding.content.accounts.spinner.getSelectedItemPosition());
                Object object = mViewModel.getAccountItemForPosition(mBinding.content.accounts.spinner.getSelectedItemPosition());

                if (mViewModel.warnWatchOnlySpend()) {
                    promptWatchOnlySpendWarning(object);
                }

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        });

        mBinding.content.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mBinding.content.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mBinding.content.accounts.spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mBinding.content.accounts.spinner.setDropDownWidth(mBinding.content.accounts.spinner.getWidth());
                }
            }
        });

        // Info Button
        mBinding.content.ivAddressInfo.setOnClickListener(v -> showAddressChangedInfo());

        // QR Code
        mBinding.content.qr.setOnClickListener(v -> showClipboardWarning());
        mBinding.content.qr.setOnLongClickListener(view -> {
            onShareClicked();
            return true;
        });
    }

    private TextWatcher mBtcTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            mBinding.content.amountContainer.amountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(mViewModel.getCurrencyHelper().getMaxBtcDecimalLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, mViewModel.getCurrencyHelper().getMaxBtcDecimalLength(), mBinding.content.amountContainer.amountBtc);

            mBinding.content.amountContainer.amountBtc.addTextChangedListener(this);

            if (mTextChangeAllowed) {
                mTextChangeAllowed = false;
                mViewModel.updateFiatTextField(s.toString());

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                mTextChangeAllowed = true;
            }
            setKeyListener(s, mBinding.content.amountContainer.amountBtc);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }
    };

    private TextWatcher mFiatTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            mBinding.content.amountContainer.amountFiat.removeTextChangedListener(this);
            int maxLength = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(maxLength + 1);
            fiatFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, maxLength, mBinding.content.amountContainer.amountFiat);

            mBinding.content.amountContainer.amountFiat.addTextChangedListener(this);

            if (mTextChangeAllowed) {
                mTextChangeAllowed = false;
                mViewModel.updateBtcTextField(s.toString());

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                mTextChangeAllowed = true;
            }
            setKeyListener(s, mBinding.content.amountContainer.amountFiat);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }
    };

    @Thunk
    void setKeyListener(Editable s, EditText editText) {
        if (s.toString().contains(getDefaultDecimalSeparator())) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        } else {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        }
    }

    @Thunk
    Editable formatEditable(Editable s, String input, int maxLength, EditText editText) {
        try {
            if (input.contains(getDefaultDecimalSeparator())) {
                String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > maxLength) {
                        editText.setText(input.substring(0, input.length() - 1));
                        editText.setSelection(editText.getText().length());
                        s = editText.getEditableText();
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "afterTextChanged: ", e);
        }
        return s;
    }

    private void setCustomKeypad() {
        mCustomKeypad = new CustomKeypad(this, (mBinding.content.keypadContainer.numericPad));
        mCustomKeypad.setDecimalSeparator(getDefaultDecimalSeparator());

        // Enable custom keypad and disables default keyboard from popping up
        mCustomKeypad.enableOnView(mBinding.content.amountContainer.amountBtc);
        mCustomKeypad.enableOnView(mBinding.content.amountContainer.amountFiat);

        mBinding.content.amountContainer.amountBtc.setText("");
        mBinding.content.amountContainer.amountBtc.requestFocus();
    }

    private void selectAccount(int position) {
        if (mBinding.content.accounts.spinner != null) {
            displayQRCode(position);
        }
    }

    @Thunk
    void displayQRCode(int spinnerIndex) {
        mBinding.content.accounts.spinner.setSelection(spinnerIndex);

        Object object = mViewModel.getAccountItemForPosition(spinnerIndex);
        mShowInfoButton = showAddressInfoButtonIfNecessary(object);

        String receiveAddress;
        if (object instanceof LegacyAddress) {
            receiveAddress = ((LegacyAddress) object).getAddress();
        } else {
            receiveAddress = mViewModel.getV3ReceiveAddress((Account) object);
        }

        mBinding.content.receivingAddress.setText(receiveAddress);

        long amountLong;
        if (mIsBTC) {
            amountLong = mViewModel.getCurrencyHelper().getLongAmount(
                    mBinding.content.amountContainer.amountBtc.getText().toString());
        } else {
            amountLong = mViewModel.getCurrencyHelper().getLongAmount(
                    mBinding.content.amountContainer.amountFiat.getText().toString());
        }

        BigInteger amountBigInt = mViewModel.getCurrencyHelper().getUndenominatedAmount(amountLong);

        if (mViewModel.getCurrencyHelper().getIfAmountInvalid(amountBigInt)) {
            ToastCustom.makeText(this, this.getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        if (!amountBigInt.equals(BigInteger.ZERO)) {
            mUri = BitcoinURI.convertToBitcoinURI(receiveAddress, Coin.valueOf(amountBigInt.longValue()), "", "");
        } else {
            mUri = "bitcoin:" + receiveAddress;
        }

        mViewModel.generateQrCode(mUri);
    }

    @Override
    public void updateFiatTextField(String text) {
        mBinding.content.amountContainer.amountFiat.setText(text);
    }

    @Override
    public void updateBtcTextField(String text) {
        mBinding.content.amountContainer.amountBtc.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.content.amountContainer.currencyBtc.setText(
                mIsBTC ? mViewModel.getCurrencyHelper().getBtcUnit() : mViewModel.getCurrencyHelper().getFiatUnit());
        mBinding.content.amountContainer.currencyFiat.setText(
                mIsBTC ? mViewModel.getCurrencyHelper().getFiatUnit() : mViewModel.getCurrencyHelper().getBtcUnit());
        mViewModel.updateSpinnerList();

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mFilter);
    }

    @Override
    public void showQrLoading() {
        mBinding.content.ivAddressInfo.setVisibility(View.GONE);
        mBinding.content.qr.setVisibility(View.GONE);
        mBinding.content.receivingAddress.setVisibility(View.GONE);
        mBinding.content.progressBar2.setVisibility(View.VISIBLE);
    }

    @Override
    public void showQrCode(@Nullable Bitmap bitmap) {
        mBinding.content.progressBar2.setVisibility(View.GONE);
        mBinding.content.qr.setVisibility(View.VISIBLE);
        mBinding.content.receivingAddress.setVisibility(View.VISIBLE);
        mBinding.content.qr.setImageBitmap(bitmap);
        if (mShowInfoButton) {
            mBinding.content.ivAddressInfo.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomSheet(String uri) {
        List<ReceiveViewModel.SendPaymentCodeData> list = mViewModel.getIntentDataList(uri);
        if (list != null) {
            ShareReceiveIntentAdapter adapter = new ShareReceiveIntentAdapter(list);
            mBinding.bottomSheet.recyclerView.setAdapter(adapter);
            mBinding.bottomSheet.recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter.notifyDataSetChanged();
        }
    }

    private boolean showAddressInfoButtonIfNecessary(Object object) {
        return !(object instanceof ImportedAccount || object instanceof LegacyAddress);
    }

    private void onShareClicked() {
        onKeypadClose();

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    setupBottomSheet(mUri);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showClipboardWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = android.content.ClipData.newPlainText("Send address", mBinding.content.receivingAddress.getText().toString());
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                    clipboard.setPrimaryClip(clip);

                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private AlertDialog showAddressChangedInfo() {
        return new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.why_has_my_address_changed))
                .setMessage(getString(R.string.new_address_info))
                .setPositiveButton(R.string.learn_more, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse(LINK_ADDRESS_INFO));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    @Thunk
    void promptWatchOnlySpendWarning(Object object) {
        if (object instanceof LegacyAddress && ((LegacyAddress) object).isWatchOnly()) {

            AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(this), R.layout.alert_watch_only_spend, null, false);

            AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setView(dialogBinding.getRoot())
                    .setCancelable(false)
                    .create();

            dialogBinding.confirmCancel.setOnClickListener(v -> {
                mBinding.content.accounts.spinner.setSelection(mViewModel.getDefaultSpinnerPosition(), true);
                mViewModel.setWarnWatchOnlySpend(!dialogBinding.confirmDontAskAgain.isChecked());
                alertDialog.dismiss();
            });

            dialogBinding.confirmContinue.setOnClickListener(v -> {
                mViewModel.setWarnWatchOnlySpend(!dialogBinding.confirmDontAskAgain.isChecked());
                alertDialog.dismiss();
            });

            alertDialog.show();
        }
    }

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    @Override
    public Bitmap getQrBitmap() {
        return ((BitmapDrawable) mBinding.content.qr.getDrawable()).getBitmap();
    }

    @Override
    public void showToast(String message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, message, ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onSpinnerDataChanged() {
        if (mReceiveToAdapter != null) mReceiveToAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_receive, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                onShareClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCustomKeypad.isVisible()) {
            onKeypadClose();
        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.destroy();
    }

    private Context getActivity() {
        return this;
    }

    @Override
    public void onKeypadClose() {
        mCustomKeypad.setNumpadVisibility(View.GONE);
    }
}
