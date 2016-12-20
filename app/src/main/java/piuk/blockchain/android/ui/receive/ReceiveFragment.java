package piuk.blockchain.android.ui.receive;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RelativeLayout;

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
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentReceiveBinding;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.CustomKeypad;
import piuk.blockchain.android.ui.customviews.CustomKeypadCallback;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.send.AddressAdapter;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

public class ReceiveFragment extends Fragment implements ReceiveViewModel.DataListener, CustomKeypadCallback {

    private static final String TAG = ReceiveFragment.class.getSimpleName();
    private static final String LINK_ADDRESS_INFO = "https://support.blockchain.com/hc/en-us/articles/210353663-Why-is-my-bitcoin-address-changing-";
    private static final String ARG_IS_BTC = "is_btc";
    private static final String ARG_SELECTED_ACCOUNT_POSITION = "selected_account_position";
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk ReceiveViewModel viewModel;
    @Thunk FragmentReceiveBinding binding;
    private CustomKeypad customKeypad;
    private BottomSheetDialog bottomSheetDialog;
    private AddressAdapter addressAdapter;
    private OnReceiveFragmentInteractionListener listener;

    @Thunk boolean textChangeAllowed = true;
    private boolean showInfoButton = false;
    private String uri;
    private long backPressed;
    private boolean isBtc;
    private int selectedAccountPosition = -1;

    private IntentFilter intentFilter = new IntentFilter(BalanceFragment.ACTION_INTENT);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(BalanceFragment.ACTION_INTENT)) {
                if (viewModel != null) {
                    // Update UI with new Address + QR
                    viewModel.updateSpinnerList();
                    displayQRCode(binding.content.accounts.spinner.getSelectedItemPosition());
                }
            }
        }
    };

    public ReceiveFragment() {
        // Required empty public constructor
    }

    public static ReceiveFragment newInstance(boolean isBtc, int selectedAccountPosition) {
        ReceiveFragment fragment = new ReceiveFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_BTC, isBtc);
        args.putInt(ARG_SELECTED_ACCOUNT_POSITION, selectedAccountPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isBtc = getArguments().getBoolean(ARG_IS_BTC, true);
            selectedAccountPosition = getArguments().getInt(ARG_SELECTED_ACCOUNT_POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_receive, container, false);
        viewModel = new ReceiveViewModel(this, Locale.getDefault());

        setupToolbar();

        viewModel.onViewReady();

        setupLayout();

        if (selectedAccountPosition != -1) {
            selectAccount(selectedAccountPosition);
        } else {
            selectAccount(viewModel.getDefaultSpinnerPosition());
        }

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    private void setupToolbar() {
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.receive_bitcoin);

            ViewUtils.setElevation(
                    getActivity().findViewById(R.id.appbar_layout),
                    ViewUtils.convertDpToPixel(5F, getContext()));
        } else {
            finishPage();
        }
    }

    private void setupLayout() {
        setCustomKeypad();

        if (viewModel.getReceiveToList().size() == 1) {
            binding.content.fromRow.setVisibility(View.GONE);
        }

        // BTC Field
        binding.content.amountContainer.amountBtc.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        binding.content.amountContainer.amountBtc.setHint("0" + getDefaultDecimalSeparator() + "00");
        binding.content.amountContainer.amountBtc.addTextChangedListener(mBtcTextWatcher);

        // Fiat Field
        binding.content.amountContainer.amountFiat.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        binding.content.amountContainer.amountFiat.setHint("0" + getDefaultDecimalSeparator() + "00");
        binding.content.amountContainer.amountFiat.setText("0" + getDefaultDecimalSeparator() + "00");
        binding.content.amountContainer.amountFiat.addTextChangedListener(mFiatTextWatcher);

        // Units
        binding.content.amountContainer.currencyBtc.setText(viewModel.getCurrencyHelper().getBtcUnit());
        binding.content.amountContainer.currencyFiat.setText(viewModel.getCurrencyHelper().getFiatUnit());

        // Spinner
        addressAdapter = new AddressAdapter(
                getActivity(),
                R.layout.spinner_item,
                viewModel.getReceiveToList(),
                true,
                isBtc,
                new MonetaryUtil(viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)),
                viewModel.getPrefsUtil().getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY),
                viewModel.getCurrencyHelper().getLastPrice());

        addressAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        binding.content.accounts.spinner.setAdapter(addressAdapter);
        binding.content.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                binding.content.accounts.spinner.setSelection(binding.content.accounts.spinner.getSelectedItemPosition());
                Object object = viewModel.getAccountItemForPosition(binding.content.accounts.spinner.getSelectedItemPosition());

                if (viewModel.warnWatchOnlySpend()) {
                    promptWatchOnlySpendWarning(object);
                }

                displayQRCode(binding.content.accounts.spinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        });

        binding.content.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.content.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    binding.content.accounts.spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.content.accounts.spinner.setDropDownWidth(binding.content.accounts.spinner.getWidth());
                }
            }
        });

        // Info Button
        binding.content.ivAddressInfo.setOnClickListener(v -> showAddressChangedInfo());

        // QR Code
        binding.content.qr.setOnClickListener(v -> showClipboardWarning());
        binding.content.qr.setOnLongClickListener(view -> {
            onShareClicked();
            return true;
        });
    }

    private TextWatcher mBtcTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            binding.content.amountContainer.amountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(viewModel.getCurrencyHelper().getMaxBtcDecimalLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, viewModel.getCurrencyHelper().getMaxBtcDecimalLength(), binding.content.amountContainer.amountBtc);

            binding.content.amountContainer.amountBtc.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateFiatTextField(s.toString());

                displayQRCode(binding.content.accounts.spinner.getSelectedItemPosition());
                textChangeAllowed = true;
            }
            setKeyListener(s, binding.content.amountContainer.amountBtc);
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

            binding.content.amountContainer.amountFiat.removeTextChangedListener(this);
            int maxLength = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(maxLength + 1);
            fiatFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, maxLength, binding.content.amountContainer.amountFiat);

            binding.content.amountContainer.amountFiat.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateBtcTextField(s.toString());

                displayQRCode(binding.content.accounts.spinner.getSelectedItemPosition());
                textChangeAllowed = true;
            }
            setKeyListener(s, binding.content.amountContainer.amountFiat);
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
        customKeypad = binding.content.keyboard;
        customKeypad.setCallback(this);
        customKeypad.setDecimalSeparator(getDefaultDecimalSeparator());

        // Enable custom keypad and disables default keyboard from popping up
        customKeypad.enableOnView(binding.content.amountContainer.amountBtc);
        customKeypad.enableOnView(binding.content.amountContainer.amountFiat);

        binding.content.amountContainer.amountBtc.setText("");
        binding.content.amountContainer.amountBtc.requestFocus();
    }

    private void selectAccount(int position) {
        if (binding.content.accounts.spinner != null) {
            displayQRCode(position);
        }
    }

    @Thunk
    void displayQRCode(int spinnerIndex) {
        binding.content.accounts.spinner.setSelection(spinnerIndex);

        Object object = viewModel.getAccountItemForPosition(spinnerIndex);
        showInfoButton = showAddressInfoButtonIfNecessary(object);

        String receiveAddress;
        if (object instanceof LegacyAddress) {
            receiveAddress = ((LegacyAddress) object).getAddress();
        } else {
            receiveAddress = viewModel.getV3ReceiveAddress((Account) object);
        }

        binding.content.receivingAddress.setText(receiveAddress);

        long amountLong;
        if (isBtc) {
            amountLong = viewModel.getCurrencyHelper().getLongAmount(
                    binding.content.amountContainer.amountBtc.getText().toString());
        } else {
            amountLong = viewModel.getCurrencyHelper().getLongAmount(
                    binding.content.amountContainer.amountFiat.getText().toString());
        }

        BigInteger amountBigInt = viewModel.getCurrencyHelper().getUndenominatedAmount(amountLong);

        if (viewModel.getCurrencyHelper().getIfAmountInvalid(amountBigInt)) {
            ToastCustom.makeText(getContext(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        if (receiveAddress != null) {
            if (!amountBigInt.equals(BigInteger.ZERO)) {
                uri = BitcoinURI.convertToBitcoinURI(receiveAddress, Coin.valueOf(amountBigInt.longValue()), "", "");
            } else {
                uri = "bitcoin:" + receiveAddress;
            }
            viewModel.generateQrCode(uri);
        }
    }

    @Override
    public void updateFiatTextField(String text) {
        binding.content.amountContainer.amountFiat.setText(text);
    }

    @Override
    public void updateBtcTextField(String text) {
        binding.content.amountContainer.amountBtc.setText(text);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.updateSpinnerList();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void showQrLoading() {
        binding.content.ivAddressInfo.setVisibility(View.GONE);
        binding.content.qr.setVisibility(View.GONE);
        binding.content.receivingAddress.setVisibility(View.GONE);
        binding.content.progressBar2.setVisibility(View.VISIBLE);
    }

    @Override
    public void showQrCode(@Nullable Bitmap bitmap) {
        binding.content.progressBar2.setVisibility(View.GONE);
        binding.content.qr.setVisibility(View.VISIBLE);
        binding.content.receivingAddress.setVisibility(View.VISIBLE);
        binding.content.qr.setImageBitmap(bitmap);
        if (showInfoButton) {
            binding.content.ivAddressInfo.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomSheet(String uri) {
        List<ReceiveViewModel.SendPaymentCodeData> list = viewModel.getIntentDataList(uri);
        if (list != null) {
            ShareReceiveIntentAdapter adapter = new ShareReceiveIntentAdapter(list);
            adapter.setItemClickedListener(() -> bottomSheetDialog.dismiss());

            View sheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_receive, null);
            RecyclerView recyclerView = (RecyclerView) sheetView.findViewById(R.id.recycler_view);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog);
            bottomSheetDialog.setContentView(sheetView);

            adapter.notifyDataSetChanged();
        }
    }

    private boolean showAddressInfoButtonIfNecessary(Object object) {
        return !(object instanceof ImportedAccount || object instanceof LegacyAddress);
    }

    private void onShareClicked() {
        closeKeypad();

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtil.requestWriteStoragePermissionFromActivity(binding.getRoot(), getActivity());
                    } else {
                        showBottomSheet();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showBottomSheet() {
        setupBottomSheet(uri);
        bottomSheetDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showBottomSheet();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showClipboardWarning() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = android.content.ClipData.newPlainText("Send address", binding.content.receivingAddress.getText().toString());
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                    clipboard.setPrimaryClip(clip);

                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private AlertDialog showAddressChangedInfo() {
        return new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
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
                    LayoutInflater.from(getActivity()), R.layout.alert_watch_only_spend, null, false);

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setView(dialogBinding.getRoot())
                    .setCancelable(false)
                    .create();

            dialogBinding.confirmCancel.setOnClickListener(v -> {
                binding.content.accounts.spinner.setSelection(viewModel.getDefaultSpinnerPosition(), true);
                viewModel.setWarnWatchOnlySpend(!dialogBinding.confirmDontAskAgain.isChecked());
                alertDialog.dismiss();
            });

            dialogBinding.confirmContinue.setOnClickListener(v -> {
                viewModel.setWarnWatchOnlySpend(!dialogBinding.confirmDontAskAgain.isChecked());
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
        return ((BitmapDrawable) binding.content.qr.getDrawable()).getBitmap();
    }

    @Override
    public void showToast(String message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), message, ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onSpinnerDataChanged() {
        if (addressAdapter != null) addressAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_receive, menu);
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
            case R.id.action_share:
                onShareClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
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
        layoutParams.setMargins(
                0, 0, 0, (int) getResources().getDimension(R.dimen.action_bar_height));

        binding.content.scrollView.setLayoutParams(layoutParams);
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

        binding.content.scrollView.setLayoutParams(layoutParams);
    }

    public void finishPage() {
        if (listener != null) {
            listener.onReceiveFragmentClose();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnReceiveFragmentInteractionListener) {
            listener = (OnReceiveFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnReceiveFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnReceiveFragmentInteractionListener {

        void onReceiveFragmentClose();

    }
}
