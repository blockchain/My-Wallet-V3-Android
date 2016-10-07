package piuk.blockchain.android.ui.send;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import info.blockchain.api.DynamicFee;
import info.blockchain.api.Unspent;
import info.blockchain.util.FeeUtil;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.AddressBookEntry;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;
import info.blockchain.wallet.payment.data.SuggestedFee;
import info.blockchain.wallet.payment.data.SweepBundle;
import info.blockchain.wallet.payment.data.UnspentOutputs;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.cache.DefaultAccountUnspentCache;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.payload.PayloadBridge;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;

public class SendViewModel extends BaseViewModel {

    private final String TAG = getClass().getSimpleName();

    private DataListener dataListener;
    private Context context;

    private PayloadManager payloadManager;
    private MonetaryUtil monetaryUtil;
    private Payment payment;
    public SendModel sendModel;

    private Thread unspentApiThread;

    private final static int SHOW_BTC = 1;
    private final static int SHOW_FIAT = 2;

    @Inject PrefsUtil prefsUtil;
    @Inject WalletAccountHelper walletAccountHelper;
    @Inject SSLVerifyUtil sslVerifyUtil;

    public SendViewModel(Context context, DataListener dataListener) {

        Injector.getInstance().getAppComponent().inject(this);

        int btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double exchangeRate = ExchangeRateFactory.getInstance().getLastPrice(fiatUnit);

        this.context = context;
        this.dataListener = dataListener;
        this.payloadManager = PayloadManager.getInstance();
        this.monetaryUtil = new MonetaryUtil(btcUnit);
        this.payment = new Payment();

        this.sendModel = new SendModel();
        this.sendModel.pendingTransaction = new PendingTransaction();
        this.sendModel.btcUnit = monetaryUtil.getBTCUnit(btcUnit);
        this.sendModel.fiatUnit = fiatUnit;
        this.sendModel.btcUniti = btcUnit;
        this.sendModel.isBTC = getBtcDisplayState();
        this.sendModel.defaultSeparator = getDefaultDecimalSeparator();
        this.sendModel.exchangeRate = exchangeRate;
        this.sendModel.unspentApiResponse = new HashMap<>();
        this.sendModel.btcExchange = ExchangeRateFactory.getInstance().getLastPrice(sendModel.fiatUnit);

        dataListener.onUpdateBtcUnit(this.sendModel.btcUnit);
        dataListener.onUpdateFiatUnit(this.sendModel.fiatUnit);
        getSuggestedFee();

        sslVerifyUtil.validateSSL();
    }

    private boolean getBtcDisplayState() {
        boolean isBTC = true;
        int BALANCE_DISPLAY_STATE = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        if (BALANCE_DISPLAY_STATE == SHOW_FIAT) {
            isBTC = false;
        }

        return isBTC;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    @Override
    public void destroy() {
        super.destroy();
        context = null;
        dataListener = null;
    }

    public String getDefaultSeparator() {
        return sendModel.defaultSeparator;
    }

    public interface DataListener {
        void onHideSendingAddressField();

        void onHideReceivingAddressField();

        void onRemoveBtcTextChangeListener();

        void onRemoveFiatTextChangeListener();

        void onAddBtcTextChangeListener();

        void onAddFiatTextChangeListener();

        void onUpdateBtcAmount(String amount);

        void onUpdateFiatAmount(String amount);

        void onUpdateBtcUnit(String unit);

        void onUpdateFiatUnit(String unit);

        void onSetSpendAllAmount(String textFromSatoshis);

        void onShowInvalidAmount();

        void onShowSpendFromWatchOnly(String address);

        void onShowPaymentDetails(PaymentConfirmationDetails confirmationDetails);

        void onShowReceiveToWatchOnlyWarning(String address);

        void onShowAlterFee(String absoluteFeeSuggested, String body, int positiveAction, int negativeAction);

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onShowTransactionSuccess();

        void onShowBIP38PassphrasePrompt(String scanData);
    }

    public int getDefaultAccount() {

        int result = 0;
        if (payloadManager.getPayload().isUpgraded()) {
            result = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        }
        return Math.max(getCorrectedAccountIndex(result), 0);
    }

    private int getCorrectedAccountIndex(int accountIndex) {
        // Filter accounts by active
        List<Account> activeAccounts = new ArrayList<>();
        List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return activeAccounts.indexOf(payloadManager.getPayload().getHdWallet().getAccounts().get(accountIndex));
    }

    /**
     * Returns a list of accounts, legacy addresses and optionally Address Book entries
     *
     * @param includeAddressBookEntries Whether or not to include a user's Address book
     * @return List of account details (balance, label, tag, account/address/address_book object)
     */
    public List<ItemAccount> getAddressList(boolean includeAddressBookEntries) {

        ArrayList<ItemAccount> result = new ArrayList<ItemAccount>() {{
            addAll(walletAccountHelper.getAccountItems(sendModel.isBTC));
        }};

        if (result.size() == 1) {
            //Only a single account/address available in wallet
            dataListener.onHideSendingAddressField();
            calculateTransactionAmounts(result.get(0), null, null, null);
        }

        //Address Book (only included in receiving)
        if (includeAddressBookEntries) {
            result.addAll(walletAccountHelper.getAddressBookEntries());
        }

        if (result.size() == 1) {
            //Only a single account/address available in wallet and no addressBook entries
            dataListener.onHideReceivingAddressField();
        }

        return result;
    }

    /**
     * Gets device's specified locale decimal separator
     *
     * @return decimal separator
     */
    String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    /**
     * Checks btc amount. Warns user when exceeding maximum and resets entered value field
     */
    private boolean isExceedingMaximumBTCAmount(String btc) {
        long lamount = 0L;
        try {
            //Long is safe to use, but double can lead to ugly rounding issues..
            Double btcDouble = Double.parseDouble(btc);
            double undenominatedAmount = monetaryUtil.getUndenominatedAmount(btcDouble);
            lamount = (BigDecimal.valueOf(undenominatedAmount).multiply(BigDecimal.valueOf(100000000)).longValue());

            if (BigInteger.valueOf(lamount).compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
                dataListener.onShowInvalidAmount();
                dataListener.onUpdateBtcAmount("");
                return true;
            }
        } catch (NumberFormatException nfe) {
            return false;
        }
        return false;
    }

    /**
     * Update fiat text field with converted btc amount
     *
     * @param btcAmountText (btc, mbtc or bits)
     */
    public void afterBtcTextChanged(String btcAmountText) {

        if (isExceedingMaximumBTCAmount(btcAmountText)) {
            return;
        }

        dataListener.onRemoveBtcTextChangeListener();

        int max_len;
        NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
        switch (sendModel.btcUniti) {
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
            if (btcAmountText.indexOf(sendModel.defaultSeparator) != -1) {
                String dec = btcAmountText.substring(btcAmountText.indexOf(sendModel.defaultSeparator));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > max_len) {
                        dataListener.onUpdateBtcAmount(btcAmountText.substring(0, btcAmountText.length() - 1));
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            ;
        }

        dataListener.onAddBtcTextChangeListener();

        if (sendModel.textChangeAllowed) {
            sendModel.textChangeAllowed = false;

            if (btcAmountText.isEmpty()) btcAmountText = "0";
            double btc_amount;
            try {
                btc_amount = monetaryUtil.getUndenominatedAmount(NumberFormat.getInstance(Locale.getDefault()).parse(btcAmountText).doubleValue());
            } catch (NumberFormatException nfe) {
                btc_amount = 0.0;
            } catch (ParseException pe) {
                btc_amount = 0.0;
            }

            double fiat_amount = sendModel.exchangeRate * btc_amount;
            dataListener.onUpdateFiatAmount(monetaryUtil.getFiatFormat(sendModel.fiatUnit).format(fiat_amount));

            sendModel.textChangeAllowed = true;
        }
    }

    /**
     * Update btc text field with converted fiat amount
     *
     * @param fiatAmountText (any currency)
     */
    public void afterFiatTextChanged(String fiatAmountText) {

        dataListener.onRemoveFiatTextChangeListener();

        int max_len = 2;
        NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
        fiatFormat.setMaximumFractionDigits(max_len + 1);
        fiatFormat.setMinimumFractionDigits(0);

        try {
            if (fiatAmountText.indexOf(sendModel.defaultSeparator) != -1) {
                String dec = fiatAmountText.substring(fiatAmountText.indexOf(sendModel.defaultSeparator));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > max_len) {
                        dataListener.onUpdateFiatAmount(fiatAmountText.substring(0, fiatAmountText.length() - 1));
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            ;
        }

        dataListener.onAddFiatTextChangeListener();

        if (sendModel.textChangeAllowed) {
            sendModel.textChangeAllowed = false;

            if (fiatAmountText.isEmpty()) fiatAmountText = "0";
            double fiat_amount;
            try {
                fiat_amount = NumberFormat.getInstance(Locale.getDefault()).parse(fiatAmountText).doubleValue();
            } catch (NumberFormatException | ParseException e) {
                fiat_amount = 0.0;
            }
            double btc_amount = fiat_amount / sendModel.exchangeRate;
            dataListener.onUpdateBtcAmount(monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(btc_amount)));
            sendModel.textChangeAllowed = true;
        }
    }

    /**
     * Handle incoming scan data or bitcoin links
     */
    public void handleIncomingQRScan(String scanData) {

        scanData = scanData.trim();

        String btcAddress = null;
        String btcAmount = null;

        // check for poorly formed BIP21 URIs
        if (scanData.startsWith("bitcoin://") && scanData.length() > 10) {
            scanData = "bitcoin:" + scanData.substring(10);
        }

        if (FormatsUtil.getInstance().isValidBitcoinAddress(scanData)) {
            btcAddress = scanData;
        } else if (FormatsUtil.getInstance().isBitcoinUri(scanData)) {
            btcAddress = FormatsUtil.getInstance().getBitcoinAddress(scanData);
            btcAmount = FormatsUtil.getInstance().getBitcoinAmount(scanData);

            //Convert to correct units
            try {
                btcAmount = monetaryUtil.getDisplayAmount(Long.parseLong(btcAmount));
            } catch (Exception e) {
                btcAmount = null;
            }

        } else {
            dataListener.onShowToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
            return;
        }

        if (!btcAddress.equals("")) {
            sendModel.setDestinationAddress(btcAddress);
            sendModel.pendingTransaction.receivingObject = null;
        }
        if (btcAmount != null && !btcAmount.equals("")) {
            dataListener.onRemoveBtcTextChangeListener();
            dataListener.onRemoveFiatTextChangeListener();

            dataListener.onUpdateBtcAmount(btcAmount);

            double btc_amount;

            try {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
                Number btcNumber = numberFormat.parse(btcAmount);
                btc_amount = monetaryUtil.getUndenominatedAmount(btcNumber.doubleValue());
            } catch (NumberFormatException | ParseException e) {
                btc_amount = 0.0;
            }

            sendModel.exchangeRate = ExchangeRateFactory.getInstance().getLastPrice(sendModel.fiatUnit);

            double fiat_amount = sendModel.exchangeRate * btc_amount;

            dataListener.onUpdateFiatAmount(monetaryUtil.getFiatFormat(sendModel.fiatUnit).format(fiat_amount));

            //QR scan comes in as BTC - set current btc unit
            prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);

            dataListener.onUpdateBtcUnit(sendModel.btcUnit);
            dataListener.onUpdateFiatUnit(sendModel.fiatUnit);

            dataListener.onAddBtcTextChangeListener();
            dataListener.onAddFiatTextChangeListener();
        }
    }

    /**
     * Get cahced dynamic fee from Bci dynamic fee API
     */
    public void getSuggestedFee() {

        //Get cached fee
        sendModel.suggestedFee = DynamicFeeCache.getInstance().getSuggestedFee();

        //Refresh cache
        new Thread(() -> {
            DynamicFeeCache.getInstance().setSuggestedFee(new DynamicFee().getDynamicFee());
            sendModel.suggestedFee = DynamicFeeCache.getInstance().getSuggestedFee();
        }).start();
    }

    /**
     * Wrapper for calculateTransactionAmounts
     */
    public void spendAllClicked(ItemAccount sendAddressItem, String customFeeText) {
        calculateTransactionAmounts(true, sendAddressItem, null, customFeeText, null);
    }

    /**
     * Wrapper for calculateTransactionAmounts
     */
    public void calculateTransactionAmounts(ItemAccount sendAddressItem, String amountToSendText, String customFeeText, TransactionDataListener listener) {
        calculateTransactionAmounts(false, sendAddressItem, amountToSendText, customFeeText, listener);
    }

    public interface TransactionDataListener {
        void onReady();
    }

    /**
     * TODO - could be cleaned up more (kept this mostly in tact from previous send code)
     *
     * Fetches unspent data Gets spendable coins Mixed checks and updates
     */
    private void calculateTransactionAmounts(boolean spendAll, ItemAccount sendAddressItem,
                                             String amountToSendText, String customFeeText, TransactionDataListener listener) {

        sendModel.setMaxAvailableProgressVisibility(View.VISIBLE);
        sendModel.setMaxAvailableVisibility(View.GONE);
        sendModel.setUnconfirmedFunds("");

        String address;

        if (sendAddressItem.accountObject instanceof Account) {
            //xpub
            address = ((Account) sendAddressItem.accountObject).getXpub();

        } else {
            //legacy address
            address = ((LegacyAddress) sendAddressItem.accountObject).getAddress();
        }

        if (unspentApiThread != null) {
            unspentApiThread.interrupt();
        }

        unspentApiThread = new Thread(() -> {
            Looper.prepare();

            JSONObject unspentResponse = null;
            try {
                unspentResponse = getUnspentApiResponse(address);
            } catch (Exception e) {
                Log.w(TAG, "Thread deliberately interrupted", e);
                return;
            }
            if (unspentResponse != null) {

                BigInteger amountToSend = getSatoshisFromText(amountToSendText);
                BigInteger customFee = getSatoshisFromText(customFeeText);

                final UnspentOutputs coins = payment.getCoins(unspentResponse);

                //Future use. There might be some unconfirmed funds. Not displaying a warning currently (to line up with iOS and Web wallet)
                if (coins.getNotice() != null) {
                    sendModel.setUnconfirmedFunds(coins.getNotice());
                } else {
                    sendModel.setUnconfirmedFunds("");
                }

                sendModel.absoluteSuggestedFee = getSuggestedAbsoluteFee(coins, amountToSend);

                if (customFeeText != null && !customFeeText.isEmpty() || customFee.compareTo(BigInteger.ZERO) == 1) {
                    customFeePayment(coins, amountToSend, customFee, spendAll);
                } else {
                    suggestedFeePayment(coins, amountToSend, spendAll);
                }

            } else {
                //No unspent outputs
                updateMaxAvailable(0);
                sendModel.pendingTransaction.unspentOutputBundle = null;
            }

            if (listener != null) listener.onReady();

            Looper.loop();
        });

        unspentApiThread.start();

    }

    private BigInteger getSuggestedAbsoluteFee(final UnspentOutputs coins, BigInteger amountToSend) {
        SpendableUnspentOutputs spendableCoins = payment.getSpendableCoins(coins, amountToSend, sendModel.suggestedFee.defaultFeePerKb);
        return spendableCoins.getAbsoluteFee();
    }

    /**
     * Payment will use customized fee
     */
    private void customFeePayment(final UnspentOutputs coins, BigInteger amountToSend, BigInteger customFee, boolean spendAll) {

        SweepBundle sweepBundle = payment.getSweepBundle(coins, BigInteger.ZERO);
        long balanceAfterFee = sweepBundle.getSweepAmount().longValue() - customFee.longValue();
        updateMaxAvailable(balanceAfterFee);

        if (spendAll) {
            dataListener.onSetSpendAllAmount(getTextFromSatoshis(balanceAfterFee));
            amountToSend = BigInteger.valueOf(balanceAfterFee);
        }


        validateCustomFee(amountToSend.add(customFee), sweepBundle.getSweepAmount());

        SpendableUnspentOutputs unspentOutputBundle = payment.getSpendableCoins(coins,
                amountToSend,
                BigInteger.ZERO);

        sendModel.pendingTransaction.bigIntAmount = amountToSend;
        sendModel.pendingTransaction.unspentOutputBundle = unspentOutputBundle;
        sendModel.pendingTransaction.bigIntFee = customFee;

        if (sendModel.suggestedFee != null && sendModel.suggestedFee.estimateList != null) {
            updateEstimateConfirmationTime(amountToSend, customFee.longValue(), coins);
        }
    }

    /**
     * Payment will use suggested dynamic fee
     */
    private void suggestedFeePayment(final UnspentOutputs coins, BigInteger amountToSend, boolean spendAll) {

        SweepBundle sweepBundle = payment.getSweepBundle(coins, sendModel.suggestedFee.defaultFeePerKb);
        long balanceAfterFee = sweepBundle.getSweepAmount().longValue();
        updateMaxAvailable(balanceAfterFee);

        if (spendAll) {
            amountToSend = BigInteger.valueOf(balanceAfterFee);
            dataListener.onSetSpendAllAmount(getTextFromSatoshis(balanceAfterFee));
        }

        BigInteger feePerKb = sendModel.suggestedFee.defaultFeePerKb;

        SpendableUnspentOutputs unspentOutputBundle = payment.getSpendableCoins(coins,
                amountToSend,
                feePerKb);

        sendModel.pendingTransaction.bigIntAmount = amountToSend;
        sendModel.pendingTransaction.unspentOutputBundle = unspentOutputBundle;
        sendModel.pendingTransaction.bigIntFee = sendModel.pendingTransaction.unspentOutputBundle.getAbsoluteFee();

        if (sendModel.suggestedFee != null && sendModel.suggestedFee.estimateList != null) {
            updateEstimateConfirmationTime(amountToSend, sendModel.pendingTransaction.bigIntFee.longValue(), coins);
        }
    }

    /**
     * If user set customized fee that exceeds available amount, disable send button
     */
    private void validateCustomFee(BigInteger totalToSend, BigInteger totalAvailable) {
        if (totalToSend.compareTo(totalAvailable) == 1) {
            sendModel.setCustomFeeColor(ContextCompat.getColor(context, R.color.blockchain_send_red));
        } else {
            sendModel.setCustomFeeColor(ContextCompat.getColor(context, R.color.textColorPrimary));
        }
    }

    /**
     * Update max available. Values are bound to UI, so UI will apdate automatically
     */
    private void updateMaxAvailable(long balanceAfterFee) {
        sendModel.maxAvailable = BigInteger.valueOf(balanceAfterFee);
        sendModel.setMaxAvailableProgressVisibility(View.GONE);
        sendModel.setMaxAvailableVisibility(View.VISIBLE);

        if (balanceAfterFee <= 0) {
            sendModel.setMaxAvailableColor(ContextCompat.getColor(context, R.color.blockchain_send_red));
        } else {
            sendModel.setMaxAvailableColor(ContextCompat.getColor(context, R.color.blockchain_blue));
        }

        //Format for display
        if (!sendModel.isBTC) {
            double fiatBalance = sendModel.btcExchange * (Math.max(balanceAfterFee, 0.0) / 1e8);
            String fiatBalanceFormatted = monetaryUtil.getFiatFormat(sendModel.fiatUnit).format(fiatBalance);
            sendModel.setMaxAviable(context.getString(R.string.max_available) + " " + fiatBalanceFormatted + " " + sendModel.fiatUnit);
        } else {
            String btcAmountFormatted = monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(Math.max(balanceAfterFee, 0.0) / 1e8));
            sendModel.setMaxAviable(context.getString(R.string.max_available) + " " + btcAmountFormatted + " " + sendModel.btcUnit);
        }
    }

    /**
     * Calculate estimated fees needed for tx to be included in blocks
     *
     * @return List of fees needed to be included in co-responding blocks
     */
    private BigInteger[] getEstimatedBlocks(BigInteger amountToSend, ArrayList<SuggestedFee.Estimates> estimates, UnspentOutputs coins) {
        BigInteger[] absoluteFeeSuggestedEstimates = new BigInteger[estimates.size()];

        for (int i = 0; i < absoluteFeeSuggestedEstimates.length; i++) {

            BigInteger feePerKb = estimates.get(i).fee;
            SpendableUnspentOutputs unspentOutputBundle = payment.getSpendableCoins(coins, amountToSend, feePerKb);

            if (unspentOutputBundle != null) {
                absoluteFeeSuggestedEstimates[i] = unspentOutputBundle.getAbsoluteFee();
            }
        }

        return absoluteFeeSuggestedEstimates;
    }

    /**
     * Retrieves unspent api data in memory. If not in memory yet, it will be retrieved and added.
     * Default account will be retrieved from cache to speed up loading
     *
     * TODO - can speed up by checking if multi_address balance > 0 before calling unspent_api
     */
    private JSONObject getUnspentApiResponse(String address) throws Exception {
        if (sendModel.unspentApiResponse.containsKey(address)) {
            return sendModel.unspentApiResponse.get(address);
        } else {

            JSONObject unspentResponse = null;

            //Get cache if is default account
            DefaultAccountUnspentCache cache = DefaultAccountUnspentCache.getInstance();
            if (payloadManager.getPayload().getHdWallet() != null && address.equals(cache.getXpub())) {
                unspentResponse = cache.getUnspentApiResponse();

                //Refresh default account cache
                new Thread(() -> {
                    try {
                        cache.setUnspentApiResponse(address, new Unspent().getUnspentOutputs(address));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                unspentResponse = new Unspent().getUnspentOutputs(address);
            }

            sendModel.unspentApiResponse.put(address, unspentResponse);

            return unspentResponse;
        }
    }

    /**
     * Returns amount of satoshis from btc amount. This could be btc, mbtc or bits.
     *
     * @return satoshis
     */
    private BigInteger getSatoshisFromText(String text) {

        if (text == null || text.isEmpty()) return BigInteger.ZERO;

        //Format string to parsable double
        String amountToSend = text.trim().replace(" ", "").replace(sendModel.defaultSeparator, ".");

        double amount;
        try {
            amount = Double.parseDouble(amountToSend);
        } catch (NumberFormatException nfe) {
            amount = 0.0;
        }

        long amountL = (BigDecimal.valueOf(monetaryUtil.getUndenominatedAmount(amount)).multiply(BigDecimal.valueOf(100000000)).longValue());
        return BigInteger.valueOf(amountL);
    }

    /**
     * Returns btc amount from satoshis.
     *
     * @return btc, mbtc or bits relative to what is set in monetaryUtil
     */
    private String getTextFromSatoshis(long satoshis) {

        String displayAmount = monetaryUtil.getDisplayAmount(satoshis);
        displayAmount = displayAmount.replace(".", sendModel.defaultSeparator);
        return displayAmount;
    }

    /**
     * Updates text displaying what block tx will be included in
     */
    private String updateEstimateConfirmationTime(BigInteger amountToSend, long fee, UnspentOutputs coins) {

        sendModel.absoluteSuggestedFeeEstimates = getEstimatedBlocks(amountToSend, sendModel.suggestedFee.estimateList, coins);

        String likelyToConfirmMessage = context.getText(R.string.estimate_confirm_block_count).toString();
        String unlikelyToConfirmMessage = context.getText(R.string.fee_too_low_no_confirm).toString();

        long minutesPerBlock = 10;
        Arrays.sort(sendModel.absoluteSuggestedFeeEstimates, Collections.reverseOrder());

        String estimateText = unlikelyToConfirmMessage;

        for (int i = 0; i < sendModel.absoluteSuggestedFeeEstimates.length; i++) {
            if (fee >= sendModel.absoluteSuggestedFeeEstimates[i].longValue()) {
                estimateText = likelyToConfirmMessage;
                estimateText = String.format(estimateText, ((i + 1) * minutesPerBlock), (i + 1));
                break;
            }
        }

        sendModel.setEstimate(estimateText);

        if (estimateText.equals(unlikelyToConfirmMessage)) {
            sendModel.setEstimateColor(ContextCompat.getColor(context, R.color.blockchain_send_red));
        } else {
            sendModel.setEstimateColor(ContextCompat.getColor(context, R.color.blockchain_blue));
        }

        return estimateText;
    }

    /**
     * //TODO could be improved Sanity checks before prompting confirmation
     */
    public void sendClicked(boolean bypassFeeCheck, String address) {

        if (FormatsUtil.getInstance().isValidBitcoinAddress(address)) {
            //Receiving address manual or scanned input
            sendModel.pendingTransaction.receivingAddress = address;
        }

        if (bypassFeeCheck || isFeeAdequate()) {

            if (isValidSpend(sendModel.pendingTransaction)) {

                //Currently only v2 has watch-only
                if (!sendModel.pendingTransaction.isHD() &&
                        ((LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject).isWatchOnly()) {

                    dataListener.onShowSpendFromWatchOnly(((LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject).getAddress());

                } else if (sendModel.verifiedSecondPassword != null) {
                    confirmPayment();

                } else {
                    new SecondPasswordHandler(context).validate(new SecondPasswordHandler.ResultListener() {
                        @Override
                        public void onNoSecondPassword() {
                            confirmPayment();
                        }

                        @Override
                        public void onSecondPasswordValidated(String validatedSecondPassword) {
                            sendModel.verifiedSecondPassword = validatedSecondPassword;
                            confirmPayment();
                        }
                    });
                }
            }
        }
    }

    /**
     * Checks that fee is not smaller than what push_tx api will accept. Checks and alerts if
     * customized fee is too small or too large.
     */
    private boolean isFeeAdequate() {

        //Push tx endpoint only accepts > 10000 per kb fees
        if (sendModel.pendingTransaction.unspentOutputBundle != null && sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs() != null
                && !FeeUtil.isAdequateFee(sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(),
                2,//assume change
                sendModel.pendingTransaction.bigIntFee)) {
            dataListener.onShowToast(R.string.insufficient_fee, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (sendModel.suggestedFee != null && sendModel.suggestedFee.estimateList != null) {

            if (sendModel.absoluteSuggestedFeeEstimates != null
                    && sendModel.pendingTransaction.bigIntFee.compareTo(sendModel.absoluteSuggestedFeeEstimates[0]) > 0) {

                String message = String.format(context.getString(R.string.high_fee_not_necessary_info),
                        monetaryUtil.getDisplayAmount(sendModel.pendingTransaction.bigIntFee.longValue()) + " " + sendModel.btcUnit,
                        monetaryUtil.getDisplayAmount(sendModel.absoluteSuggestedFeeEstimates[0].longValue()) + " " + sendModel.btcUnit);

                dataListener.onShowAlterFee(
                        getTextFromSatoshis(sendModel.absoluteSuggestedFeeEstimates[0].longValue()),
                        message,
                        R.string.lower_fee,
                        R.string.keep_high_fee);

                return false;
            }

            if (sendModel.absoluteSuggestedFeeEstimates != null
                    && sendModel.pendingTransaction.bigIntFee.compareTo(sendModel.absoluteSuggestedFeeEstimates[5]) < 0) {

                String message = String.format(context.getString(R.string.low_fee_suggestion),
                        monetaryUtil.getDisplayAmount(sendModel.pendingTransaction.bigIntFee.longValue()) + " " + sendModel.btcUnit,
                        monetaryUtil.getDisplayAmount(sendModel.absoluteSuggestedFeeEstimates[5].longValue()) + " " + sendModel.btcUnit);

                dataListener.onShowAlterFee(
                        getTextFromSatoshis(sendModel.absoluteSuggestedFeeEstimates[5].longValue()),
                        message,
                        R.string.raise_fee,
                        R.string.keep_low_fee);

                return false;
            }


        }

        return true;
    }

    /**
     * Sets payment confirmation details to be displayed to user and fires callback to display
     * this.
     */
    private void confirmPayment() {

        PendingTransaction pendingTransaction = sendModel.pendingTransaction;

        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        details.fromLabel = pendingTransaction.sendingObject.label;
        if (pendingTransaction.receivingObject != null
                && pendingTransaction.receivingObject.label != null
                && !pendingTransaction.receivingObject.label.isEmpty()) {
            details.toLabel = pendingTransaction.receivingObject.label;
        } else {
            details.toLabel = pendingTransaction.receivingAddress;
        }
        details.btcAmount = getTextFromSatoshis(pendingTransaction.bigIntAmount.longValue());
        details.btcFee = getTextFromSatoshis(pendingTransaction.bigIntFee.longValue());
        details.btcSuggestedFee = getTextFromSatoshis(sendModel.absoluteSuggestedFee.longValue());
        details.btcUnit = sendModel.btcUnit;
        details.fiatUnit = sendModel.fiatUnit;
        details.btcTotal = getTextFromSatoshis(pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).longValue());

        details.fiatFee = (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (pendingTransaction.bigIntFee.doubleValue() / 1e8)));

        details.fiatAmount = (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (pendingTransaction.bigIntAmount.doubleValue() / 1e8)));

        BigInteger totalFiat = (pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee));
        details.fiatTotal = (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (totalFiat.doubleValue() / 1e8)));

        details.isSurge = sendModel.suggestedFee.isSurge;
        details.isLargeTransaction = isLargeTransaction();
        details.hasConsumedAmounts = pendingTransaction.unspentOutputBundle.getConsumedAmount().compareTo(BigInteger.ZERO) == 1;

        dataListener.onShowPaymentDetails(details);
    }

    /**
     * Returns true if transaction is large by checking if fee > USD 0.50, size > 516, fee > 1% of
     * total
     */
    public boolean isLargeTransaction() {

        int txSize = FeeUtil.estimatedSize(sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(), 2);//assume change
        double relativeFee = sendModel.absoluteSuggestedFee.doubleValue() / sendModel.pendingTransaction.bigIntAmount.doubleValue() * 100.0;

        if (sendModel.absoluteSuggestedFee.longValue() > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE) {

            return true;
        } else {
            return false;
        }
    }

    /**
     * Various checks on validity of transaction details
     */
    private boolean isValidSpend(PendingTransaction pendingTransaction) {

        //Validate amount
        if (!isValidAmount(pendingTransaction.bigIntAmount)) {
            dataListener.onShowInvalidAmount();
            return false;
        }

        //Validate sufficient funds
        if (pendingTransaction.unspentOutputBundle == null || pendingTransaction.unspentOutputBundle.getSpendableOutputs() == null) {
            dataListener.onShowToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (sendModel.maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            dataListener.onShowToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
            sendModel.setCustomFeeColor(R.color.blockchain_send_red);
            return false;
        } else {
            sendModel.setCustomFeeColor(R.color.primary_text_default_material_light);
        }

        //Validate addresses
        if (pendingTransaction.receivingAddress == null || !FormatsUtil.getInstance().isValidBitcoinAddress(pendingTransaction.receivingAddress)) {
            dataListener.onShowToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
            return false;
        }

        //Validate send and receive not same addresses
        if (pendingTransaction.sendingObject == pendingTransaction.receivingObject) {
            dataListener.onShowToast(R.string.send_to_same_address_warning, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (pendingTransaction.unspentOutputBundle == null) {
            dataListener.onShowToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (pendingTransaction.unspentOutputBundle.getSpendableOutputs().size() == 0) {
            dataListener.onShowToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
            return false;
        }


        if (sendModel.pendingTransaction.receivingObject != null
                && sendModel.pendingTransaction.receivingObject.accountObject == sendModel.pendingTransaction.sendingObject.accountObject) {
            dataListener.onShowToast(R.string.send_to_same_address_warning, ToastCustom.TYPE_ERROR);
            return false;
        }

        return true;
    }

    public void setSendingAddress(ItemAccount selectedItem) {
        sendModel.pendingTransaction.sendingObject = selectedItem;
    }

    /**
     * Set the receiving object. Null can be passed to reset receiving address for when user
     * customizes address
     */
    public void setReceivingAddress(@Nullable ItemAccount selectedItem) {

        sendModel.pendingTransaction.receivingObject = selectedItem;

        if (selectedItem != null) {
            if (selectedItem.accountObject instanceof Account) {

                //V3
                Account account = ((Account) selectedItem.accountObject);
                try {
                    sendModel.pendingTransaction.receivingAddress = payloadManager.getNextReceiveAddress(account.getRealIdx());
                } catch (AddressFormatException e) {
                    e.printStackTrace();
                }

            } else if (selectedItem.accountObject instanceof LegacyAddress) {

                //V2
                LegacyAddress legacyAddress = ((LegacyAddress) selectedItem.accountObject);
                sendModel.pendingTransaction.receivingAddress = legacyAddress.getAddress();

                if (legacyAddress.isWatchOnly())
                    if (legacyAddress.isWatchOnly() && prefsUtil.getValue("WARN_WATCH_ONLY_SPEND", true)) {
                        dataListener.onShowReceiveToWatchOnlyWarning(legacyAddress.getAddress());
                    }
            } else {

                //Address book
                AddressBookEntry addressBook = ((AddressBookEntry) selectedItem.accountObject);
                sendModel.pendingTransaction.receivingAddress = addressBook.getAddress();
            }
        } else {
            sendModel.pendingTransaction.receivingAddress = "";
        }
    }

    private boolean isValidAmount(BigInteger bAmount) {

        if (bAmount == null) {
            return false;
        }

        //Test that amount is more than dust
        if (bAmount.compareTo(SendCoins.bDust) == -1) {
            return false;
        }

        //Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
            dataListener.onUpdateBtcAmount("0");
            return false;
        }

        //Test that amount is not zero
        if (!(bAmount.compareTo(BigInteger.ZERO) >= 0)) {
            return false;
        }

        return true;
    }

    /**
     * Executes transaction //TODO implement transaction queue for when transaction fails
     */
    public void submitPayment(AlertDialog alertDialog) {

        new Thread(() -> {
            try {

                String changeAddress;
                Account account = null;
                LegacyAddress legacyAddress = null;
                List<ECKey> keys = new ArrayList<ECKey>();

                if (sendModel.pendingTransaction.isHD()) {
                    account = ((Account) sendModel.pendingTransaction.sendingObject.accountObject);
                    changeAddress = payloadManager.getNextChangeAddress(account.getRealIdx());

                    keys.addAll(payloadManager.getHDKeys(sendModel.verifiedSecondPassword, account, sendModel.pendingTransaction.unspentOutputBundle));

                } else {
                    legacyAddress = ((LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject);
                    changeAddress = legacyAddress.getAddress();

                    if (!legacyAddress.isWatchOnly() && payloadManager.getPayload().isDoubleEncrypted()) {
                        ECKey walletKey = legacyAddress.getECKey(new CharSequenceX(sendModel.verifiedSecondPassword));
                        keys.add(walletKey);
                    } else {
                        ECKey walletKey = legacyAddress.getECKey();
                        keys.add(walletKey);
                    }
                }

                payment.submitPayment(sendModel.pendingTransaction.unspentOutputBundle,
                        keys,
                        sendModel.pendingTransaction.receivingAddress,
                        changeAddress,
                        sendModel.pendingTransaction.bigIntFee,
                        sendModel.pendingTransaction.bigIntAmount,
                        new Payment.SubmitPaymentListener() {
                            @Override
                            public void onSuccess(String s) {

                                if (alertDialog != null && alertDialog.isShowing())
                                    alertDialog.dismiss();

                                handleSuccessfulPayment();
                            }

                            @Override
                            public void onFail(String s) {
                                dataListener.onShowToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR);
                            }
                        });

            } catch (Exception e) {
                e.printStackTrace();
                dataListener.onShowToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR);
            }
        }).start();

    }

    private void handleSuccessfulPayment(){
        if (sendModel.pendingTransaction.isHD()) {
            // increment change address counter
            ((Account) sendModel.pendingTransaction.sendingObject.accountObject).incChange();
        }

        updateInternalBalances();
        PayloadBridge.getInstance().remoteSaveThread(null);
        dataListener.onShowTransactionSuccess();
    }

    /**
     * Update balance immediately after spend - until refresh from server
     */
    private void updateInternalBalances() {

        BigInteger totalSent = sendModel.pendingTransaction.bigIntAmount.add(sendModel.pendingTransaction.bigIntFee);

        if (sendModel.pendingTransaction.isHD()) {

            Account account = (Account) sendModel.pendingTransaction.sendingObject.accountObject;


            long updatedBalance = MultiAddrFactory.getInstance().getXpubBalance() - totalSent.longValue();

            //Set total balance
            MultiAddrFactory.getInstance().setXpubBalance(updatedBalance);

            //Set individual xpub balance
            MultiAddrFactory.getInstance().setXpubAmount(
                    account.getXpub(),
                    MultiAddrFactory.getInstance().getXpubAmounts().get(account.getXpub()) - updatedBalance);

        } else {
            MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - totalSent.longValue());
        }
    }

    public void handleScannedDataForWatchOnlySpend(String scanData) {
        try {
            final String format = PrivateKeyFactory.getInstance().getFormat(scanData);
            if (format != null) {
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    spendFromWatchOnlyNonBIP38(format, scanData);
                } else {
                    //BIP38 needs passphrase
                    dataListener.onShowBIP38PassphrasePrompt(scanData);
                }
            } else {
                dataListener.onShowToast(R.string.privkey_error, ToastCustom.TYPE_ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spendFromWatchOnlyNonBIP38(final String format, final String scanData) {

        try {
            ECKey key = PrivateKeyFactory.getInstance().getKey(format, scanData);
            LegacyAddress legacyAddress = (LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject;
            setTempLegacyAddressPrivateKey(legacyAddress, key);

        } catch (Exception e) {
            dataListener.onShowToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
            e.printStackTrace();
        }
    }

    private void setTempLegacyAddressPrivateKey(LegacyAddress legacyAddress, ECKey key) {
        if (key != null && key.hasPrivKey() && legacyAddress.getAddress().equals(key.toAddress(MainNetParams.get()).toString())) {

            //Create copy, otherwise pass by ref will override private key in wallet payload
            LegacyAddress tempLegacyAddress = new LegacyAddress();
            tempLegacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            tempLegacyAddress.setAddress(key.toAddress(MainNetParams.get()).toString());
            tempLegacyAddress.setLabel(legacyAddress.getLabel());
            tempLegacyAddress.setWatchOnly(true);
            sendModel.pendingTransaction.sendingObject.accountObject = tempLegacyAddress;

            confirmPayment();
        } else {
            dataListener.onShowToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR);
        }
    }

    public void spendFromWatchOnlyBIP38(String pw, String scanData) {
        new Thread(() -> {

            Looper.prepare();

            try {
                BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), scanData);
                final ECKey key = bip38.decrypt(pw);

                LegacyAddress legacyAddress = (LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject;
                setTempLegacyAddressPrivateKey(legacyAddress, key);

            } catch (Exception e) {
                dataListener.onShowToast(R.string.bip38_error, ToastCustom.TYPE_ERROR);
            }

            Looper.loop();

        }).start();
    }

    public void setWatchOnlySpendWarning(boolean enabled) {
        prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", enabled);
    }
}
