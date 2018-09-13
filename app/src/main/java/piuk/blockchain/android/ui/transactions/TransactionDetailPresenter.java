package piuk.blockchain.android.ui.transactions;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import info.blockchain.balance.CryptoCurrency;
import info.blockchain.balance.FiatValue;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.multiaddress.MultiAddressFactory;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;
import info.blockchain.wallet.util.FormatsUtil;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.apache.commons.lang3.tuple.Pair;
import piuk.blockchain.android.R;
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.contacts.models.ContactTransactionDisplayModel;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.androidcore.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager;
import piuk.blockchain.androidcore.data.currency.BTCDenomination;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.currency.ETHDenomination;
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.transactions.models.Displayable;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcoreui.ui.base.BasePresenter;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import timber.log.Timber;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings("WeakerAccess")
public class TransactionDetailPresenter extends BasePresenter<TransactionDetailView> {

    private TransactionHelper transactionHelper;
    private PayloadDataManager payloadDataManager;
    private StringUtils stringUtils;
    private TransactionListDataManager transactionListDataManager;
    private ExchangeRateDataManager exchangeRateFactory;
    private ContactsDataManager contactsDataManager;
    private EthDataManager ethDataManager;
    private BchDataManager bchDataManager;
    private EnvironmentConfig environmentSettings;
    private CurrencyFormatManager currencyFormatManager;

    private String fiatType;

    @VisibleForTesting
    Displayable displayable;

    @Inject
    public TransactionDetailPresenter(TransactionHelper transactionHelper,
                                      PrefsUtil prefsUtil,
                                      PayloadDataManager payloadDataManager,
                                      StringUtils stringUtils,
                                      TransactionListDataManager transactionListDataManager,
                                      ExchangeRateDataManager exchangeRateFactory,
                                      ContactsDataManager contactsDataManager,
                                      EthDataManager ethDataManager,
                                      BchDataManager bchDataManager,
                                      EnvironmentConfig environmentSettings,
                                      CurrencyFormatManager currencyFormatManager) {

        this.transactionHelper = transactionHelper;
        fiatType = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        this.payloadDataManager = payloadDataManager;
        this.stringUtils = stringUtils;
        this.transactionListDataManager = transactionListDataManager;
        this.exchangeRateFactory = exchangeRateFactory;
        this.contactsDataManager = contactsDataManager;
        this.ethDataManager = ethDataManager;
        this.bchDataManager = bchDataManager;
        this.environmentSettings = environmentSettings;
        this.currencyFormatManager = currencyFormatManager;
    }

    @Override
    public void onViewReady() {
        Intent pageIntent = getView().getPageIntent();
        if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)) {
            int transactionPosition = pageIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1);
            if (transactionPosition != -1) {
                displayable = transactionListDataManager.getTransactionList().get(transactionPosition);
                updateUiFromTransaction(displayable);
            } else {
                getView().pageFinish();
            }
        } else if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_HASH)) {
            getCompositeDisposable().add(
                    transactionListDataManager.getTxFromHash(pageIntent.getStringExtra(KEY_TRANSACTION_HASH))
                            .doOnSuccess(displayable -> this.displayable = displayable)
                            .subscribe(
                                    this::updateUiFromTransaction,
                                    throwable -> getView().pageFinish()));
        } else {
            Timber.e("Transaction hash not found");
            getView().pageFinish();
        }
    }

    void updateTransactionNote(String description) {
        Completable completable;
        if (displayable.getCryptoCurrency() == CryptoCurrency.BTC) {
            completable = payloadDataManager.updateTransactionNotes(displayable.getHash(), description);
        } else if (displayable.getCryptoCurrency() == CryptoCurrency.ETHER) {
            completable = ethDataManager.updateTransactionNotes(displayable.getHash(), description);
        } else {
            throw new IllegalArgumentException("Only BTC and ETHER currently supported");
        }

        completable.compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(() -> {
                    getView().showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                    getView().setDescription(description);
                }, throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR));
    }

    private void updateUiFromTransaction(Displayable displayable) {
        getView().setTransactionType(displayable.getDirection());
        setTransactionColor(displayable);
        setTransactionAmountInBtcOrEth(displayable.getCryptoCurrency(), displayable.getTotal());
        setConfirmationStatus(displayable.getCryptoCurrency(), displayable.getHash(), displayable.getConfirmations());
        setTransactionNote(displayable.getHash());
        setDate(displayable.getTimeStamp());
        setFee(displayable.getCryptoCurrency(), displayable.getFee());

        if (displayable.getCryptoCurrency() == CryptoCurrency.BTC) {
            handleBtcToAndFrom(displayable);
        } else if (displayable.getCryptoCurrency() == CryptoCurrency.ETHER) {
            handleEthToAndFrom(displayable);
        } else if (displayable.getCryptoCurrency() == CryptoCurrency.BCH) {
            handleBchToAndFrom(displayable);
        } else {
            throw new IllegalArgumentException(displayable.getCryptoCurrency() + " is not currently supported");
        }

        getCompositeDisposable().add(
                getTransactionValueString(fiatType, displayable)
                        .subscribe(
                                value -> getView().setTransactionValueFiat(value),
                                throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        getView().onDataLoaded();
        getView().setIsDoubleSpend(displayable.getDoubleSpend());
    }

    @SuppressWarnings("ConstantConditions")
    private void handleEthToAndFrom(Displayable displayable) {
        String fromAddress = displayable.getInputsMap().keySet().iterator().next();
        String toAddress = displayable.getOutputsMap().keySet().iterator().next();

        String ethAddress = ethDataManager.getEthResponseModel().getAddressResponse().getAccount();
        if (fromAddress.equals(ethAddress)) {
            fromAddress = stringUtils.getString(R.string.eth_default_account_label);
        }
        if (toAddress.equals(ethAddress)) {
            toAddress = stringUtils.getString(R.string.eth_default_account_label);
        }

        getView().setFromAddress(Collections.singletonList(new TransactionDetailModel(
                fromAddress, "", "")));
        getView().setToAddresses(Collections.singletonList(new TransactionDetailModel(
                toAddress, "", "")));
    }

    private void handleBtcToAndFrom(Displayable displayable) {
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> pair =
                transactionHelper.filterNonChangeAddresses(displayable);

        // From Addresses
        ArrayList<TransactionDetailModel> fromList = getFromList(displayable.getCryptoCurrency(), pair.getLeft());
        getView().setFromAddress(fromList);

        // From Contacts
        ContactTransactionDisplayModel displayModel = null;
        if (contactsDataManager.getTransactionDisplayMap().containsKey(displayable.getHash())) {
            displayModel = contactsDataManager.getTransactionDisplayMap().get(displayable.getHash());

            // Check if should be "Paid" state via contacts
            if (displayModel != null) {
                if (displayModel.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)
                        && displayModel.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    getView().showTransactionAsPaid();
                }
            }
        }

        // To Addresses
        ArrayList<TransactionDetailModel> recipients = getToList(displayable.getCryptoCurrency(), displayModel, pair.getRight());
        getView().setToAddresses(recipients);

        if (displayModel != null) {
            getView().setTransactionNote(displayModel.getNote());
        }
    }

    private void handleBchToAndFrom(Displayable displayable) {
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> pair =
                transactionHelper.filterNonChangeAddressesBch(displayable);

        // From Addresses
        ArrayList<TransactionDetailModel> fromList = getFromList(displayable.getCryptoCurrency(), pair.getLeft());
        getView().setFromAddress(fromList);

        // From Contacts
        ContactTransactionDisplayModel displayModel = null;
        if (contactsDataManager.getTransactionDisplayMap().containsKey(displayable.getHash())) {
            displayModel = contactsDataManager.getTransactionDisplayMap().get(displayable.getHash());

            // Check if should be "Paid" state via contacts
            if (displayModel != null) {
                if (displayModel.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)
                        && displayModel.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    getView().showTransactionAsPaid();
                }
            }
        }

        // To Addresses
        ArrayList<TransactionDetailModel> recipients = getToList(displayable.getCryptoCurrency(), displayModel, pair.getRight());
        getView().setToAddresses(recipients);

        if (displayModel != null) {
            getView().setTransactionNote(displayModel.getNote());
        }
    }

    private ArrayList<TransactionDetailModel> getFromList(CryptoCurrency currency, HashMap<String, BigInteger> inputMap) {
        ArrayList<TransactionDetailModel> inputs = new ArrayList<>();

        String unit = "";
        for (Map.Entry<String, BigInteger> item : inputMap.entrySet()) {

            long value = (item.getValue() != null) ? item.getValue().longValue() : 0;

            String label;
            if (currency == CryptoCurrency.BTC) {
                label = payloadDataManager.addressToLabel(item.getKey());
                unit = getDisplayUnitsBtc();
            } else {
                label = bchDataManager.getLabelFromBchAddress(item.getKey());
                unit = getDisplayUnitsBch();
                if (label == null) label = FormatsUtil.toShortCashAddress(
                        environmentSettings.getBitcoinCashNetworkParameters(),
                        item.getKey());
            }

            TransactionDetailModel transactionDetailModel = new TransactionDetailModel(
                    label,
                    currencyFormatManager.getFormattedSelectedCoinValue(BigInteger.valueOf(value)),
                    unit);

            if (transactionDetailModel.getAddress().equals(MultiAddressFactory.ADDRESS_DECODE_ERROR)) {
                transactionDetailModel.setAddress(stringUtils.getString(R.string.tx_decode_error));
                transactionDetailModel.setAddressDecodeError(true);
            }

            inputs.add(transactionDetailModel);
        }

        //No inputs = coinbase
        if (inputs.isEmpty()) {
            TransactionDetailModel coinbase = new TransactionDetailModel(
                    stringUtils.getString(R.string.transaction_detail_coinbase),
                    "",
                    unit);

            inputs.add(coinbase);
        }

        return inputs;
    }

    private ArrayList<TransactionDetailModel> getToList(CryptoCurrency currency,
                                                        ContactTransactionDisplayModel displayModel,
                                                        HashMap<String, BigInteger> outputMap) {
        ArrayList<TransactionDetailModel> recipients = new ArrayList<>();

        for (Map.Entry<String, BigInteger> item : outputMap.entrySet()) {

            long value = (item.getValue() != null) ? item.getValue().longValue() : 0;
            String unit;
            String label;

            if (currency == CryptoCurrency.BTC) {
                label = payloadDataManager.addressToLabel(item.getKey());
                unit = getDisplayUnitsBtc();
            } else {
                label = bchDataManager.getLabelFromBchAddress(item.getKey());
                if (label == null) label = FormatsUtil.toShortCashAddress(
                        environmentSettings.getBitcoinCashNetworkParameters(),
                        item.getKey());
                unit = getDisplayUnitsBch();
            }

            TransactionDetailModel transactionDetailModel = new TransactionDetailModel(
                    label,
                    currencyFormatManager.getFormattedSelectedCoinValue(BigInteger.valueOf(value)),
                    unit);

            if (displayModel != null && displayable.getDirection().equals(Direction.SENT)) {
                transactionDetailModel.setAddress(displayModel.getContactName());
            }

            if (transactionDetailModel.getAddress().equals(MultiAddressFactory.ADDRESS_DECODE_ERROR)) {
                transactionDetailModel.setAddress(stringUtils.getString(R.string.tx_decode_error));
                transactionDetailModel.setAddressDecodeError(true);
            }

            recipients.add(transactionDetailModel);
        }

        return recipients;
    }

    private void setFee(CryptoCurrency currency, BigInteger fee) {
        if (currency == CryptoCurrency.BTC) {
            String formattedFee = (
                    currencyFormatManager.getFormattedBtcValueWithUnit(
                            new BigDecimal(fee),
                            BTCDenomination.SATOSHI));
            getView().setFee(formattedFee);
        } else if (currency == CryptoCurrency.ETHER) {
            String formattedFee = (
                    currencyFormatManager.getFormattedEthShortValueWithUnit(
                            new BigDecimal(fee),
                            ETHDenomination.WEI));
            getView().setFee(formattedFee);
        } else if (currency == CryptoCurrency.BCH) {
            String formattedFee = (
                    currencyFormatManager.getFormattedBchValueWithUnit(
                            new BigDecimal(fee),
                            BTCDenomination.SATOSHI));
            getView().setFee(formattedFee);
        } else {
            throw new IllegalArgumentException(currency + " is not currently supported");
        }
    }

    private void setTransactionAmountInBtcOrEth(CryptoCurrency currency, BigInteger total) {
        if (currency == CryptoCurrency.ETHER) {
            String amountEth = (
                    currencyFormatManager.getFormattedEthShortValueWithUnit(
                            new BigDecimal(total.abs()), ETHDenomination.WEI));

            getView().setTransactionValueBtc(amountEth);
        } else if (currency == CryptoCurrency.BTC) {
            String amountBtc = (
                    currencyFormatManager.getFormattedBtcValueWithUnit(
                            new BigDecimal(total.abs()), BTCDenomination.SATOSHI));

            getView().setTransactionValueBtc(amountBtc);
        } else {
            String amountBch = (
                    currencyFormatManager.getFormattedBchValueWithUnit(
                            new BigDecimal(total.abs()), BTCDenomination.SATOSHI));

            getView().setTransactionValueBtc(amountBch);
        }
    }

    private void setTransactionNote(String txHash) {
        String notes;
        if (displayable.getCryptoCurrency() == CryptoCurrency.BTC) {
            notes = payloadDataManager.getTransactionNotes(txHash);
        } else if (displayable.getCryptoCurrency() == CryptoCurrency.ETHER) {
            notes = ethDataManager.getTransactionNotes(displayable.getHash());
        } else {
            //Only BTC and ETHER currently supported
            notes = "";
            getView().hideDescriptionField();
        }
        getView().setDescription(notes);
    }

    public String getTransactionNote() {
        if (displayable.getCryptoCurrency() == CryptoCurrency.BTC) {
            return payloadDataManager.getTransactionNotes(displayable.getHash());
        } else if (displayable.getCryptoCurrency() == CryptoCurrency.ETHER) {
            return ethDataManager.getTransactionNotes(displayable.getHash());
        } else {
            // Currently no available notes for bch
            return "";
        }
    }

    public String getTransactionHash() {
        return displayable.getHash();
    }

    public CryptoCurrency getTransactionType() {
        return displayable.getCryptoCurrency();
    }

    @VisibleForTesting
    void setConfirmationStatus(CryptoCurrency cryptoCurrency, String txHash, long confirmations) {
        if (confirmations >= cryptoCurrency.getRequiredConfirmations()) {
            getView().setStatus(cryptoCurrency, stringUtils.getString(R.string.transaction_detail_confirmed), txHash);
        } else {
            String pending = stringUtils.getString(R.string.transaction_detail_pending);
            pending = String.format(Locale.getDefault(), pending, confirmations, cryptoCurrency.getRequiredConfirmations());
            getView().setStatus(cryptoCurrency, pending, txHash);
        }
    }

    private void setDate(long time) {
        long epochTime = time * 1000;

        Date date = new Date(epochTime);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG);
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String dateText = dateFormat.format(date);
        String timeText = timeFormat.format(date);

        getView().setDate(dateText + " @ " + timeText);
    }

    @VisibleForTesting
    void setTransactionColor(Displayable transaction) {
        if (transaction.getDirection() == Direction.TRANSFERRED) {
            getView().setTransactionColour(transaction.getConfirmations() < transaction.getCryptoCurrency().getRequiredConfirmations()
                    ? R.color.product_gray_transferred_50 : R.color.product_gray_transferred);
        } else if (transaction.getDirection() == Direction.SENT) {
            getView().setTransactionColour(transaction.getConfirmations() < transaction.getCryptoCurrency().getRequiredConfirmations()
                    ? R.color.product_red_sent_50 : R.color.product_red_sent);
        } else {
            getView().setTransactionColour(transaction.getConfirmations() < transaction.getCryptoCurrency().getRequiredConfirmations()
                    ? R.color.product_green_received_50 : R.color.product_green_received);
        }
    }

    @VisibleForTesting
    Observable<String> getTransactionValueString(String currency, Displayable transaction) {
        if (transaction.getCryptoCurrency() == CryptoCurrency.BTC) {
            return exchangeRateFactory.getBtcHistoricPrice(
                    transaction.getTotal().longValue(),
                    currency,
                    transaction.getTimeStamp())
                    .map(aDouble -> getTransactionString(transaction, aDouble));
        } else if (transaction.getCryptoCurrency() == CryptoCurrency.BCH) {
            return exchangeRateFactory.getBchHistoricPrice(
                    transaction.getTotal().longValue(),
                    currency,
                    transaction.getTimeStamp())
                    .map(aDouble -> getTransactionString(transaction, aDouble));
        } else {
            return exchangeRateFactory.getEthHistoricPrice(
                    transaction.getTotal(),
                    currency,
                    transaction.getTimeStamp())
                    .map(aDouble -> getTransactionString(transaction, aDouble));
        }
    }

    @NonNull
    private String getTransactionString(Displayable transaction, BigDecimal aDouble) {
        int stringId = -1;
        switch (transaction.getDirection()) {
            case TRANSFERRED:
                stringId = R.string.transaction_detail_value_at_time_transferred;
                break;
            case SENT:
                stringId = R.string.transaction_detail_value_at_time_sent;
                break;
            case RECEIVED:
                stringId = R.string.transaction_detail_value_at_time_received;
                break;
        }
        return stringUtils.getString(stringId) +
                FiatValue.fromMajor(fiatType, aDouble).toStringWithSymbol(Locale.getDefault());
    }

    private String getDisplayUnitsBtc() {
        return CryptoCurrency.BTC.name();
    }

    private String getDisplayUnitsBch() {
        return CryptoCurrency.BCH.name();
    }
}
