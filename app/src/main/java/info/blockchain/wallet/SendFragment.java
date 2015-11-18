package info.blockchain.wallet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ReselectSpinner;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.TypefaceUtil;
import piuk.blockchain.android.R;

//import android.util.Log;

public class SendFragment extends Fragment implements View.OnClickListener, CustomKeypadCallback {

    private Locale locale = null;

    private EditText edDestination = null;
    private EditText edAmount1 = null;
    private TextView tvCurrency1 = null;
    private EditText edAmount2 = null;
    private TextView tvFiat2 = null;
    private Spinner spAccounts = null;
    private ReselectSpinner spDestination = null;
    private MenuItem btSend;
    private TextView tvMax = null;

    private TextWatcher btcTextWatcher = null;
    private TextWatcher fiatTextWatcher = null;

    private String currentSelectedFromAddress = null;
    private String currentSelectedToAddress = null;

    private String strBTC = "BTC";
    private String strFiat = null;
    private boolean isBTC = true;
    private double btc_fx = 319.13;

    private boolean textChangeAllowed = true;
    private String defaultSeparator;//Decimal separator based on locale

    private boolean spendInProgress = false;
    private List<String> _accounts = null;
    private boolean spDestinationSelected = false;
    private TableLayout numpad;
    public static boolean isKeypadVisible = false;

    private ProgressDialog progress = null;

    private static Context context = null;

    private View rootView;

    private class PendingSpend {
        boolean isHD;
        String amount;
        BigInteger bamount;
        BigInteger bfee;
        String destination;
        String sending_from;
        String btc_amount;
        String fiat_amount;
        String btc_units;
    };

    private PendingSpend pendingSpend = new PendingSpend();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        rootView.setFilterTouchesWhenObscured(true);

        locale = Locale.getDefault();

        ((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((ActionBarActivity)getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.send_bitcoin);
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

                //Default to 'Total Funds' for lame mode
                if(AppUtil.getInstance(getActivity()).isLegacy())
                    AccountsUtil.getInstance(getActivity()).setCurrentSpinnerIndex(0);

                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
        });

        edDestination = ((EditText)rootView.findViewById(R.id.destination));
        edDestination.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.NORMAL);
        edDestination.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    validateSpend(true, spAccounts.getSelectedItemPosition());
                }

                return false;
            }
        });
        edDestination.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                if (edAmount1 != null && edDestination != null && edAmount2 != null && spAccounts != null) {
                    currentSelectedToAddress = edDestination.getText().toString();
                    validateSpend(false, spAccounts.getSelectedItemPosition());
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
        defaultSeparator =Character.toString(symbols.getDecimalSeparator());

        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
        edAmount1.setHint("0" + defaultSeparator + "00");
        edAmount1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.NORMAL);
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    validateSpend(true, spAccounts.getSelectedItemPosition());
                }

                return false;
            }
        });

        btcTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {

                long lamount = 0L;
                try {
                    //Long is safe to use, but double can lead to ugly rounding issues..
                    lamount = ( BigDecimal.valueOf(MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()))).multiply(BigDecimal.valueOf(100000000)).longValue());

                    if(BigInteger.valueOf(lamount).compareTo(BigInteger.valueOf(2100000000000000L)) == 1)    {
                        ToastCustom.makeText(getActivity(), getActivity().getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        edAmount1.setText("0.0");
                        pendingSpend.bamount = BigInteger.ZERO;
                        return;
                    }
                }
                catch(NumberFormatException nfe) {
                    ;
                }
                //TODO: I feel this is a little bit hacky, but it will solve the problem of keeping the amount after a scan
                ((MainActivity)getActivity()).sendFragmentBitcoinAmountStorage = lamount;

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
                    double d = Double.parseDouble(s.toString());
                    String s1 = btcFormat.format(d);
                    if (s1.indexOf(defaultSeparator) != -1) {
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount1.setText(s1.substring(0, s1.length() - 1));
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

                    if (edAmount1 != null && edDestination != null && edAmount2 != null && spAccounts != null) {
                        currentSelectedToAddress = edDestination.getText().toString();
                        validateSpend(false, spAccounts.getSelectedItemPosition());
                    }
                    textChangeAllowed = true;
                }

                if(s.toString().contains(defaultSeparator))
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

        edAmount1.addTextChangedListener(btcTextWatcher);

        tvCurrency1 = (TextView)rootView.findViewById(R.id.currency1);
        edAmount2 = (EditText)rootView.findViewById(R.id.amount2);
        edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"+ defaultSeparator));
        edAmount2.setHint("0" + defaultSeparator + "00");
        tvFiat2 = (TextView)rootView.findViewById(R.id.fiat2);
        fiatTextWatcher = new TextWatcher()	{
            public void afterTextChanged(Editable s) {

                edAmount2.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                try	{
                    double d = Double.parseDouble(s.toString());
                    String s1 = fiatFormat.format(d);
                    if(s1.indexOf(defaultSeparator) != -1)	{
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if(dec.length() > 0)	{
                            dec = dec.substring(1);
                            if(dec.length() > max_len)	{
                                edAmount2.setText(s1.substring(0, s1.length() - 1));
                                edAmount2.setSelection(edAmount2.getText().length());
                                s = edAmount2.getEditableText();
                            }
                        }
                    }
                }
                catch(NumberFormatException nfe)	{
                    ;
                }

                edAmount2.addTextChangedListener(this);

                if(textChangeAllowed) {
                    textChangeAllowed = false;
                    updateBtcTextField(s.toString());

                    if(edAmount1 != null && edDestination != null && edAmount2 != null && spAccounts != null) {
                        currentSelectedToAddress = edDestination.getText().toString();
                        validateSpend(false, spAccounts.getSelectedItemPosition());
                    }
                    textChangeAllowed = true;
                }

                if(s.toString().contains(defaultSeparator))
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }

            public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        };
        edAmount2.addTextChangedListener(fiatTextWatcher);

        spAccounts = (Spinner)rootView.findViewById(R.id.accounts);

        _accounts = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountList();

        if(_accounts.size()==1)rootView.findViewById(R.id.from_row).setVisibility(View.GONE);

        AccountAdapter dataAdapter = new AccountAdapter(getActivity(), R.layout.spinner_item, _accounts);
        spAccounts.setAdapter(dataAdapter);
        spAccounts.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spAccounts.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spAccounts.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spAccounts.setDropDownWidth(spAccounts.getWidth());
            }
        });

        spAccounts.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        int position = spAccounts.getSelectedItemPosition();
                        AccountsUtil.getInstance(getActivity()).setCurrentSpinnerIndex(position + 1);//all account included

                        if(AppUtil.getInstance(getActivity()).isLegacy())    {

                            List<LegacyAddress> legacy = PayloadFactory.getInstance().get().getLegacyAddresses();
                            for(int i = 0; i < legacy.size(); i++) {
                                if(legacy.get(i).getTag() == PayloadFactory.NORMAL_ADDRESS) {
                                    currentSelectedFromAddress = legacy.get(i).getAddress();
                                    break;
                                }
                            }

                        }
                        else    {
                            if (position >= AccountsUtil.getInstance(getActivity()).getLastHDIndex()) {

                                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - AccountsUtil.getInstance(getActivity()).getLastHDIndex());

                                if (legacyAddress.getTag() == PayloadFactory.WATCHONLY_ADDRESS) {
                                    spAccounts.setSelection(0);
                                    ToastCustom.makeText(getActivity(), getString(R.string.watchonly_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                    return;
                                } else if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                                    spAccounts.setSelection(0);
                                    ToastCustom.makeText(getActivity(), getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                    return;
                                } else {
                                    currentSelectedFromAddress = legacyAddress.getAddress();
                                }

                            }
                        }

                        displayMaxAvailable();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                }
        );

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);

        tvMax = (TextView)rootView.findViewById(R.id.max);
        tvMax.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());

        DestinationAdapter destinationAdapter = new DestinationAdapter(getActivity(), R.layout.spinner_item, _accounts);
        destinationAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spDestination = (ReselectSpinner)rootView.findViewById(R.id.sp_destination);
        if(_accounts.size() <= 1)
            spDestination.setVisibility(View.GONE);
        spDestination.setAdapter(destinationAdapter);
        spDestination.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spDestination.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spDestination.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spDestination.setDropDownWidth(spAccounts.getWidth());

                if(AppUtil.getInstance(getActivity()).isLegacy())    {
                    LinearLayout fromRow = (LinearLayout)rootView.findViewById(R.id.from_row);
                    fromRow.setVisibility(View.GONE);
                }
            }
        });

        spDestination.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        int position = spDestination.getSelectedItemPosition();
                        spDestination.getSelectedItem().toString();

                        if (position >= AccountsUtil.getInstance(getActivity()).getLastHDIndex()) {
                            //Legacy addresses
                            LegacyAddress account = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - AccountsUtil.getInstance(getActivity()).getLastHDIndex());

                            if(account.getTag() == PayloadFactory.WATCHONLY_ADDRESS)    {
                                edDestination.setText("");
                                ToastCustom.makeText(getActivity(), getString(R.string.watchonly_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            }
                            else if(account.getTag() == PayloadFactory.ARCHIVED_ADDRESS)   {
                                edDestination.setText("");
                                ToastCustom.makeText(getActivity(), getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            }
                            else    {
                                currentSelectedToAddress = account.getAddress();
                            }

                        } else {
                            //hd accounts
                            Integer currentSelectedAccount = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position);
                            try {
                                ReceiveAddress currentSelectedReceiveAddress = HDPayloadBridge.getInstance(getActivity()).getReceiveAddress(currentSelectedAccount);
                                currentSelectedToAddress = currentSelectedReceiveAddress.getAddress();

                            } catch (IOException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                                    | MnemonicException.MnemonicWordException | AddressFormatException
                                    | DecoderException e) {
                                e.printStackTrace();
                            }
                        }

//                        edDestination.setText(label);//future use
                        edDestination.setText(currentSelectedToAddress);
                        spDestinationSelected = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        // get bundle
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            boolean validate = false;
            String address_arg = bundle.getString("btc_address", "");
            String amount_arg = bundle.getString("btc_amount", "");
            if(!address_arg.equals("")) {
                edDestination.setText(address_arg);
                validate = false;
            }
            if(!amount_arg.equals("")) {
                edAmount1.removeTextChangedListener(btcTextWatcher);
                edAmount2.removeTextChangedListener(fiatTextWatcher);

                edAmount1.setText(amount_arg);
                edAmount1.setSelection(edAmount1.getText().toString().length());

                double btc_amount = 0.0;
                try {
                    btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()));
                }
                catch(NumberFormatException e) {
                    btc_amount = 0.0;
                }

                // sanity check on strFiat, necessary if the result of a URI scan
                if(strFiat == null) {
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

                validate = true;
            }
            if(validate) {
                int currentSelected = AccountsUtil.getInstance(getActivity()).getCurrentSpinnerIndex();
                if(currentSelected!=0)currentSelected--;//exclude 'all account'
                validateSpend(true, currentSelected);
            }
        }

        edAmount1.removeTextChangedListener(btcTextWatcher);
        edAmount2.removeTextChangedListener(fiatTextWatcher);
        decimalCompatCheck(rootView);
        edAmount1.addTextChangedListener(btcTextWatcher);
        edAmount2.addTextChangedListener(fiatTextWatcher);

        return rootView;
    }

    /*
    Custom keypad implementation
    Numerous Samsung devices have keypad issues where decimal separators are absent.
     */
    private void decimalCompatCheck(View rootView){

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
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
            tvCurrency1.setText(isBTC ? strBTC : strFiat);
            tvFiat2.setText(isBTC ? strFiat : strBTC);
            displayMaxAvailable();
        }
        else {
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
        tvCurrency1.setText(isBTC ? strBTC : strFiat);
        tvFiat2.setText(isBTC ? strFiat : strBTC);

        if(getArguments()!=null)
            if(getArguments().getBoolean("incoming_from_scan", false)) {
                ;
            }

        if(spAccounts != null) {

            int currentSelected = AccountsUtil.getInstance(getActivity()).getCurrentSpinnerIndex();
            if(currentSelected!=0)currentSelected--;//exclude 'all account'
            spAccounts.setSelection(currentSelected);
        }

        displayMaxAvailable();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void validateSpend(boolean showMessages, int position) {

        if(edAmount1.getText() == null || edAmount1.getText().toString().isEmpty()) return;
        if(edAmount2.getText() == null || edAmount2.getText().toString().isEmpty()) return;
        
        pendingSpend.amount = null;
        pendingSpend.destination = null;
        pendingSpend.bamount = BigInteger.ZERO;
        pendingSpend.bfee = BigInteger.valueOf(PayloadFactory.getInstance().get().getOptions().getFeePerKB());
        pendingSpend.isHD = true;
        pendingSpend.btc_units = strBTC;



        int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();

        //Legacy addresses
        if(position >= hdAccountsIdx) {
            LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
            currentSelectedFromAddress = legacyAddress.getAddress();
            if(legacyAddress.getLabel() != null && legacyAddress.getLabel().length() > 0) {
                pendingSpend.sending_from = legacyAddress.getLabel();
            }
            else {
                pendingSpend.sending_from = legacyAddress.getAddress();
            }
            pendingSpend.isHD = false;
        }
        else {

            //HD accounts
            Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
            pendingSpend.sending_from = hda.getLabel();
            pendingSpend.isHD = true;
        }

        pendingSpend.destination = currentSelectedToAddress;
        if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)) {
            if(showMessages) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            return;
        }

        if(isBTC) {
            pendingSpend.amount = edAmount1.getText().toString();
        }
        else {
            pendingSpend.amount = edAmount2.getText().toString();
        }
        long lamount = 0L;
        try {
            //Long is safe to use, but double can lead to ugly rounding issues..
            lamount = ( BigDecimal.valueOf(MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()))).multiply(BigDecimal.valueOf(100000000)).longValue());

            pendingSpend.bamount = BigInteger.valueOf(lamount);
            if(pendingSpend.bamount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1)    {
                ToastCustom.makeText(getActivity(), getActivity().getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                edAmount1.setText("0.0");
                pendingSpend.bamount = BigInteger.ZERO;
                return;
            }
            if(!(pendingSpend.bamount.compareTo(BigInteger.ZERO) >= 0)) {
                if(showMessages) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return;
            }
        }
        catch(NumberFormatException nfe) {
            if(showMessages) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            return;
        }

        if(pendingSpend.isHD) {
            String xpub = HDPayloadBridge.getInstance().account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));

            if(xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long _lamount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendCoins.bFee.longValue()) > _lamount) {
                    if(showMessages) {
                        ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                    return;
                }
            }
        }
        else {
            long _lamount = MultiAddrFactory.getInstance().getLegacyBalance(currentSelectedFromAddress);
            if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendCoins.bFee.longValue()) > _lamount) {
                if(showMessages) {
                    ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return;
            }
        }

    }

    private void updateFiatTextField(String cBtc) {
        double btc_amount = 0.0;
        try {
            btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(NumberFormat.getInstance(locale).parse(cBtc).doubleValue());
        }
        catch(NumberFormatException nfe) {
            btc_amount = 0.0;
        }
        catch(ParseException pe) {
            btc_amount = 0.0;
        }
        double fiat_amount = btc_fx * btc_amount;
        edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
    }

    private void updateBtcTextField(String cfiat){

        double fiat_amount = 0.0;
        try {
            fiat_amount = NumberFormat.getInstance(locale).parse(cfiat).doubleValue();
        }
        catch(NumberFormatException | ParseException e) {
            fiat_amount = 0.0;
        }
        double btc_amount = fiat_amount / btc_fx;
        edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_amount)));
    }

    private void displayMaxAvailable() {

        int position = spAccounts.getSelectedItemPosition();
        long amount = 0L;
        int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();
        if(position >= hdAccountsIdx) {
            if(AppUtil.getInstance(getActivity()).isLegacy())    {
                amount = MultiAddrFactory.getInstance().getLegacyBalance(PayloadFactory.NORMAL_ADDRESS);
            }
            else    {
                amount = MultiAddrFactory.getInstance().getLegacyBalance(AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx).getAddress());
            }
        }
        else {
            String xpub = account2Xpub(position);
            if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
            }
            else {
                amount = 0L;
            }
        }

        long amount_available = amount - SendCoins.bFee.longValue();
        if(amount_available > 0L) {
            double btc_balance = (((double)amount_available) / 1e8);
            tvMax.setText(getActivity().getResources().getText(R.string.max_available) + " " + MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_balance)) + " " + strBTC);
        }
        else {
            if(AppUtil.getInstance(getActivity()).isLegacy())    {
                tvMax.setText(R.string.no_funds_available2);
            }
            else    {
                tvMax.setText(R.string.no_funds_available);
            }
        }

    }

    public String account2Xpub(int sel) {

        Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        String xpub = null;
        if(hda instanceof ImportedAccount) {
            xpub = null;
        }
        else {
            xpub = HDPayloadBridge.getInstance(getActivity()).account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        }

        return xpub;
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

    private void sendClicked(){

        if(isBTC) {
            pendingSpend.btc_amount = edAmount1.getText().toString().replace(defaultSeparator,".");
            pendingSpend.fiat_amount = edAmount2.getText().toString().replace(defaultSeparator, ".");
        }
        else {
            pendingSpend.fiat_amount = edAmount1.getText().toString().replace(defaultSeparator, ".");
            pendingSpend.btc_amount = edAmount2.getText().toString().replace(defaultSeparator,".");
        }

        pendingSpend.btc_units = strBTC;

        double amt = 0.0;
        try {
            amt = Double.parseDouble(pendingSpend.btc_amount);
        }
        catch(NumberFormatException nfe) {
            ;
        }
        if(amt == 0.0) {
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        if(pendingSpend.isHD) {

            if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {

                confirmPayment(true, AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()), null);
            }
            else if(DoubleEncryptionFactory.getInstance().isActivated()) {

                confirmPayment(true, AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()), null);
            }
            else {

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

                                if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), new CharSequenceX(pw), PayloadFactory.getInstance().get().getIterations())) {

                                    String encrypted_hex = PayloadFactory.getInstance().get().getHdWallet().getSeedHex();
                                    String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                                            encrypted_hex,
                                            PayloadFactory.getInstance().get().getSharedKey(),
                                            pw,
                                            PayloadFactory.getInstance().get().getIterations());

                                    try {
                                        Wallet hdw = WalletFactory.getInstance().restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
                                        WalletFactory.getInstance().setWatchOnlyWallet(hdw);
                                    } catch (IOException | DecoderException | AddressFormatException |
                                            MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException |
                                            MnemonicException.MnemonicWordException e) {
                                        e.printStackTrace();
                                    } finally {
                                        ;
                                    }

                                    confirmPayment(true, AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()), null);
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
        }
        else {
            LegacyAddress addr = null;
//            List<LegacyAddress> legacy = AccountsUtil.getInstance(getActivity()).getLegacyAddresses();
            List<LegacyAddress> legacy = PayloadFactory.getInstance().get().getLegacyAddresses();
            for(int i = 0; i < legacy.size(); i++) {
                if(legacy.get(i).getAddress().equals(currentSelectedFromAddress)) {
                    addr = legacy.get(i);
                    break;
                }
            }

            if(addr.getTag() != PayloadFactory.NORMAL_ADDRESS)    {
                for(int i = 0; i < legacy.size(); i++) {
                    if(legacy.get(i).getTag() == PayloadFactory.NORMAL_ADDRESS) {
                        addr = legacy.get(i);
                        break;
                    }
                }
            }

            if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {

                confirmPayment(false, -1, addr);
            }
            else if(DoubleEncryptionFactory.getInstance().isActivated()) {

                confirmPayment(false, -1, addr);
            }
            else {

                final LegacyAddress legacyAddress = addr;
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

                                if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), new CharSequenceX(pw), PayloadFactory.getInstance().get().getIterations())) {
                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));

                                    confirmPayment(false, -1, legacyAddress);
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

        }

    }

    class AccountAdapter extends ArrayAdapter<String>{

        public AccountAdapter(Context context, int textViewResourceId, List<String> accounts) {
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
            if(isDropdown)layoutRes = R.layout.fragment_send_account_row_dropdown;

            View row =  inflater.inflate(layoutRes, parent, false);

            TextView label = null;
            TextView balance = null;
            if(isDropdown) {
                label = (TextView) row.findViewById(R.id.receive_account_label);
                balance = (TextView) row.findViewById(R.id.receive_account_balance);
            }else
                label = (TextView) row.findViewById(R.id.text);

            String labelText = "";
            long amount = 0L;
            int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();
            if(position >= hdAccountsIdx) {
                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
                if(legacyAddress.getLabel() != null && legacyAddress.getLabel().length() > 0) {
                    labelText = legacyAddress.getLabel();
                }
                else {
                    labelText = legacyAddress.getAddress();
                }
            }
            else {
                Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
                labelText = hda.getLabel();
            }

            if(position >= hdAccountsIdx) {
                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
                amount = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
            }
            else {
                Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
                String xpub = account2Xpub(position);
                if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                }
                else {
                    amount = 0L;
                }
            }

            if(isDropdown) {
                balance.setText("(" + MonetaryUtil.getInstance(getActivity()).getDisplayAmount(amount) + " " + strBTC + ")");
                label.setText(labelText);
            }else
                label.setText(labelText);

            return row;
        }
    }

    private void confirmPayment(final boolean isHd, final int currentAcc, final LegacyAddress legacyAddress){

        if(ConnectivityStatus.hasConnectivity(getActivity()))    {

            if(isValidSpend()) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Looper.prepare();

                        final UnspentOutputsBundle unspents = SendFactory.getInstance(getActivity()).prepareSend(isHd ? currentAcc : -1, pendingSpend.destination, pendingSpend.bamount, legacyAddress == null ? null : legacyAddress, BigInteger.ZERO, null);

                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.fragment_send_confirm, null);
                        dialogBuilder.setView(dialogView);

                        final AlertDialog alertDialog = dialogBuilder.create();
                        alertDialog.setCanceledOnTouchOutside(false);

                        TextView confirmDestination = (TextView) dialogView.findViewById(R.id.confirm_to);
                        confirmDestination.setText(pendingSpend.destination);

                        TextView confirmFee = (TextView) dialogView.findViewById(R.id.confirm_fee);
//                        pendingSpend.bfee = unspents.getRecommendedFee();
                        pendingSpend.bfee = BigInteger.valueOf(10000L);
                        confirmFee.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(pendingSpend.bfee.longValue()) + " " + strBTC);

                        TextView confirmTotal = (TextView) dialogView.findViewById(R.id.confirm_total_to_send);
                        BigInteger cTotal = (pendingSpend.bamount.add(pendingSpend.bfee));
                        confirmTotal.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(cTotal.longValue()) + " " + strBTC);

                        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
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

                                if(!spendInProgress) {
                                    spendInProgress = true;

                                    final int account = currentAcc;
                                    final String destination = pendingSpend.destination;
                                    final BigInteger bamount = pendingSpend.bamount;
                                    final BigInteger bfee = pendingSpend.bfee;
                                    final String strNote = null;

                                    if(progress != null && progress.isShowing()) {
                                        progress.dismiss();
                                        progress = null;
                                    }
                                    progress = new ProgressDialog(getActivity());
                                    progress.setCancelable(false);
                                    progress.setTitle(R.string.app_name);
                                    progress.setMessage(getString(R.string.sending));
                                    progress.show();

                                    context = getActivity();

                                    if(unspents != null) {

                                        if(isHd) {

                                            SendFactory.getInstance(context).execSend(account, unspents.getOutputs(), destination, bamount, null, bfee, strNote, false, new OpCallback() {

                                                public void onSuccess() {
                                                }

                                                @Override
                                                public void onSuccess(final String hash) {

                                                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                                    if (audioManager!=null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                                        MediaPlayer mp;
                                                        mp = MediaPlayer.create(context, R.raw.alert);
                                                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                                                            @Override
                                                            public void onCompletion(MediaPlayer mp) {
                                                                mp.reset();
                                                                mp.release();
                                                            }

                                                        });
                                                        mp.start();
                                                    }

                                                    ToastCustom.makeText(context, getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                                    PayloadBridge.getInstance(context).remoteSaveThread();

                                                    MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (bamount.longValue() + bfee.longValue()));
                                                    MultiAddrFactory.getInstance().setXpubAmount(HDPayloadBridge.getInstance(context).account2Xpub(account), MultiAddrFactory.getInstance().getXpubAmounts().get(HDPayloadBridge.getInstance(context).account2Xpub(account)) - (bamount.longValue() + bfee.longValue()));

                                                    closeDialog(alertDialog, true);
                                                }

                                                public void onFail() {
                                                    //Initial send failed - Put send in queue for reattempt
                                                    String direction = MultiAddrFactory.SENT;
                                                    if(spDestinationSelected)direction = MultiAddrFactory.MOVED;

                                                    final Intent intent = new Intent("info.blockchain.wallet.BalanceFragment.REFRESH");
                                                    Bundle bundle = new Bundle();
                                                    bundle.putLong("queued_bamount", (bamount.longValue() + bfee.longValue()));
                                                    bundle.putString("queued_strNote", strNote);
                                                    bundle.putString("queued_direction", direction);
                                                    bundle.putLong("queued_time", System.currentTimeMillis() / 1000);
                                                    intent.putExtras(bundle);

                                                    ToastCustom.makeText(context, getResources().getString(R.string.transaction_queued), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                                    SendFactory.getInstance(context).execSend(account, unspents.getOutputs(), destination, bamount, null, bfee, strNote, true, this);

                                                    closeDialog(alertDialog, true);

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Looper.prepare();
                                                            try{Thread.sleep(1000);}catch (Exception e){}//wait for broadcast receiver to register
                                                            LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
                                                            Looper.loop();
                                                        }
                                                    }).start();

                                                }

                                                @Override
                                                public void onFailPermanently() {
                                                    if(getActivity()!=null)ToastCustom.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                                    closeDialog(alertDialog, false);
                                                }

                                            });
                                        }
                                        else if (legacyAddress != null) {

                                            SendFactory.getInstance(context).execSend(-1, unspents.getOutputs(), destination, bamount, legacyAddress, bfee, strNote, false, new OpCallback() {

                                                public void onSuccess() {
                                                }

                                                @Override
                                                public void onSuccess(final String hash) {
                                                    ToastCustom.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                                    if (strNote != null) {
                                                        PayloadBridge.getInstance(getActivity()).remoteSaveThread();
                                                    }

                                                    MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (bamount.longValue() + bfee.longValue()));

                                                    List<String> legacyAddrs = PayloadFactory.getInstance().get().getLegacyAddressStrings(PayloadFactory.NORMAL_ADDRESS);
                                                    String[] addrs = legacyAddrs.toArray(new String[legacyAddrs.size()]);
                                                    MultiAddrFactory.getInstance().getLegacy(addrs, false);

//                                                    updateTx(isHd, strNote, hash, 0, legacyAddress);
                                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));

                                                    closeDialog(alertDialog, true);
                                                }

                                                public void onFail() {

                                                    //Initial send failed - Put send in queue for reattempt
                                                    String direction = MultiAddrFactory.SENT;
                                                    if(spDestinationSelected)direction = MultiAddrFactory.MOVED;

                                                    final Intent intent = new Intent("info.blockchain.wallet.BalanceFragment.REFRESH");
                                                    Bundle bundle = new Bundle();
                                                    bundle.putLong("queued_bamount", (bamount.longValue() + bfee.longValue()));
                                                    bundle.putString("queued_strNote", strNote);
                                                    bundle.putString("queued_direction", direction);
                                                    bundle.putLong("queued_time", System.currentTimeMillis() / 1000);
                                                    intent.putExtras(bundle);

                                                    ToastCustom.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.transaction_queued), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                                    SendFactory.getInstance(getActivity()).execSend(-1, unspents.getOutputs(), destination, bamount, legacyAddress, bfee, strNote, true, this);

                                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));

                                                    closeDialog(alertDialog, true);

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Looper.prepare();
                                                            try{Thread.sleep(1000);}catch (Exception e){}//wait for broadcast receiver to register
                                                            LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
                                                            Looper.loop();
                                                        }
                                                    }).start();

                                                }

                                                @Override
                                                public void onFailPermanently() {
                                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                                    if(getActivity()!=null)ToastCustom.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                                    closeDialog(alertDialog, false);
                                                }

                                            });
                                        }
                                    }
                                    else{

                                        PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                        ToastCustom.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
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
        }
        else    {
            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
            ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
        }

    }

    private void closeDialog(AlertDialog alertDialog, boolean sendSuccess){

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
        }

        if(sendSuccess) {
            //Default to 'Total Funds' for lame mode
            if (AppUtil.getInstance(getActivity()).isLegacy()) {
                AccountsUtil.getInstance(getActivity()).setCurrentSpinnerIndex(0);
            }

            Fragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    private boolean isValidSpend() {

        try{
            if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }catch(Exception e){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        long lamount = 0L;
        try {
            lamount = (long) (( MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(pendingSpend.amount) ) ) * 1e8);
            if(!(pendingSpend.bamount.compareTo(BigInteger.ZERO) >= 0)) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }
        catch(NumberFormatException e) {
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        if(pendingSpend.isHD) {

            String xpub = HDPayloadBridge.getInstance().account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()));

            if(xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long _lamount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendCoins.bFee.longValue()) > _lamount) {
                    ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    return false;
                }
            }
        }
        else {
            long _lamount = 0L;
            if(AppUtil.getInstance(getActivity()).isLegacy())    {
                _lamount = MultiAddrFactory.getInstance().getLegacyBalance();
            }
            else    {
                _lamount = MultiAddrFactory.getInstance().getLegacyBalance(currentSelectedFromAddress);
            }

            if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendCoins.bFee.longValue()) > _lamount) {
                ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }

        return true;
    }

    class DestinationAdapter extends ArrayAdapter<String> {

        private ArrayList<String> items;

        public DestinationAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
            this.items = new ArrayList<String>(items);
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
            if(isDropdown){
                int layoutRes = R.layout.fragment_send_account_row_dropdown;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.receive_account_label);
                label.setText(items.get(position));
            }else{
                int layoutRes = R.layout.spinner_item;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.text);
                label.setText("");
            }

            return row;
        }
    }

    @Override
    public void onClick(View v) {

        String pad = "";
        switch (v.getId()) {
            case R.id.button1:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button2:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button3:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button4:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button5:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button6:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button7:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button8:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button9:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button10:pad = ".";break;
            case R.id.button0:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.buttonDeleteBack:pad = null;break;
            case R.id.buttonDone:numpad.setVisibility(View.GONE);break;
        }

        if(pad!=null) {
            // Append tapped #
            if (edAmount1.hasFocus()) {
                edAmount1.append(pad);
            } else if (edAmount2.hasFocus()) {
                edAmount2.append(pad);
            }
        }else{
            // Back clicked
            if (edAmount1.hasFocus()) {
                String e1 = edAmount1.getText().toString();
                if(e1.length()>0)edAmount1.setText(e1.substring(0, e1.length() - 1));
            } else if (edAmount2.hasFocus()) {
                String e2 = edAmount2.getText().toString();
                if(e2.length()>0)edAmount2.setText(e2.substring(0, e2.length() - 1));
            }
        }

        if (edAmount1.hasFocus() && edAmount1.getText().length()>0) {
            edAmount1.post(new Runnable() {
                @Override
                public void run() {
                    edAmount1.setSelection(edAmount1.getText().toString().length());
                }
            });
        } else if (edAmount2.hasFocus() && edAmount2.getText().length()>0) {
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
}