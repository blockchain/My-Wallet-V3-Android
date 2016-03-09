package info.blockchain.wallet;

import com.google.common.collect.HashBiMap;
import com.google.zxing.client.android.CaptureActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.callbacks.OpSimpleCallback;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.AddressBookEntry;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FeeUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.ReselectSpinner;
import info.blockchain.wallet.util.ToastCustom;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.R;

public class SendFragment extends Fragment implements View.OnClickListener, CustomKeypadCallback {

    private final int SCAN_PRIVX = 301;
    public static boolean isKeypadVisible = false;
    private static Context context = null;
    private View rootView;

    private EditText edReceiveTo = null;
    private EditText edAmount1 = null;
    private TextView tvCurrency1 = null;
    private EditText edAmount2 = null;
    private TextView tvFiat2 = null;
    private MenuItem btSend;
    private TextView tvMax = null;
    private EditText etCustomFee = null;
    private TableLayout numpad;

    private Spinner sendFromSpinner = null;
    private List<String> sendFromList = null;
    private HashBiMap<Object, Integer> sendFromBiMap = null;
    private SendFromAdapter sendFromAdapter = null;

    private ReselectSpinner receiveToSpinner = null;
    private List<String> receiveToList = null;
    private HashBiMap<Object, Integer> receiveToBiMap = null;
    private ReceiveToAdapter receiveToAdapter = null;
    private HashMap<Integer, Integer> spinnerIndexAccountIndexMap = null;

    private TextWatcher btcTextWatcher = null;
    private TextWatcher fiatTextWatcher = null;

    private ProgressDialog progress = null;

    private String strBTC = "BTC";
    private String strFiat = null;
    private double btc_fx = 319.13;
    private boolean textChangeAllowed = true;
    private String defaultSeparator;//Decimal separator based on locale
    private boolean spendInProgress = false;//Used to avoid double clicking on spend and opening confirm dialog twice
    private boolean spDestinationSelected = false;//When a destination is selected from dropdown, mark spend as 'Moved'

    private PendingSpend watchOnlyPendingSpend;

    private BigInteger dynamicFee = SendCoins.bFee;
    private boolean dynamicFeeFailed = false;
    private boolean isFeeAbnormallyHigh;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                updateSendFromSpinnerList();
                updateReceiveToSpinnerList();
            }
        }
    };

    private class PendingSpend {

        boolean isHD;
        int fromXpubIndex;
        LegacyAddress fromLegacyAddress;
        Account fromAccount;
        String note;
        String destination;
        BigInteger bigIntFee;
        BigInteger bigIntAmount;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        setupToolbar();

        setupViews();

        defaultSeparator = getDefaultDecimalSeparator();

        setBtcTextWatcher();

        setFiatTextWatcher();

        sendFromList = new ArrayList<>();
        sendFromBiMap = HashBiMap.create();
        receiveToList = new ArrayList<>();
        receiveToBiMap = HashBiMap.create();
        spinnerIndexAccountIndexMap = new HashMap<>();

        updateSendFromSpinnerList();
        updateReceiveToSpinnerList();

        setSendFromDropDown();
        setReceiveToDropDown();

        initVars();

        handleIncomingQRScan();

        decimalCompatCheck(rootView);

        return rootView;
    }

    private void setupToolbar(){

        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((ActionBarActivity) getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(R.string.send_bitcoin);
        setHasOptionsMenu(true);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }

                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
        });

    }

    private void setupViews(){

        sendFromSpinner = (Spinner) rootView.findViewById(R.id.accounts);
        receiveToSpinner = (ReselectSpinner) rootView.findViewById(R.id.sp_destination);
        edReceiveTo = ((EditText) rootView.findViewById(R.id.destination));

        edAmount1 = ((EditText) rootView.findViewById(R.id.amount1));
        edAmount2 = (EditText) rootView.findViewById(R.id.amount2);

        tvCurrency1 = (TextView) rootView.findViewById(R.id.currency1);
        tvFiat2 = (TextView) rootView.findViewById(R.id.fiat2);
        tvMax = (TextView) rootView.findViewById(R.id.max);

        etCustomFee = (EditText) rootView.findViewById(R.id.custom_fee);
        //As soon as the user customizes our suggested dynamic fee - hide (recommended)
        etCustomFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                rootView.findViewById(R.id.tv_recommended).setVisibility(View.GONE);
                displayMaxAvailable();
            }
        });
    }

    private void setSendFromDropDown(){

        if (sendFromList.size() == 1) rootView.findViewById(R.id.from_row).setVisibility(View.GONE);

        sendFromAdapter = new SendFromAdapter(getActivity(), R.layout.spinner_item, sendFromList);
        sendFromSpinner.setAdapter(sendFromAdapter);
        sendFromSpinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    sendFromSpinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    sendFromSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    sendFromSpinner.setDropDownWidth(sendFromSpinner.getWidth());
                }
            }
        });

        sendFromSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        displayMaxAvailable();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                }
        );

    }

    private void updateSendFromSpinnerList() {
        //sendFromList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        sendFromList.clear();
        sendFromBiMap.clear();

        int spinnerIndex = 0;

        if (PayloadFactory.getInstance().get().isUpgraded()) {

            //V3
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
            for (Account item : accounts) {

                if (item.isArchived())
                    continue;//skip archived account

                //no xpub watch only yet

                sendFromList.add(item.getLabel());
                sendFromBiMap.put(item, spinnerIndex);
                spinnerIndex++;
            }
        }

        List<LegacyAddress> legacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)
                continue;//skip archived

            //If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel() == null || legacyAddress.getLabel().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();

            //Append watch-only with a label - we'll asl for xpriv scan when spending from
            if(legacyAddress.isWatchOnly()){
                labelOrAddress = getResources().getString(R.string.watch_only_label)+" "+labelOrAddress;
            }

            sendFromList.add(labelOrAddress);
            sendFromBiMap.put(legacyAddress, spinnerIndex);
            spinnerIndex++;
        }

        //Notify adapter of list update
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sendFromAdapter != null) sendFromAdapter.notifyDataSetChanged();
            }
        });
    }

    private void updateReceiveToSpinnerList() {
        //receiveToList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        receiveToList.clear();
        receiveToBiMap.clear();
        spinnerIndexAccountIndexMap.clear();

        int spinnerIndex = 0;

        if (PayloadFactory.getInstance().get().isUpgraded()) {

            //V3
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
            int accountIndex = 0;
            for (Account item : accounts) {

                spinnerIndexAccountIndexMap.put(spinnerIndex, accountIndex);
                accountIndex++;

                if (item.isArchived())
                    continue;//skip archived account

                //no xpub watch only yet

                receiveToList.add(item.getLabel());
                receiveToBiMap.put(item, spinnerIndex);
                spinnerIndex++;
            }
        }

        List<LegacyAddress> legacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)
                continue;//skip archived address

            //If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel() == null || legacyAddress.getLabel().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();

            //Prefix "watch-only"
            if (legacyAddress.isWatchOnly()) {
                labelOrAddress = getActivity().getString(R.string.watch_only_label) + " " + labelOrAddress;
            }

            receiveToList.add(labelOrAddress);
            receiveToBiMap.put(legacyAddress, spinnerIndex);
            spinnerIndex++;
        }

        //Address Book
        List<AddressBookEntry> addressBookEntries = PayloadFactory.getInstance().get().getAddressBookEntries();

        for(AddressBookEntry addressBookEntry : addressBookEntries){

            //If address has no label, we'll display address
            String labelOrAddress = addressBookEntry.getLabel() == null || addressBookEntry.getLabel().length() == 0 ? addressBookEntry.getAddress() : addressBookEntry.getLabel();

            receiveToList.add(labelOrAddress);
            receiveToBiMap.put(addressBookEntry, spinnerIndex);
            spinnerIndex++;

        }

        //Notify adapter of list update
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (receiveToAdapter != null) receiveToAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setReceiveToDropDown(){

        receiveToAdapter = new ReceiveToAdapter(getActivity(), R.layout.spinner_item, receiveToList);
        receiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);

        //If there is only 1 account/address - hide drop down
        if (receiveToList.size() <= 1) receiveToSpinner.setVisibility(View.GONE);

        receiveToSpinner.setAdapter(receiveToAdapter);
        receiveToSpinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    receiveToSpinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    receiveToSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if(sendFromSpinner.getWidth() > 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        receiveToSpinner.setDropDownWidth(sendFromSpinner.getWidth());
                    }
            }
        });

        receiveToSpinner.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        final Object object = receiveToBiMap.inverse().get(receiveToSpinner.getSelectedItemPosition());

                        if (object instanceof LegacyAddress) {

                            //V2
                            if (((LegacyAddress) object).isWatchOnly() && PrefsUtil.getInstance(getActivity()).getValue("WARN_WATCH_ONLY_SPEND", true)) {

                                promptWatchOnlySpendWarning(object);

                            } else {
                                edReceiveTo.setText(((LegacyAddress) object).getAddress());
                            }
                        } else if(object instanceof Account){
                            //V3
                            //TODO - V3 no watch only yet
                            edReceiveTo.setText(getV3ReceiveAddress((Account) object));

                        } else if (object instanceof AddressBookEntry){
                            //Address book
                            edReceiveTo.setText(((AddressBookEntry) object).getAddress());
                        }

                        spDestinationSelected = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void promptWatchOnlySpendWarning(final Object object){

        if (object instanceof LegacyAddress && ((LegacyAddress) object).isWatchOnly()) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.alert_watch_only_spend, null);
            dialogBuilder.setView(dialogView);
            dialogBuilder.setCancelable(false);

            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);

            final CheckBox confirmDismissForever = (CheckBox) dialogView.findViewById(R.id.confirm_dont_ask_again);

            TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
            confirmCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    edReceiveTo.setText("");
                    if(confirmDismissForever.isChecked())PrefsUtil.getInstance(getActivity()).setValue("WARN_WATCH_ONLY_SPEND", false);
                    alertDialog.dismiss();
                }
            });

            TextView confirmContinue = (TextView) dialogView.findViewById(R.id.confirm_continue);
            confirmContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    edReceiveTo.setText(((LegacyAddress) object).getAddress());
                    if(confirmDismissForever.isChecked())PrefsUtil.getInstance(getActivity()).setValue("WARN_WATCH_ONLY_SPEND", false);
                    alertDialog.dismiss();
                }
            });

            alertDialog.show();
        }
    }

    private String getV3ReceiveAddress(Account account) {

        try {
            int spinnerIndex = receiveToBiMap.get(account);
            int accountIndex = spinnerIndexAccountIndexMap.get(spinnerIndex);

            ReceiveAddress receiveAddress = null;
            receiveAddress = HDPayloadBridge.getInstance(getActivity()).getReceiveAddress(accountIndex);
            return receiveAddress.getAddress();

        } catch (DecoderException | IOException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException | AddressFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getDefaultDecimalSeparator(){
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    private void initVars(){

        edAmount1.addTextChangedListener(btcTextWatcher);

        edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
        edAmount2.setHint("0" + defaultSeparator + "00");
        edAmount2.addTextChangedListener(fiatTextWatcher);

        edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
        edAmount1.setHint("0" + defaultSeparator + "00");

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);
    }

    private void handleIncomingQRScan(){

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String address_arg = bundle.getString("btc_address", "");
            String amount_arg = bundle.getString("btc_amount", "");
            if (!address_arg.equals("")) {
                edReceiveTo.setText(address_arg);
            }
            if (!amount_arg.equals("")) {
                edAmount1.removeTextChangedListener(btcTextWatcher);
                edAmount2.removeTextChangedListener(fiatTextWatcher);

                edAmount1.setText(amount_arg);
                edAmount1.setSelection(edAmount1.getText().toString().length());

                double btc_amount = 0.0;
                try {
                    btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()));
                } catch (NumberFormatException e) {
                    btc_amount = 0.0;
                }

                // sanity check on strFiat, necessary if the result of a URI scan
                if (strFiat == null) {
                    strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
                }
                btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

                double fiat_amount = btc_fx * btc_amount;
                edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
//                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
                strBTC = MonetaryUtil.getInstance().getBTCUnit(MonetaryUtil.UNIT_BTC);
                tvCurrency1.setText(strBTC);
                tvFiat2.setText(strFiat);

                edAmount1.addTextChangedListener(btcTextWatcher);
                edAmount2.addTextChangedListener(fiatTextWatcher);
            }
        }
    }

    private void setDynamicFee(){

        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    //This call might take a while - so when a result is returned refresh the max available
                    JSONObject dynamicFeeJson = FeeUtil.getInstance().getDynamicFee();
                    if(dynamicFeeJson != null){

                        dynamicFee = BigInteger.valueOf(dynamicFeeJson.getLong("fee"));
                        isFeeAbnormallyHigh = dynamicFeeJson.getBoolean("surge");//We'll use this later to warn the user if the dynamic fee is too high
                        dynamicFeeFailed = false;
                    }else{
                        dynamicFee = SendCoins.bFee;
                        dynamicFeeFailed = true;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    dynamicFee = SendCoins.bFee;
                    dynamicFeeFailed = true;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayMaxAvailable();
                    }
                });

                return null;
            }
        }.execute();
    }

    private void setBtcTextWatcher(){

        btcTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {

                String input = s.toString();

                long lamount = 0L;
                try {
                    //Long is safe to use, but double can lead to ugly rounding issues..
                    lamount = (BigDecimal.valueOf(MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()))).multiply(BigDecimal.valueOf(100000000)).longValue());

                    if (BigInteger.valueOf(lamount).compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
                        ToastCustom.makeText(getActivity(), getActivity().getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        edAmount1.setText("0.0");
                        return;
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }
                //TODO: I feel this is a little bit hacky, but it will solve the problem of keeping the amount after a scan
                ((MainActivity) getActivity()).sendFragmentBitcoinAmountStorage = lamount;

                edAmount1.removeTextChangedListener(this);

                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
                switch (unit) {
                    case MonetaryUtil.MICRO_BTC:
                        max_len = 2;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        max_len = 4;
                        break;
                    default:
                        max_len = 8;
                        break;
                }
                btcFormat.setMaximumFractionDigits(max_len + 1);
                btcFormat.setMinimumFractionDigits(0);

                try {
                    if (input.indexOf(defaultSeparator) != -1) {
                        String dec = input.substring(input.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount1.setText(input.substring(0, input.length() - 1));
                                edAmount1.setSelection(edAmount1.getText().length());
                                s = edAmount1.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                edAmount1.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateFiatTextField(s.toString());
                    textChangeAllowed = true;
                }

                if (s.toString().contains(defaultSeparator))
                    edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

    }

    private void setFiatTextWatcher(){

        fiatTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {

                String input = s.toString();

                edAmount2.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                try {
                    if (input.indexOf(defaultSeparator) != -1) {
                        String dec = input.substring(input.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount2.setText(input.substring(0, input.length() - 1));
                                edAmount2.setSelection(edAmount2.getText().length());
                                s = edAmount2.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                edAmount2.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateBtcTextField(s.toString());
                    textChangeAllowed = true;
                }

                if (s.toString().contains(defaultSeparator))
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

    }

    /*
    Custom keypad implementation
    Numerous Samsung devices have keypad issues where decimal separators are absent.
     */
    private void decimalCompatCheck(View rootView) {

        edAmount1.removeTextChangedListener(btcTextWatcher);
        edAmount2.removeTextChangedListener(fiatTextWatcher);

        numpad = ((TableLayout) rootView.findViewById(R.id.numericPad));

        if (Build.MANUFACTURER.toLowerCase().contains("samsung")) {

            numpad.setVisibility(View.GONE);
            isKeypadVisible = false;
            edAmount1.setTextIsSelectable(true);
            edAmount1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        numpad.setVisibility(View.VISIBLE);
                        isKeypadVisible = true;
                    } else {
                        numpad.setVisibility(View.GONE);
                        isKeypadVisible = false;
                    }
                }
            });
            edAmount1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    numpad.setVisibility(View.VISIBLE);
                    isKeypadVisible = true;
                }
            });

            edAmount2.setTextIsSelectable(true);
            edAmount2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {

                    if (hasFocus) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        numpad.setVisibility(View.VISIBLE);
                        isKeypadVisible = true;
                    } else {
                        numpad.setVisibility(View.GONE);
                        isKeypadVisible = false;
                    }
                }
            });
            edAmount2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    numpad.setVisibility(View.VISIBLE);
                    isKeypadVisible = true;
                }
            });

            rootView.findViewById(R.id.button1).setOnClickListener(this);
            rootView.findViewById(R.id.button2).setOnClickListener(this);
            rootView.findViewById(R.id.button3).setOnClickListener(this);
            rootView.findViewById(R.id.button4).setOnClickListener(this);
            rootView.findViewById(R.id.button5).setOnClickListener(this);
            rootView.findViewById(R.id.button6).setOnClickListener(this);
            rootView.findViewById(R.id.button7).setOnClickListener(this);
            rootView.findViewById(R.id.button8).setOnClickListener(this);
            rootView.findViewById(R.id.button9).setOnClickListener(this);
            rootView.findViewById(R.id.button10).setOnClickListener(this);
            rootView.findViewById(R.id.button0).setOnClickListener(this);
            rootView.findViewById(R.id.buttonDeleteBack).setOnClickListener(this);
            rootView.findViewById(R.id.buttonDone).setOnClickListener(this);

        }

        edAmount1.addTextChangedListener(btcTextWatcher);
        edAmount2.addTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
            tvCurrency1.setText(strBTC);
            tvFiat2.setText(strFiat);
            displayMaxAvailable();
        } else {
            ;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity.currentFragment = this;

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);

        if (getArguments() != null)
            if (getArguments().getBoolean("incoming_from_scan", false)) {
                ;
            }

        selectDefaultAccount();

        setDynamicFee();

        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }

    private void selectDefaultAccount() {

        if (sendFromSpinner != null) {

            if (PayloadFactory.getInstance().get().isUpgraded()) {
                int defaultIndex = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
                Account defaultAccount = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(defaultIndex);
                int defaultSpinnerIndex = sendFromBiMap.get(defaultAccount);
                sendFromSpinner.setSelection(defaultSpinnerIndex);
            } else {
                //V2
                sendFromSpinner.setSelection(0);
            }
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateFiatTextField(String cBtc) {
        double btc_amount = 0.0;
        try {
            btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(NumberFormat.getInstance(Locale.getDefault()).parse(cBtc).doubleValue());
        } catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        } catch (ParseException pe) {
            btc_amount = 0.0;
        }
        double fiat_amount = btc_fx * btc_amount;
        edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
    }

    private void updateBtcTextField(String cfiat) {

        double fiat_amount = 0.0;
        try {
            fiat_amount = NumberFormat.getInstance(Locale.getDefault()).parse(cfiat).doubleValue();
        } catch (NumberFormatException | ParseException e) {
            fiat_amount = 0.0;
        }
        double btc_amount = fiat_amount / btc_fx;
        edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_amount)));
    }

    private void displayMaxAvailable() {

        long fee = dynamicFee.longValue();

        //User has customized the fee
        if(!etCustomFee.getText().toString().isEmpty()){
            fee = getLongValue(etCustomFee.getText().toString());
        }

        Object object = sendFromBiMap.inverse().get(sendFromSpinner.getSelectedItemPosition());//the current selected item in from dropdown (Account or Legacy Address)

        long balance = 0L;
        if(object instanceof Account) {
            //V3
            if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(((Account) object).getXpub())) {
                balance = MultiAddrFactory.getInstance().getXpubAmounts().get(((Account) object).getXpub());
            }
        }else{
            //V2
            balance = MultiAddrFactory.getInstance().getLegacyBalance(((LegacyAddress)object).getAddress());
        }

        //Subtract fee
        long balanceAfterFee = (balance - fee);

        if (balanceAfterFee > 0L) {
            double btc_balance = (((double) balanceAfterFee) / 1e8);
            tvMax.setText(getActivity().getResources().getText(R.string.max_available) + " " + MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_balance)) + " " + strBTC);
        } else {
            if (AppUtil.getInstance(getActivity()).isNotUpgraded()) {
                tvMax.setText(R.string.no_funds_available2);
            } else {
                tvMax.setText(R.string.no_funds_available);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_merchant_directory).setVisible(false);
        menu.findItem(R.id.action_qr).setVisible(true);
        menu.findItem(R.id.action_share_receive).setVisible(false);

        btSend = menu.findItem(R.id.action_send);
        btSend.setVisible(true);

        btSend.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                sendClicked();

                return false;
            }
        });
    }

    private void sendClicked() {

        //Hide keyboard
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        //Check connectivity before we spend
        if(ConnectivityStatus.hasConnectivity(getActivity())){

            //Get spend details from UI
            final PendingSpend pendingSpend = getPendingSpendFromUIFields();
            if(pendingSpend != null){
                if(isValidSpend(pendingSpend)){

                    //Currently only v2 has watch-only
                    if(!pendingSpend.isHD && pendingSpend.fromLegacyAddress.isWatchOnly()){
                        promptWatchOnlySpend(pendingSpend);
                    }else{
                        checkDoubleEncrypt(pendingSpend);
                    }
                }
            }

        }else{
            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
            ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
        }
    }

    private void promptWatchOnlySpend(final PendingSpend pendingSpend){

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.privx_required)
                .setMessage(getString(R.string.watch_only_spend_instructions).replace("[--address--]",pendingSpend.fromLegacyAddress.getAddress()))
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        watchOnlyPendingSpend = pendingSpend;

                        Intent intent = new Intent(getActivity(), CaptureActivity.class);
                        startActivityForResult(intent, SCAN_PRIVX);

                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK){
            final String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);

            try {
                final String format = PrivateKeyFactory.getInstance().getFormat(scanData);
                if (format != null) {

                    if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                        promptForSecondPassword(new OpSimpleCallback() {
                            @Override
                            public void onSuccess(String string) {
                                if (!format.equals(PrivateKeyFactory.BIP38)) {
                                    spendFromWatchOnlyNonBIP38(format, scanData);
                                } else {
                                    spendFromWatchOnlyBIP38(scanData);
                                }
                            }

                            @Override
                            public void onFail() {
                                ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                            }
                        });
                    }else{
                        if (!format.equals(PrivateKeyFactory.BIP38)) {
                            spendFromWatchOnlyNonBIP38(format, scanData);
                        } else {
                            spendFromWatchOnlyBIP38(scanData);
                        }
                    }

                } else {
                    ToastCustom.makeText(getActivity(), getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void promptForSecondPassword(final OpSimpleCallback callback){

        final EditText double_encrypt_password = new EditText(getActivity());
        double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_double_encryption_pw)
                .setView(double_encrypt_password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String secondPassword = double_encrypt_password.getText().toString();

                        if (secondPassword != null &&
                                secondPassword.length() > 0 &&
                                DoubleEncryptionFactory.getInstance().validateSecondPassword(
                                        PayloadFactory.getInstance().get().getDoublePasswordHash(),
                                        PayloadFactory.getInstance().get().getSharedKey(),
                                        new CharSequenceX(secondPassword), PayloadFactory.getInstance().get().getOptions().getIterations()) &&
                                !StringUtils.isEmpty(secondPassword)) {

                            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(secondPassword));
                            callback.onSuccess(secondPassword);

                        } else {
                            callback.onFail();
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void spendFromWatchOnlyNonBIP38(final String format, final String scanData){
        ECKey key = null;

        try {
            key = PrivateKeyFactory.getInstance().getKey(format, scanData);
        } catch (Exception e) {
            ToastCustom.makeText(getActivity(), getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            e.printStackTrace();
            return;
        }

        if (key != null && key.hasPrivKey() && watchOnlyPendingSpend.fromLegacyAddress.getAddress().equals(key.toAddress(MainNetParams.get()).toString())) {

            //Create copy, otherwise pass by ref will override
            LegacyAddress tempLegacyAddress = new LegacyAddress();
            if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
                String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
                tempLegacyAddress.setEncryptedKey(encrypted2);
            }else{
                tempLegacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            }
            tempLegacyAddress.setAddress(key.toAddress(MainNetParams.get()).toString());
            tempLegacyAddress.setLabel(watchOnlyPendingSpend.fromLegacyAddress.getLabel());

            watchOnlyPendingSpend.fromLegacyAddress = tempLegacyAddress;

            confirmPayment(watchOnlyPendingSpend);
        } else {
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void spendFromWatchOnlyBIP38(final String scanData){

        final EditText password = new EditText(getActivity());
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }
                        progress = new ProgressDialog(getActivity());
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                        progress.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                Looper.prepare();

                                try {
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), scanData);
                                    final ECKey key = bip38.decrypt(pw);

                                    if (key != null && key.hasPrivKey()) {

                                        if(watchOnlyPendingSpend.fromLegacyAddress.getAddress().equals(key.toAddress(MainNetParams.get()).toString())){
                                            //Create copy, otherwise pass by ref will override
                                            LegacyAddress tempLegacyAddress = new LegacyAddress();
                                            if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                                                String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
                                                String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
                                                tempLegacyAddress.setEncryptedKey(encrypted2);
                                            }else{
                                                tempLegacyAddress.setEncryptedKey(key.getPrivKeyBytes());
                                            }
                                            tempLegacyAddress.setAddress(key.toAddress(MainNetParams.get()).toString());
                                            tempLegacyAddress.setLabel(watchOnlyPendingSpend.fromLegacyAddress.getLabel());

                                            watchOnlyPendingSpend.fromLegacyAddress = tempLegacyAddress;

                                            confirmPayment(watchOnlyPendingSpend);
                                        }else{
                                            ToastCustom.makeText(getActivity(), getString(R.string.invalid_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                        }

                                    } else {
                                        ToastCustom.makeText(getActivity(), getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }

                                } catch (Exception e) {
                                    ToastCustom.makeText(getActivity(), getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } finally {
                                    if (progress != null && progress.isShowing()) {
                                        progress.dismiss();
                                        progress = null;
                                    }
                                }

                                Looper.loop();

                            }
                        }).start();

                    }
                }).setNegativeButton(R.string.cancel, null).show();

    }

    private void checkDoubleEncrypt(final PendingSpend pendingSpend){

        if (!PayloadFactory.getInstance().get().isDoubleEncrypted() || DoubleEncryptionFactory.getInstance().isActivated()) {
            confirmPayment(pendingSpend);
        } else {
            alertDoubleEncrypted(pendingSpend);
        }
    }

    private void alertDoubleEncrypted(final PendingSpend pendingSpend){
        final EditText password = new EditText(getActivity());
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_double_encryption_pw)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), new CharSequenceX(pw), PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations())) {

                            if (pendingSpend.isHD) {

                                String encrypted_hex = PayloadFactory.getInstance().get().getHdWallet().getSeedHex();
                                String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                                        encrypted_hex,
                                        PayloadFactory.getInstance().get().getSharedKey(),
                                        pw,
                                        PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations());

                                try {
                                    Wallet hdw = WalletFactory.getInstance().restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
                                    WalletFactory.getInstance().setWatchOnlyWallet(hdw);
                                } catch (IOException | DecoderException | AddressFormatException |
                                        MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException |
                                        MnemonicException.MnemonicWordException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));
                            }

                            confirmPayment(pendingSpend);

                        } else {
                            ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void confirmPayment(final PendingSpend pendingSpend) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.fragment_send_confirm, null);
                dialogBuilder.setView(dialogView);

                final AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(false);

                TextView confirmFrom = (TextView) dialogView.findViewById(R.id.confirm_from_label);

                if(pendingSpend.isHD) {
                    confirmFrom.setText(pendingSpend.fromAccount.getLabel());
                }else{
                    confirmFrom.setText(pendingSpend.fromLegacyAddress.getLabel());
                }

                TextView confirmDestination = (TextView) dialogView.findViewById(R.id.confirm_to_label);
                confirmDestination.setText(pendingSpend.destination);

                TextView tvAmountBtcUnit = (TextView) dialogView.findViewById(R.id.confirm_amount_btc_unit);
                tvAmountBtcUnit.setText(strBTC);
                TextView tvAmountFiatUnit = (TextView) dialogView.findViewById(R.id.confirm_amount_fiat_unit);
                tvAmountFiatUnit.setText(strFiat);

                //BTC
                TextView tvAmountBtc = (TextView) dialogView.findViewById(R.id.confirm_amount_btc);
                tvAmountBtc.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(pendingSpend.bigIntAmount.longValue()));

                final TextView tvFeeBtc = (TextView) dialogView.findViewById(R.id.confirm_fee_btc);
                if(isFeeAbnormallyHigh)tvFeeBtc.setTextColor(getResources().getColor(R.color.blockchain_send_red));
                tvFeeBtc.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(pendingSpend.bigIntFee.longValue()));

                TextView tvTotlaBtc = (TextView) dialogView.findViewById(R.id.confirm_total_btc);
                BigInteger totalBtc = (pendingSpend.bigIntAmount.add(pendingSpend.bigIntFee));
                tvTotlaBtc.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(totalBtc.longValue()));

                //Fiat
                btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
                String amountFiat = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (pendingSpend.bigIntAmount.doubleValue() / 1e8)));
                TextView tvAmountFiat = (TextView) dialogView.findViewById(R.id.confirm_amount_fiat);
                tvAmountFiat.setText(amountFiat);

                TextView tvFeeFiat = (TextView) dialogView.findViewById(R.id.confirm_fee_fiat);
                String feeFiat = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (pendingSpend.bigIntFee.doubleValue() / 1e8)));
                if(isFeeAbnormallyHigh)tvFeeFiat.setTextColor(getResources().getColor(R.color.blockchain_send_red));
                tvFeeFiat.setText(feeFiat);

                TextView tvTotalFiat = (TextView) dialogView.findViewById(R.id.confirm_total_fiat);
                BigInteger totalFiat = (pendingSpend.bigIntAmount.add(pendingSpend.bigIntFee));
                String totalFiatS = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (totalFiat.doubleValue() / 1e8)));
                tvTotalFiat.setText(totalFiatS);

                TextView tvCustomizeFee = (TextView) dialogView.findViewById(R.id.tv_customize_fee);
                tvCustomizeFee.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rootView.findViewById(R.id.custom_fee_container).setVisibility(View.VISIBLE);

                                String fee = MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(dynamicFee.doubleValue() / 1e8));

                                EditText etCustomFee = (EditText)rootView.findViewById(R.id.custom_fee);
                                etCustomFee.setText(fee);
                                etCustomFee.setHint(fee);
                                etCustomFee.requestFocus();
                                etCustomFee.setSelection(etCustomFee.getText().length());

                                //If dynamic fee service is failing, don't display 'recommended'
                                if(!dynamicFeeFailed)rootView.findViewById(R.id.tv_recommended).setVisibility(View.VISIBLE);
                            }
                        });

                        //If dynamic fee service is failing, don't display 'recommended' prompt
                        if(!dynamicFeeFailed)
                            alertCustomSpend();

                    }
                });

                ImageView confirmCancel = (ImageView) dialogView.findViewById(R.id.confirm_cancel);
                confirmCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                    }
                });

                TextView confirmSend = (TextView) dialogView.findViewById(R.id.confirm_send);
                confirmSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (!spendInProgress) {
                            spendInProgress = true;

                            if (progress != null && progress.isShowing()) {
                                progress.dismiss();
                                progress = null;
                            }
                            progress = new ProgressDialog(getActivity());
                            progress.setCancelable(false);
                            progress.setTitle(R.string.app_name);
                            progress.setMessage(getString(R.string.sending));
                            progress.show();

                            context = getActivity();

                            final UnspentOutputsBundle unspents = SendFactory.getInstance(context).prepareSend(pendingSpend.fromXpubIndex, pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, null);
                            //TODO - unspents.getRecommendedFee() = fee per kb?

                            if (unspents != null) {
                                executeSend(pendingSpend, unspents, alertDialog);
                            } else {

                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                ToastCustom.makeText(context.getApplicationContext(), getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                closeDialog(alertDialog, false);
                            }

                            spendInProgress = false;
                        }
                    }
                });

                alertDialog.show();

                Looper.loop();

            }
        }).start();
    }

    private void alertCustomSpend(){

        String message = getResources().getString(R.string.recommended_fee)
                +"\n\n"
                +MonetaryUtil.getInstance(getActivity()).getDisplayAmount(dynamicFee.longValue())
                +" "+strBTC;

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.transaction_fee)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null).show();
    }

    private void executeSend(final PendingSpend pendingSpend, final UnspentOutputsBundle unspents, final AlertDialog alertDialog){

        SendFactory.getInstance(context).execSend(pendingSpend.fromXpubIndex, unspents.getOutputs(), pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, pendingSpend.note, false, new OpCallback() {

            public void onSuccess() {
            }

            @Override
            public void onSuccess(final String hash) {

                ToastCustom.makeText(context, getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                playAudio();

                if (pendingSpend.isHD) {

                    //Update v3 balance immediately after spend - until refresh from server
                    MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                    MultiAddrFactory.getInstance().setXpubAmount(HDPayloadBridge.getInstance(context).account2Xpub(pendingSpend.fromXpubIndex), MultiAddrFactory.getInstance().getXpubAmounts().get(HDPayloadBridge.getInstance(context).account2Xpub(pendingSpend.fromXpubIndex)) - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));

                } else {

                    //Update v2 balance immediately after spend - until refresh from server
                    MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                    //TODO - why are we not setting individual address balance as well, was this over looked?

                    //Reset double encrypt for V2
                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                }

                PayloadBridge.getInstance(context).remoteSaveThread();
                closeDialog(alertDialog, true);
            }

            public void onFail() {

                ToastCustom.makeText(context, getResources().getString(R.string.transaction_queued), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

                //Initial send failed - Put send in queue for reattempt
                String direction = MultiAddrFactory.SENT;
                if (spDestinationSelected) direction = MultiAddrFactory.MOVED;

                SendFactory.getInstance(context).execSend(pendingSpend.fromXpubIndex, unspents.getOutputs(), pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, pendingSpend.note, true, this);

                //Refresh BalanceFragment with the following - "placeholder" tx until websocket refreshes list
                final Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
                Bundle bundle = new Bundle();
                bundle.putLong("queued_bamount", (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                bundle.putString("queued_strNote", pendingSpend.note);
                bundle.putString("queued_direction", direction);
                bundle.putLong("queued_time", System.currentTimeMillis() / 1000);
                intent.putExtras(bundle);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }//wait for broadcast receiver to register
                        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
                        Looper.loop();
                    }
                }).start();

                //Reset double encrypt for V2
                if (!pendingSpend.isHD) {
                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                }

                closeDialog(alertDialog, true);
            }

            @Override
            public void onFailPermanently() {

                //Reset double encrypt for V2
                if (!pendingSpend.isHD) {
                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                }

                if (getActivity() != null)
                    ToastCustom.makeText(getActivity().getApplicationContext(), getActivity().getApplicationContext().getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                closeDialog(alertDialog, false);
            }

        });
    }

    private void closeDialog(AlertDialog alertDialog, boolean sendSuccess) {

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
        }

        if (sendSuccess) {
            Fragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    private void playAudio(){

        AudioManager audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(context.getApplicationContext(), R.raw.alert);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mp.release();
                }

            });
            mp.start();
        }
    }

    private PendingSpend getPendingSpendFromUIFields(){

        PendingSpend pendingSpend = new PendingSpend();

        //Check if fields are parsable
        if ((edAmount1.getText() == null || edAmount1.getText().toString().isEmpty()) ||
                (edAmount2.getText() == null || edAmount2.getText().toString().isEmpty())){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return null;
        }
        if(edReceiveTo.getText() == null || edReceiveTo.getText().toString().isEmpty()){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return null;
        }

        //is V3?
        Object object = sendFromBiMap.inverse().get(sendFromSpinner.getSelectedItemPosition());

        if(object instanceof LegacyAddress){
            //V2
            pendingSpend.isHD = false;
            pendingSpend.fromLegacyAddress = (LegacyAddress) object;
            pendingSpend.fromXpubIndex = -1;//V2, xpub index must be -1
        }else{
            //V3
            int spinnerIndex = sendFromBiMap.get(object);
            int accountIndex = spinnerIndexAccountIndexMap.get(spinnerIndex);

            pendingSpend.isHD = true;
            pendingSpend.fromAccount = (Account) object;
            pendingSpend.fromXpubIndex = accountIndex;//TODO - get rid of this xpub index
            pendingSpend.fromLegacyAddress = null;//V3, legacy address must be null
        }

        //Amount to send
        pendingSpend.bigIntAmount = BigInteger.valueOf(getLongValue(edAmount1.getText().toString()));

        //Fee
        if(etCustomFee.getText() != null && !etCustomFee.getText().toString().isEmpty()){
            //User customized fee
            pendingSpend.bigIntFee = BigInteger.valueOf(getLongValue(etCustomFee.getText().toString()));
        }else{
            pendingSpend.bigIntFee = dynamicFee;
        }

        //Destination
        pendingSpend.destination = edReceiveTo.getText().toString().trim();

        //Note
        pendingSpend.note = null;//future use

        return pendingSpend;
    }

    private boolean isValidSpend(PendingSpend pendingSpend) {

        //Validate amount
        if(!isValidAmount(pendingSpend.bigIntAmount)){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        //Validate sufficient fee
        if(pendingSpend.bigIntFee.compareTo(SendCoins.bDust) <= 0){
            ToastCustom.makeText(getActivity(), getString(R.string.insufficient_fee), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        //Validate sufficient funds
        long amountToSendIncludingFee = pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue();
        if(pendingSpend.isHD){
            String xpub = pendingSpend.fromAccount.getXpub();
            if(!hasSufficientFunds(xpub, null, amountToSendIncludingFee)){
                ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }else{
            if(!hasSufficientFunds(null, pendingSpend.fromLegacyAddress.getAddress(), amountToSendIncludingFee)){
                ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }

        //Validate addresses
        if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        //Validate send and receive not same addresses
        if((pendingSpend.isHD && getV3ReceiveAddress(pendingSpend.fromAccount).equals(pendingSpend.destination)) ||
                (!pendingSpend.isHD && pendingSpend.fromLegacyAddress.getAddress().equals(pendingSpend.destination))){
            ToastCustom.makeText(getActivity(), getString(R.string.send_to_same_address_warning), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        return true;
    }

    private long getLongValue(String amountToSendString){

        String amountToSend = amountToSendString.replace(" ","").replace(defaultSeparator, ".");
        return (BigDecimal.valueOf(MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(amountToSend))).multiply(BigDecimal.valueOf(100000000)).longValue());
    }

    private boolean isValidAmount(BigInteger bAmount){

        //Test that amount is more than dust
        if (bAmount.compareTo(SendCoins.bDust) == -1) {
            return false;
        }

        //Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
            edAmount1.setText("0");
            return false;
        }

        //Test that amount is not zero
        if (!(bAmount.compareTo(BigInteger.ZERO) >= 0)) {
            return false;
        }

        return true;
    }

    private boolean hasSufficientFunds(String xpub, String legacyAddress, long amountToSendIncludingFee){

        if (xpub != null) {
            //HD
            if (xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long xpubBalance = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if ((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(amountToSendIncludingFee).longValue()) > xpubBalance) {
                    return false;
                }
            }
        } else {
            //Legacy
            long legacyAddressBalance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress);
            if ((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(amountToSendIncludingFee).longValue()) > legacyAddressBalance) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onClick(View v) {

        String pad = "";
        switch (v.getId()) {
            case R.id.button1:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button2:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button3:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button4:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button5:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button6:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button7:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button8:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button9:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button10:
                pad = defaultSeparator;
                break;
            case R.id.button0:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.buttonDeleteBack:
                pad = null;
                break;
            case R.id.buttonDone:
                numpad.setVisibility(View.GONE);
                break;
        }

        if (pad != null) {
            // Append tapped #
            if (edAmount1.hasFocus()) {
                edAmount1.append(pad);
            } else if (edAmount2.hasFocus()) {
                edAmount2.append(pad);
            }
        } else {
            // Back clicked
            if (edAmount1.hasFocus()) {
                String e1 = edAmount1.getText().toString();
                if (e1.length() > 0) edAmount1.setText(e1.substring(0, e1.length() - 1));
            } else if (edAmount2.hasFocus()) {
                String e2 = edAmount2.getText().toString();
                if (e2.length() > 0) edAmount2.setText(e2.substring(0, e2.length() - 1));
            }
        }

        if (edAmount1.hasFocus() && edAmount1.getText().length() > 0) {
            edAmount1.post(new Runnable() {
                @Override
                public void run() {
                    edAmount1.setSelection(edAmount1.getText().toString().length());
                }
            });
        } else if (edAmount2.hasFocus() && edAmount2.getText().length() > 0) {
            edAmount2.post(new Runnable() {
                @Override
                public void run() {
                    edAmount2.setSelection(edAmount2.getText().toString().length());
                }
            });
        }
    }

    @Override
    public void onKeypadClose() {
        numpad.setVisibility(View.GONE);
        isKeypadVisible = false;
    }

    class SendFromAdapter extends ArrayAdapter<String> {

        public SendFromAdapter(Context context, int textViewResourceId, List<String> accounts) {
            super(context, textViewResourceId, accounts);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, false);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent, boolean isDropdown) {

            LayoutInflater inflater = getActivity().getLayoutInflater();

            int layoutRes = R.layout.spinner_item;
            if (isDropdown) layoutRes = R.layout.fragment_send_account_row_dropdown;

            View row = inflater.inflate(layoutRes, parent, false);

            TextView label = null;
            TextView balance = null;
            if (isDropdown) {
                label = (TextView) row.findViewById(R.id.receive_account_label);
                balance = (TextView) row.findViewById(R.id.receive_account_balance);
            } else
                label = (TextView) row.findViewById(R.id.text);

            String labelText = "";
            long amount = 0L;

            Object object = sendFromBiMap.inverse().get(position);

            if(object instanceof LegacyAddress){
                //V2
                LegacyAddress legacyAddress = ((LegacyAddress) object);
                if(legacyAddress.isWatchOnly())labelText = getActivity().getString(R.string.watch_only_label);
                if (legacyAddress.getLabel() != null && legacyAddress.getLabel().length() > 0) {
                    labelText += legacyAddress.getLabel();
                } else {
                    labelText += legacyAddress.getAddress();
                }

                amount = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());

            }else{
                //V3
                Account account = ((Account)object);
                labelText = account.getLabel();

                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(account.getXpub())) {
                    amount = MultiAddrFactory.getInstance().getXpubAmounts().get(account.getXpub());
                }
            }

            if (isDropdown) {
                balance.setText("(" + MonetaryUtil.getInstance(getActivity()).getDisplayAmount(amount) + " " + strBTC + ")");
                label.setText(labelText);
            } else
                label.setText(labelText);

            return row;
        }
    }

    class ReceiveToAdapter extends ArrayAdapter<String> {

        public ReceiveToAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, false);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent, boolean isDropdown) {

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View row = null;
            if (isDropdown) {
                int layoutRes = R.layout.fragment_send_account_row_dropdown;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.receive_account_label);
                label.setText(receiveToList.get(position));
            } else {
                int layoutRes = R.layout.spinner_item;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.text);
                label.setText("");
            }

            return row;
        }
    }
}