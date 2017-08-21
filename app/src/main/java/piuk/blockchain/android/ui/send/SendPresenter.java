package piuk.blockchain.android.ui.send;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.data.FeeOptions;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.AddressBook;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.answers.ContactEventType;
import piuk.blockchain.android.data.answers.ContactsEvent;
import piuk.blockchain.android.data.answers.Logging;
import piuk.blockchain.android.data.answers.PaymentSentEvent;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.auth.AuthService;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.models.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.FeeDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.payments.SendDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.EventService;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.annotations.Thunk;
import timber.log.Timber;

import static piuk.blockchain.android.ui.send.SendFragment.ARGUMENT_CONTACT_ID;
import static piuk.blockchain.android.ui.send.SendFragment.ARGUMENT_CONTACT_MDID;
import static piuk.blockchain.android.ui.send.SendFragment.ARGUMENT_FCTX_ID;
import static piuk.blockchain.android.ui.send.SendFragment.ARGUMENT_SCAN_DATA;
import static piuk.blockchain.android.ui.send.SendFragment.ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE;

@SuppressWarnings("WeakerAccess")
public class SendPresenter extends BasePresenter<SendView> {

    private static final String PREF_WARN_ADVANCED_FEE = "pref_warn_advanced_fee";
    private static final String PREF_WARN_WATCH_ONLY_SPEND = "pref_warn_watch_only_spend";

    private MonetaryUtil monetaryUtil;
    private ReceiveCurrencyHelper currencyHelper;
    private SendModel sendModel;
    @Nullable private String contactMdid;
    @Nullable private String fctxId;
    private String metricInputFlag;
    private Locale locale;

    private Disposable unspentApiDisposable;

    private PrefsUtil prefsUtil;
    private WalletAccountHelper walletAccountHelper;
    private ExchangeRateFactory exchangeRateFactory;
    private PrivateKeyFactory privateKeyFactory;
    private PayloadManager payloadManager;
    private StringUtils stringUtils;
    private ContactsDataManager contactsDataManager;
    private SendDataManager sendDataManager;
    private PayloadDataManager payloadDataManager;
    private DynamicFeeCache dynamicFeeCache;
    private TransactionListDataManager transactionListDataManager;
    private EnvironmentSettings environmentSettings;
    private FeeDataManager feeDataManager;
    private AccessState accessState;

    @Inject
    SendPresenter(PrefsUtil prefsUtil,
                  WalletAccountHelper walletAccountHelper,
                  ExchangeRateFactory exchangeRateFactory,
                  SSLVerifyUtil sslVerifyUtil,
                  PrivateKeyFactory privateKeyFactory,
                  PayloadManager payloadManager,
                  StringUtils stringUtils,
                  ContactsDataManager contactsDataManager,
                  SendDataManager sendDataManager,
                  PayloadDataManager payloadDataManager,
                  DynamicFeeCache dynamicFeeCache,
                  TransactionListDataManager transactionListDataManager,
                  EnvironmentSettings environmentSettings,
                  FeeDataManager feeDataManager,
                  AccessState accessState) {

        this.prefsUtil = prefsUtil;
        this.walletAccountHelper = walletAccountHelper;
        this.exchangeRateFactory = exchangeRateFactory;
        this.privateKeyFactory = privateKeyFactory;
        this.payloadManager = payloadManager;
        this.stringUtils = stringUtils;
        this.contactsDataManager = contactsDataManager;
        this.sendDataManager = sendDataManager;
        this.payloadDataManager = payloadDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
        this.transactionListDataManager = transactionListDataManager;
        this.environmentSettings = environmentSettings;
        this.feeDataManager = feeDataManager;
        this.accessState = accessState;
        locale = Locale.getDefault();

        sendModel = new SendModel();
        sendModel.pendingTransaction = new PendingTransaction();
        sendModel.unspentApiResponses = new HashMap<>();
        getSuggestedFee();

        sslVerifyUtil.validateSSL();
    }

    @Override
    public void onViewReady() {
        updateUI();

        if (getView().getFragmentBundle() != null) {
            final String scanData = getView().getFragmentBundle().getString(ARGUMENT_SCAN_DATA);
            final String contactId = getView().getFragmentBundle().getString(ARGUMENT_CONTACT_ID);
            contactMdid = getView().getFragmentBundle().getString(ARGUMENT_CONTACT_MDID);
            fctxId = getView().getFragmentBundle().getString(ARGUMENT_FCTX_ID);
            metricInputFlag = getView().getFragmentBundle().getString(ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE);

            if (contactId != null) {
                getView().lockContactsFields();

                getCompositeDisposable().add(
                        contactsDataManager.getContactList()
                                .filter(ContactsPredicates.filterById(contactId))
                                .firstOrError()
                                .subscribe(
                                        contact -> getView().setContactName(contact.getName()),
                                        throwable -> getView().finishPage(false)));
            }

            if (scanData != null) {
                handleIncomingQRScan(scanData, metricInputFlag);
            }
        }
    }

    void updateUI() {
        int btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        monetaryUtil = new MonetaryUtil(btcUnit);
        currencyHelper = new ReceiveCurrencyHelper(monetaryUtil, locale, prefsUtil, exchangeRateFactory);

        sendModel.btcUnit = monetaryUtil.getBtcUnit(btcUnit);
        sendModel.fiatUnit = fiatUnit;
        sendModel.btcUniti = btcUnit;
        sendModel.isBTC = accessState.isBtc();
        sendModel.exchangeRate = exchangeRate;
        sendModel.btcExchange = exchangeRateFactory.getLastPrice(sendModel.fiatUnit);

        getView().updateBtcUnit(sendModel.btcUnit);
        getView().updateFiatUnit(sendModel.fiatUnit);
    }

    void onSendClicked(String amount, String address, @FeeType.FeePriorityDef int feePriority) {
        // Contact selected but no FacilitationTransaction to respond to
        if (fctxId == null && contactMdid != null) {
            setupTransaction(amount, feePriority, () -> {
                if (isValidSpend(sendModel.pendingTransaction, true)) {
                    //noinspection SuspiciousMethodCalls
                    getCompositeDisposable().add(
                            contactsDataManager.getContactList()
                                    .filter(ContactsPredicates.filterByMdid(contactMdid))
                                    .firstOrError()
                                    .subscribe(
                                            contact -> getView().navigateToAddNote(
                                                    getConfirmationDetails(),
                                                    PaymentRequestType.SEND,
                                                    contact.getId(),
                                                    sendModel.pendingTransaction.bigIntAmount.longValue(),
                                                    payloadDataManager.getAccounts().indexOf(sendModel.pendingTransaction.sendingObject)),
                                            throwable -> showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR)));
                }
            });
        } else {
            setupTransaction(amount, feePriority, () -> sendClicked(address));
        }
    }

    int getDefaultAccount() {
        int result = 0;
        if (payloadManager.getPayload().isUpgraded()) {
            result = payloadManager.getPayload().getHdWallets().get(0).getDefaultAccountIdx();
        }
        return Math.max(getCorrectedAccountIndex(result), 0);
    }

    void setContact(@Nullable Contact contact) {
        if (contact != null) {
            contactMdid = contact.getMdid();
            getView().setContactName(contact.getName());
        } else {
            contactMdid = null;
        }
    }

    private int getCorrectedAccountIndex(int accountIndex) {
        // Filter accounts by active
        List<Account> activeAccounts = new ArrayList<>();
        List<Account> accounts = payloadManager.getPayload().getHdWallets().get(0).getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return activeAccounts.indexOf(payloadManager.getPayload().getHdWallets().get(0).getAccounts().get(accountIndex));
    }

    /**
     * Returns a list of accounts, legacy addresses and optionally Address Book entries
     *
     * @return List of account details (balance, label, tag, account/address/address_book object)
     */
    List<ItemAccount> getAddressList(int feePriority) {
        ArrayList<ItemAccount> result = new ArrayList<>();
        result.addAll(walletAccountHelper.getAccountItems(sendModel.isBTC));

        if (result.size() == 1) {
            //Only a single account/address available in wallet
            if (getView() != null) getView().hideSendingAddressField();
            calculateTransactionAmounts(result.get(0), null, feePriority, null);
        }

        if (result.size() == 1) {
            //Only a single account/address available in wallet and no addressBook entries
            if (getView() != null) getView().hideReceivingAddressField();
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

    void updateFiatTextField(String bitcoin) {
        if (bitcoin.isEmpty()) bitcoin = "0";
        double btcAmount = currencyHelper.getUndenominatedAmount(currencyHelper.getDoubleAmount(bitcoin));
        double fiatAmount = currencyHelper.getLastPrice() * btcAmount;
        getView().updateFiatTextField(currencyHelper.getFormattedFiatString(fiatAmount));
    }

    void updateBtcTextField(String fiat) {
        if (fiat.isEmpty()) fiat = "0";
        double fiatAmount = currencyHelper.getDoubleAmount(fiat);
        double btcAmount = fiatAmount / currencyHelper.getLastPrice();
        String amountString = currencyHelper.getFormattedBtcString(btcAmount);
        getView().updateBtcTextField(amountString);
    }

    public ReceiveCurrencyHelper getCurrencyHelper() {
        return currencyHelper;
    }

    /**
     * Handle incoming scan data or bitcoin links
     */
    void handleIncomingQRScan(String scanData, String scanRoute) {
        metricInputFlag = scanRoute;
        scanData = scanData.trim();
        String btcAddress;
        String btcAmount = null;

        // check for poorly formed BIP21 URIs
        if (scanData.startsWith("bitcoin://") && scanData.length() > 10) {
            scanData = "bitcoin:" + scanData.substring(10);
        }

        if (FormatsUtil.isValidBitcoinAddress(scanData)) {
            btcAddress = scanData;
        } else if (FormatsUtil.isBitcoinUri(scanData)) {
            btcAddress = FormatsUtil.getBitcoinAddress(scanData);
            btcAmount = FormatsUtil.getBitcoinAmount(scanData);

            //Convert to correct units
            try {
                btcAmount = monetaryUtil.getDisplayAmount(Long.parseLong(btcAmount));
            } catch (Exception e) {
                btcAmount = null;
            }

        } else {
            showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
            return;
        }

        if (!btcAddress.equals("")) {
            sendModel.pendingTransaction.receivingObject = null;
            sendModel.pendingTransaction.receivingAddress = btcAddress;
            getView().setDestinationAddress(btcAddress);
        }
        if (btcAmount != null && !btcAmount.equals("")) {
            if (getView() != null) {
                getView().updateBtcAmount(btcAmount);
                // QR scan comes in as BTC - set current btc unit
                prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
                getView().updateBtcUnit(sendModel.btcUnit);
                getView().updateFiatUnit(sendModel.fiatUnit);
            }
        }
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    private void getSuggestedFee() {
        sendModel.feeOptions = dynamicFeeCache.getFeeOptions();

        // Refresh fee cache
        getCompositeDisposable().add(
                feeDataManager.getFeeOptions()
                        .doOnError(ignored -> {
                            getView().showToast(R.string.confirm_payment_fee_sync_error, ToastCustom.TYPE_ERROR);
                            getView().finishPage(false);
                        })
                        .doOnTerminate(() -> sendModel.feeOptions = dynamicFeeCache.getFeeOptions())
                        .subscribe(
                                feeOptions -> dynamicFeeCache.setFeeOptions(feeOptions),
                                Throwable::printStackTrace));
    }

    /**
     * Wrapper for calculateTransactionAmounts
     */
    void spendAllClicked(ItemAccount sendAddressItem, @FeeType.FeePriorityDef int feePriority) {
        calculateTransactionAmounts(true, sendAddressItem, null, feePriority, null);
    }

    /**
     * Wrapper for calculateTransactionAmounts
     */
    void calculateTransactionAmounts(ItemAccount sendAddressItem,
                                     String amountToSendText,
                                     @FeeType.FeePriorityDef int feePriority,
                                     TransactionDataListener listener) {
        calculateTransactionAmounts(false,
                sendAddressItem,
                amountToSendText,
                feePriority,
                listener);
    }

    /**
     * TODO - could be cleaned up more (kept this mostly in tact from previous send code)
     *
     * Fetches unspent data Gets spendable coins Mixed checks and updates
     */
    private void calculateTransactionAmounts(boolean spendAll,
                                             ItemAccount sendAddressItem,
                                             String amountToSendText,
                                             @FeeType.FeePriorityDef int feePriority,
                                             TransactionDataListener listener) {

        //Convert selected fee priority to feePerKb
        BigInteger feePerKb = getFeePerKbFromPriority(feePriority);

        getView().setMaxAvailableVisible(false);
        getView().setUnconfirmedFunds("");

        String address;

        if (sendAddressItem.getAccountObject() instanceof Account) {
            //xpub
            address = ((Account) sendAddressItem.getAccountObject()).getXpub();
        } else {
            //legacy address
            address = ((LegacyAddress) sendAddressItem.getAccountObject()).getAddress();
        }

        if (unspentApiDisposable != null) unspentApiDisposable.dispose();

        unspentApiDisposable = getUnspentApiResponse(address)
                .compose(RxUtil.applySchedulersToObservable())
                .subscribe(
                        coins -> {
                            BigInteger amountToSend = getSatoshisFromText(amountToSendText);

                            // Future use. There might be some unconfirmed funds. Not displaying a warning currently (to line up with iOS and Web wallet)
                            getView().setUnconfirmedFunds(coins.getNotice() != null ? coins.getNotice() : "");
                            sendModel.absoluteSuggestedFee = getSuggestedAbsoluteFee(coins, amountToSend, feePerKb);
                            updateFeeField();

                            suggestedFeePayment(coins, amountToSend, spendAll, feePerKb);

                            if (listener != null) listener.onReady();
                        }, throwable -> {
                            Timber.e(throwable);
                            // No unspent outputs
                            sendModel.absoluteSuggestedFee = BigInteger.ZERO;
                            updateMaxAvailable(0);
                            updateFeeField();
                            sendModel.pendingTransaction.unspentOutputBundle = null;
                        });
    }

    private void updateFeeField() {
        String displayAmount = monetaryUtil.getDisplayAmount(sendModel.absoluteSuggestedFee.longValue());
        getView().updateFeeField(displayAmount
                + " "
                + sendModel.btcUnit
                + " ("
                + (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (sendModel.absoluteSuggestedFee.doubleValue() / 1e8))
                + sendModel.fiatUnit
                + ")"));
    }

    private BigInteger getSuggestedAbsoluteFee(final UnspentOutputs coins, BigInteger amountToSend, BigInteger feePerKb)
            throws UnsupportedEncodingException {
        SpendableUnspentOutputs spendableCoins = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb);
        return spendableCoins.getAbsoluteFee();
    }

    /**
     * Payment will use suggested dynamic fee
     */
    private void suggestedFeePayment(final UnspentOutputs coins, BigInteger amountToSend, boolean spendAll, BigInteger feePerKb)
            throws UnsupportedEncodingException {

        //Calculate sweepable amount to display max available
        Pair<BigInteger, BigInteger> sweepBundle = sendDataManager.getSweepableCoins(coins, feePerKb);
        BigInteger sweepableAmount = sweepBundle.getLeft();

        long balanceAfterFee = sweepableAmount.longValue();
        updateMaxAvailable(balanceAfterFee);

        if (spendAll) {
            amountToSend = BigInteger.valueOf(balanceAfterFee);
            if (getView() != null) {
                getView().onSetSpendAllAmount(getTextFromSatoshis(balanceAfterFee));
            }
        }

        SpendableUnspentOutputs unspentOutputBundle = sendDataManager.getSpendableCoins(coins,
                amountToSend,
                feePerKb);

        sendModel.pendingTransaction.bigIntAmount = amountToSend;
        sendModel.pendingTransaction.unspentOutputBundle = unspentOutputBundle;
        sendModel.pendingTransaction.bigIntFee = sendModel.pendingTransaction.unspentOutputBundle.getAbsoluteFee();
    }

    /**
     * Update max available. Values are bound to UI, so UI will update automatically
     */
    private void updateMaxAvailable(long balanceAfterFee) {
        sendModel.maxAvailable = BigInteger.valueOf(balanceAfterFee);
        getView().setMaxAvailableVisible(true);

        //Format for display
        if (!sendModel.isBTC) {
            double fiatBalance = sendModel.btcExchange * (Math.max(balanceAfterFee, 0.0) / 1e8);
            String fiatBalanceFormatted = monetaryUtil.getFiatFormat(sendModel.fiatUnit).format(fiatBalance);
            getView().setMaxAvailable(stringUtils.getString(R.string.max_available) + " " + fiatBalanceFormatted + " " + sendModel.fiatUnit);
        } else {
            String btcAmountFormatted = monetaryUtil.getBtcFormat().format(monetaryUtil.getDenominatedAmount(Math.max(balanceAfterFee, 0.0) / 1e8));
            getView().setMaxAvailable(stringUtils.getString(R.string.max_available) + " " + btcAmountFormatted + " " + sendModel.btcUnit);
        }

        if (balanceAfterFee <= Payment.DUST.longValue()) {
            getView().setMaxAvailable(stringUtils.getString(R.string.insufficient_funds));
            getView().setMaxAvailableColor(R.color.product_red_medium);
        } else {
            getView().setMaxAvailableColor(R.color.primary_blue_accent);
        }
    }

    /**
     * Retrieves unspent api data in memory. If not in memory yet, it will be retrieved and added.
     */
    private Observable<UnspentOutputs> getUnspentApiResponse(String address) {
        if (payloadManager.getAddressBalance(address).longValue() > 0) {
            if (sendModel.unspentApiResponses.containsKey(address)) {
                return Observable.just(sendModel.unspentApiResponses.get(address));
            } else {
                return sendDataManager.getUnspentOutputs(address);
            }
        } else {
            return Observable.error(new Throwable("No funds - skipping call to unspent API"));
        }
    }

    /**
     * Returns amount of satoshis from btc amount. This could be btc, mbtc or bits.
     *
     * @return satoshis
     */
    private BigInteger getSatoshisFromText(String text) {
        if (text == null || text.isEmpty()) return BigInteger.ZERO;

        String amountToSend = stripSeparator(text);

        double amount;
        try {
            amount = Double.parseDouble(amountToSend);
        } catch (NumberFormatException nfe) {
            amount = 0.0;
        }

        long amountL = BigDecimal.valueOf(monetaryUtil.getUndenominatedAmount(amount))
                .multiply(BigDecimal.valueOf(100000000))
                .longValue();
        return BigInteger.valueOf(amountL);
    }

    /**
     * Returns btc amount from satoshis.
     *
     * @return btc, mbtc or bits relative to what is set in monetaryUtil
     */
    private String getTextFromSatoshis(long satoshis) {
        String displayAmount = monetaryUtil.getDisplayAmount(satoshis);
        displayAmount = displayAmount.replace(".", getDefaultDecimalSeparator());
        return displayAmount;
    }

    private void setupTransaction(String amount,
                                  @FeeType.FeePriorityDef int feePriority,
                                  TransactionDataListener transactionDataListener) {
        ItemAccount selectedItem = getSendingItemAccount();
        setSendingAddress(selectedItem);
        calculateTransactionAmounts(selectedItem,
                amount,
                feePriority,
                transactionDataListener);
    }

    private void sendClicked(String address) {
        checkClipboardPaste(address);
        if (FormatsUtil.isValidBitcoinAddress(address)) {
            //Receiving address manual or scanned input
            sendModel.pendingTransaction.receivingAddress = address;
        } else {
            sendModel.recipient = address;
        }

        if (isValidSpend(sendModel.pendingTransaction, false)) {
            LegacyAddress legacyAddress = null;
            if (!sendModel.pendingTransaction.isHD()) {
                legacyAddress = ((LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject());
            }

            if (legacyAddress != null && legacyAddress.isWatchOnly() &&
                    (legacyAddress.getPrivateKey() == null || legacyAddress.getPrivateKey().isEmpty())) {
                if (getView() != null) {
                    getView().onShowSpendFromWatchOnly(((LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject()).getAddress());
                }
            } else if ((legacyAddress != null && legacyAddress.isWatchOnly()) || sendModel.verifiedSecondPassword != null) {
                confirmPayment();
            } else {
                getView().showSecondPasswordDialog();
            }
        }
    }

    void onNoSecondPassword() {
        confirmPayment();
    }

    void onSecondPasswordValidated(String secondPassword) {
        sendModel.verifiedSecondPassword = secondPassword;
        confirmPayment();
    }

    /**
     * Sets payment confirmation details to be displayed to user and fires callback to display
     * this.
     */
    @Thunk
    void confirmPayment() {
        if (fctxId != null) {
            contactsDataManager.getFacilitatedTransactions()
                    .filter(model -> model.getFacilitatedTransaction().getId().equals(fctxId))
                    .firstOrError()
                    .subscribe(contactTransactionModel -> {
                        if (getView() != null) getView().onShowPaymentDetails(
                                getConfirmationDetails(),
                                contactTransactionModel.getFacilitatedTransaction().getNote());
                    }, throwable -> {
                        Timber.e(throwable);
                        getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                        getView().finishPage(false);
                    });
        } else {
            if (getView() != null) getView().onShowPaymentDetails(getConfirmationDetails(), null);
        }
    }

    private PaymentConfirmationDetails getConfirmationDetails() {
        PendingTransaction pendingTransaction = sendModel.pendingTransaction;

        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        details.fromLabel = pendingTransaction.sendingObject.getLabel();
        if (contactMdid != null) {
            details.toLabel = sendModel.recipient;
        } else if (pendingTransaction.receivingObject != null
                && pendingTransaction.receivingObject.getLabel() != null
                && !pendingTransaction.receivingObject.getLabel().isEmpty()) {
            details.toLabel = pendingTransaction.receivingObject.getLabel();
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

        details.fiatSymbol = exchangeRateFactory.getSymbol(sendModel.fiatUnit);
        details.isLargeTransaction = isLargeTransaction();
        details.hasConsumedAmounts = pendingTransaction.unspentOutputBundle.getConsumedAmount().compareTo(BigInteger.ZERO) == 1;

        return details;
    }

    /**
     * Returns true if transaction is large by checking against 3 criteria:
     *
     * If the fee > $0.50
     * If the Tx size is over 1kB
     * If the ratio of fee/amount is over 1%
     */
    boolean isLargeTransaction() {
        String valueString = monetaryUtil.getFiatFormat("USD")
                .format(exchangeRateFactory.getLastPrice("USD") * sendModel.absoluteSuggestedFee.doubleValue() / 1e8);
        double usdValue = Double.parseDouble(stripSeparator(valueString));
        int txSize = sendDataManager.estimateSize(sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(), 2);//assume change
        double relativeFee = sendModel.absoluteSuggestedFee.doubleValue() / sendModel.pendingTransaction.bigIntAmount.doubleValue() * 100.0;

        return usdValue > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE;
    }

    /**
     * Various checks on validity of transaction details
     */
    private boolean isValidSpend(PendingTransaction pendingTransaction, boolean checkAmountsOnly) {
        // Validate amount
        if (!isValidAmount(pendingTransaction.bigIntAmount)) {
            if (getView() != null) getView().showInvalidAmount();
            return false;
        }

        // Validate sufficient funds
        if (pendingTransaction.unspentOutputBundle == null || pendingTransaction.unspentOutputBundle.getSpendableOutputs() == null) {
            showToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (sendModel.maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            if (getView() != null) {
                getView().showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
            }
            return false;
        }

        if (pendingTransaction.unspentOutputBundle == null) {
            showToast(R.string.no_confirmed_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (pendingTransaction.unspentOutputBundle.getSpendableOutputs().isEmpty()) {
            showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (!checkAmountsOnly) {
            // Validate addresses
            if (pendingTransaction.receivingAddress == null || !FormatsUtil.isValidBitcoinAddress(pendingTransaction.receivingAddress)) {
                showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR);
                return false;
            }

            // Validate send and receive not same addresses
            if (pendingTransaction.sendingObject == pendingTransaction.receivingObject) {
                showToast(R.string.send_to_same_address_warning, ToastCustom.TYPE_ERROR);
                return false;
            }

            if (sendModel.pendingTransaction.receivingObject != null
                    && sendModel.pendingTransaction.receivingObject.getAccountObject() == sendModel.pendingTransaction.sendingObject.getAccountObject()) {
                showToast(R.string.send_to_same_address_warning, ToastCustom.TYPE_ERROR);
                return false;
            }
        }

        return true;
    }

    void setSendingAddress(ItemAccount selectedItem) {
        sendModel.pendingTransaction.sendingObject = selectedItem;
    }

    ItemAccount getSendingItemAccount() {
        return sendModel.pendingTransaction.sendingObject;
    }

    /**
     * Set the receiving object. Null can be passed to reset receiving address for when user
     * customizes address
     */
    void setReceivingAddress(@Nullable ItemAccount selectedItem) {
        metricInputFlag = null;

        sendModel.pendingTransaction.receivingObject = selectedItem;
        if (selectedItem != null) {
            if (selectedItem.getAccountObject() instanceof Account) {
                //V3
                Account account = ((Account) selectedItem.getAccountObject());

                getCompositeDisposable().add(
                        payloadDataManager.getNextReceiveAddress(account)
                                .subscribe(
                                        address -> sendModel.pendingTransaction.receivingAddress = address,
                                        throwable -> showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

            } else if (selectedItem.getAccountObject() instanceof LegacyAddress) {
                //V2
                LegacyAddress legacyAddress = ((LegacyAddress) selectedItem.getAccountObject());
                sendModel.pendingTransaction.receivingAddress = legacyAddress.getAddress();

                if (legacyAddress.isWatchOnly())
                    if (legacyAddress.isWatchOnly() && prefsUtil.getValue(PREF_WARN_WATCH_ONLY_SPEND, true)) {
                        if (getView() != null) {
                            getView().onShowReceiveToWatchOnlyWarning(legacyAddress.getAddress());
                        }
                    }
            } else {
                //Address book
                AddressBook addressBook = ((AddressBook) selectedItem.getAccountObject());
                sendModel.pendingTransaction.receivingAddress = addressBook.getAddress();
            }

            metricInputFlag = EventService.EVENT_TX_INPUT_FROM_DROPDOWN;
        } else {
            sendModel.pendingTransaction.receivingAddress = "";
        }
    }

    private boolean isValidAmount(BigInteger bAmount) {
        if (bAmount == null) {
            return false;
        }

        // Test that amount is more than dust
        if (bAmount.compareTo(Payment.DUST) == -1) {
            return false;
        }

        // Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2_100_000_000_000_000L)) == 1) {
            if (getView() != null) getView().updateBtcAmount("0");
            return false;
        }

        // Test that amount is not zero
        return bAmount.compareTo(BigInteger.ZERO) >= 0;
    }

    /**
     * Executes transaction
     */
    void submitPayment() {
        String changeAddress;
        List<ECKey> keys = new ArrayList<>();
        Account account;
        LegacyAddress legacyAddress;
        try {
            if (sendModel.pendingTransaction.isHD()) {
                account = ((Account) sendModel.pendingTransaction.sendingObject.getAccountObject());
                changeAddress = payloadManager.getNextChangeAddress(account);

                if (payloadManager.getPayload().isDoubleEncryption()) {
                    payloadManager.getPayload()
                            .decryptHDWallet(0, sendModel.verifiedSecondPassword);
                }

                keys.addAll(payloadManager.getPayload().getHdWallets().get(0).getHDKeysForSigning(
                        account, sendModel.pendingTransaction.unspentOutputBundle));

            } else {
                legacyAddress = ((LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject());
                changeAddress = legacyAddress.getAddress();
                keys.add(payloadManager.getAddressECKey(legacyAddress, sendModel.verifiedSecondPassword));
            }

        } catch (Exception e) {
            Timber.e(e);
            getView().dismissProgressDialog();
            showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR);
            return;
        }

        getView().showProgressDialog(R.string.app_name);

        getCompositeDisposable().add(
                sendDataManager.submitPayment(
                        sendModel.pendingTransaction.unspentOutputBundle,
                        keys,
                        sendModel.pendingTransaction.receivingAddress,
                        changeAddress,
                        sendModel.pendingTransaction.bigIntFee,
                        sendModel.pendingTransaction.bigIntAmount)
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(
                                hash -> {
                                    Logging.INSTANCE.logCustom(new PaymentSentEvent()
                                            .putSuccess(true)
                                            .putAmountForRange(sendModel.pendingTransaction.bigIntAmount));

                                    clearUnspentResponseCache();
                                    getView().dismissConfirmationDialog();
                                    handleSuccessfulPayment(hash);
                                }, throwable -> {
                                    showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR);

                                    Logging.INSTANCE.logCustom(new PaymentSentEvent()
                                            .putSuccess(false)
                                            .putAmountForRange(sendModel.pendingTransaction.bigIntAmount));
                                }));
    }

    private void handleSuccessfulPayment(String hash) {
        insertPlaceHolderTransaction(hash, sendModel.pendingTransaction);

        if (sendModel.pendingTransaction.isHD()) {
            Account account = (Account) sendModel.pendingTransaction.sendingObject.getAccountObject();
            payloadDataManager.incrementChangeAddress(account);
            payloadDataManager.incrementReceiveAddress(account);
            updateInternalBalances();
        }
        if (getView() != null) {
            getView().onShowTransactionSuccess(contactMdid,
                    fctxId,
                    hash,
                    sendModel.pendingTransaction.bigIntAmount.longValue());

        }
        resetPresenterState();

        logAddressInputMetric();
    }

    /**
     * This is a precautionary measure in case the presenter isn't cleared from the app's lifecycle.
     */
    private void resetPresenterState() {
        sendModel = new SendModel();
        sendModel.pendingTransaction = new PendingTransaction();
        sendModel.unspentApiResponses = new HashMap<>();
        fctxId = null;
        contactMdid = null;
    }

    private void insertPlaceHolderTransaction(String hash, PendingTransaction pendingTransaction) {
        // After sending btc we create a "placeholder" tx until websocket handler refreshes list
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put(pendingTransaction.sendingObject.getLabel(), pendingTransaction.bigIntAmount);
        HashMap<String, BigInteger> outputs = new HashMap<>();

        String outLabel = pendingTransaction.receivingAddress;
        if (pendingTransaction.receivingObject != null && pendingTransaction.receivingObject.getLabel() != null) {
            outLabel = pendingTransaction.receivingObject.getLabel();
        }
        outputs.put(outLabel, pendingTransaction.bigIntAmount);

        TransactionSummary tx = new TransactionSummary();
        tx.setDirection(Direction.SENT);
        tx.setTime(System.currentTimeMillis() / 1000);
        tx.setTotal(pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee));
        tx.setHash(hash);
        tx.setFee(pendingTransaction.bigIntFee);
        tx.setInputsMap(inputs);
        tx.setOutputsMap(outputs);
        tx.setPending(true);

        transactionListDataManager.insertTransactionIntoListAndReturnSorted(tx);
    }

    void broadcastPaymentSuccess(String mdid, String txHash, String facilitatedTxId, long transactionValue) {
        getCompositeDisposable().add(
                // Get contacts
                contactsDataManager.getContactList()
                        // Find contact by MDID
                        .filter(ContactsPredicates.filterByMdid(mdid))
                        .singleOrError()
                        .flatMapCompletable(transaction -> {
                            // Broadcast payment to shared metadata service
                            return contactsDataManager.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
                                    // Show successfully broadcast
                                    .doOnComplete(() -> getView().showBroadcastSuccessDialog())
                                    // Log event
                                    .doOnComplete(() -> logContactsPayment())
                                    // Show retry dialog if broadcast failed
                                    .doOnError(throwable -> getView().showBroadcastFailedDialog(mdid, txHash, facilitatedTxId, transactionValue));
                        })
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.contacts_broadcasting_payment))
                        .subscribe(
                                () -> {
                                    // No-op
                                }, throwable -> {
                                    // Not sure if it's worth notifying people at this point? Dialogs are advisory anyway.
                                }));
    }

    private void logContactsPayment() {
        Logging.INSTANCE.logCustom(new ContactsEvent(ContactEventType.PAYMENT_BROADCASTED));
        metricInputFlag = EventService.EVENT_TX_INPUT_FROM_CONTACTS;
        logAddressInputMetric();
    }

    private void logAddressInputMetric() {
        EventService handler = new EventService(prefsUtil, new AuthService(new WalletApi()));
        if (metricInputFlag != null) handler.logAddressInputEvent(metricInputFlag);
    }

    private void checkClipboardPaste(String address) {
        String contents = getView().getClipboardContents();
        if (contents != null && contents.equals(address)) {
            metricInputFlag = EventService.EVENT_TX_INPUT_FROM_PASTE;
        }
    }

    private void clearUnspentResponseCache() {
        if (sendModel.pendingTransaction.isHD()) {
            Account account = ((Account) sendModel.pendingTransaction.sendingObject.getAccountObject());
            sendModel.unspentApiResponses.remove(account.getXpub());
        } else {
            LegacyAddress legacyAddress = ((LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject());
            sendModel.unspentApiResponses.remove(legacyAddress.getAddress());
        }
    }

    /**
     * Update balance immediately after spend - until refresh from server
     */
    private void updateInternalBalances() {
        try {
            BigInteger totalSent = sendModel.pendingTransaction.bigIntAmount.add(sendModel.pendingTransaction.bigIntFee);
            if (sendModel.pendingTransaction.isHD()) {
                Account account = (Account) sendModel.pendingTransaction.sendingObject.getAccountObject();
                payloadManager.subtractAmountFromAddressBalance(account.getXpub(), totalSent);
            } else {
                LegacyAddress address = (LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject();
                payloadManager.subtractAmountFromAddressBalance(address.getAddress(), totalSent);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    void handleScannedDataForWatchOnlySpend(String scanData) {
        try {
            final String format = privateKeyFactory.getFormat(scanData);
            if (format != null) {
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    spendFromWatchOnlyNonBIP38(format, scanData);
                } else {
                    //BIP38 needs passphrase
                    if (getView() != null) getView().onShowBIP38PassphrasePrompt(scanData);
                }
            } else {
                showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR);
            }

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void spendFromWatchOnlyNonBIP38(final String format, final String scanData) {
        try {
            ECKey key = privateKeyFactory.getKey(format, scanData);
            LegacyAddress legacyAddress = (LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject();
            setTempLegacyAddressPrivateKey(legacyAddress, key);

        } catch (Exception e) {
            showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
            Timber.e(e);
        }
    }

    void spendFromWatchOnlyBIP38(String pw, String scanData) {
        getCompositeDisposable().add(
                sendDataManager.getEcKeyFromBip38(pw, scanData, environmentSettings.getNetworkParameters())
                        .subscribe(ecKey -> {
                            LegacyAddress legacyAddress = (LegacyAddress) sendModel.pendingTransaction.sendingObject.getAccountObject();
                            setTempLegacyAddressPrivateKey(legacyAddress, ecKey);
                        }, throwable -> showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR)));
    }

    List<DisplayFeeOptions> getFeeOptionsForDropDown() {
        DisplayFeeOptions regular = new DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_regular),
                stringUtils.getString(R.string.fee_options_regular_time));
        DisplayFeeOptions priority = new DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_priority),
                stringUtils.getString(R.string.fee_options_priority_time));
        DisplayFeeOptions custom = new DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_custom),
                stringUtils.getString(R.string.fee_options_custom_warning));
        return Arrays.asList(regular, priority, custom);
    }

    FeeOptions getFeeOptions() {
        return dynamicFeeCache.getFeeOptions();
    }

    private void setTempLegacyAddressPrivateKey(LegacyAddress legacyAddress, ECKey key) {
        if (key != null && key.hasPrivKey() && legacyAddress.getAddress().equals(key.toAddress(
                environmentSettings.getNetworkParameters()).toString())) {

            //Create copy, otherwise pass by ref will override private key in wallet payload
            LegacyAddress tempLegacyAddress = new LegacyAddress();
            tempLegacyAddress.setPrivateKeyFromBytes(key.getPrivKeyBytes());
            tempLegacyAddress.setAddress(key.toAddress(environmentSettings.getNetworkParameters()).toString());
            tempLegacyAddress.setLabel(legacyAddress.getLabel());
            sendModel.pendingTransaction.sendingObject.setAccountObject(tempLegacyAddress);

            confirmPayment();
        } else {
            showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR);
        }
    }

    private void showToast(@StringRes int message, @ToastCustom.ToastType String type) {
        if (getView() != null) getView().showToast(message, type);
    }

    private String stripSeparator(String text) {
        return text.trim()
                .replace(" ", "")
                .replace(getDefaultDecimalSeparator(), ".");
    }

    void disableWatchOnlySpendWarning() {
        prefsUtil.setValue(PREF_WARN_WATCH_ONLY_SPEND, false);
    }

    boolean shouldShowAdvancedFeeWarning() {
        return prefsUtil.getValue(PREF_WARN_ADVANCED_FEE, true);
    }

    void disableAdvancedFeeWarning() {
        prefsUtil.setValue(PREF_WARN_ADVANCED_FEE, false);
    }

    BigInteger getFeePerKbFromPriority(@FeeType.FeePriorityDef int feePriorityTemp) {
        if (sendModel.feeOptions == null) {
            // This is a stopgap in case of failure to prevent crashes.
            return BigInteger.ZERO;
        }

        switch (feePriorityTemp) {
            case FeeType.FEE_OPTION_CUSTOM:
                return BigInteger.valueOf(getView().getCustomFeeValue() * 1000);
            case FeeType.FEE_OPTION_PRIORITY:
                return BigInteger.valueOf(sendModel.feeOptions.getPriorityFee() * 1000);
            case FeeType.FEE_OPTION_REGULAR:
            default:
                return BigInteger.valueOf(sendModel.feeOptions.getRegularFee() * 1000);
        }
    }

    interface TransactionDataListener {

        void onReady();

    }

}
