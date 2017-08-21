package piuk.blockchain.android.ui.transactions;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings("WeakerAccess")
public class TransactionDetailPresenter extends BasePresenter<TransactionDetailView> {

    private static final int REQUIRED_CONFIRMATIONS = 3;

    private MonetaryUtil mMonetaryUtil;
    private TransactionHelper transactionHelper;
    private PrefsUtil mPrefsUtil;
    private PayloadDataManager mPayloadDataManager;
    private StringUtils mStringUtils;
    private TransactionListDataManager mTransactionListDataManager;
    private ExchangeRateFactory mExchangeRateFactory;
    private ContactsDataManager mContactsDataManager;

    private String mFiatType;

    @VisibleForTesting TransactionSummary mTransaction;

    @Inject
    public TransactionDetailPresenter(TransactionHelper transactionHelper,
                                      PrefsUtil mPrefsUtil,
                                      PayloadDataManager mPayloadDataManager,
                                      StringUtils mStringUtils,
                                      TransactionListDataManager mTransactionListDataManager,
                                      ExchangeRateFactory mExchangeRateFactory,
                                      ContactsDataManager mContactsDataManager) {

        this.transactionHelper = transactionHelper;
        mMonetaryUtil = new MonetaryUtil(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        mFiatType = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        this.mPrefsUtil = mPrefsUtil;
        this.mPayloadDataManager = mPayloadDataManager;
        this.mStringUtils = mStringUtils;
        this.mTransactionListDataManager = mTransactionListDataManager;
        this.mExchangeRateFactory = mExchangeRateFactory;
        this.mContactsDataManager = mContactsDataManager;
    }

    @Override
    public void onViewReady() {
        Intent pageIntent = getView().getPageIntent();
        if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)) {
            int transactionPosition = pageIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1);
            if (transactionPosition != -1) {
                mTransaction = mTransactionListDataManager.getTransactionList().get(transactionPosition);
                updateUiFromTransaction(mTransaction);
            } else {
                getView().pageFinish();
            }
        } else if (pageIntent != null && pageIntent.hasExtra(KEY_TRANSACTION_HASH)) {
            getCompositeDisposable().add(
                    mTransactionListDataManager.getTxFromHash(pageIntent.getStringExtra(KEY_TRANSACTION_HASH))
                            .subscribe(
                                    this::updateUiFromTransaction,
                                    throwable -> getView().pageFinish()));
        } else {
            getView().pageFinish();
        }
    }

    public void updateTransactionNote(String description) {
        getCompositeDisposable().add(
                mPayloadDataManager.updateTransactionNotes(mTransaction.getHash(), description)
                        .subscribe(() -> {
                            getView().showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            getView().setDescription(description);
                        }, throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }

    private void updateUiFromTransaction(TransactionSummary transactionSummary) {
        getView().setTransactionType(transactionSummary.getDirection());
        setTransactionColor(transactionSummary);
        setTransactionAmountInBtc(transactionSummary.getTotal());
        setConfirmationStatus(transactionSummary.getHash(), transactionSummary.getConfirmations());
        setTransactionNote(transactionSummary.getHash());
        setDate(transactionSummary.getTime());
        setFee(transactionSummary.getFee());

        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> pair =
                transactionHelper.filterNonChangeAddresses(transactionSummary);

        // From Address
        HashMap<String, BigInteger> inputMap = pair.getLeft();
        ArrayList<String> labelList = new ArrayList<>();
        Set<Entry<String, BigInteger>> entrySet = inputMap.entrySet();
        for (Entry<String, BigInteger> set : entrySet) {
            String label = mPayloadDataManager.addressToLabel(set.getKey());
            if (!labelList.contains(label)) labelList.add(label);
        }

        String inputMapString = org.apache.commons.lang3.StringUtils.join(labelList.toArray(), "\n\n");
        if (inputMapString.isEmpty()) {
            inputMapString = mStringUtils.getString(R.string.transaction_detail_coinbase);
        }

        ContactTransactionDisplayModel displayModel = null;

        if (mContactsDataManager.getTransactionDisplayMap().containsKey(transactionSummary.getHash())) {
            displayModel =
                    mContactsDataManager.getTransactionDisplayMap().get(transactionSummary.getHash());

            inputMapString = displayModel.getContactName();
        }

        // TODO: 14/03/2017 Change this to dropdown like outputs, as a list of addresses looks terrible
        getView().setFromAddress(inputMapString);

        // Check if should be "Paid" state via contacts
        if (displayModel != null) {
            if (displayModel.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)
                    && displayModel.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                getView().showTransactionAsPaid();
            }
        }

        // To Address
        HashMap<String, BigInteger> outputMap = pair.getRight();
        ArrayList<RecipientModel> recipients = new ArrayList<>();

        for (Entry<String, BigInteger> item : outputMap.entrySet()) {
            RecipientModel recipientModel = new RecipientModel(
                    mPayloadDataManager.addressToLabel(item.getKey()),
                    mMonetaryUtil.getDisplayAmountWithFormatting(item.getValue().longValue()),
                    getDisplayUnits());

            if (displayModel != null && transactionSummary.getDirection().equals(Direction.SENT)) {
                recipientModel.setAddress(displayModel.getContactName());
            }

            recipients.add(recipientModel);
        }

        getView().setToAddresses(recipients);

        if (displayModel != null) {
            getView().setTransactionNote(displayModel.getNote());
        }

        getCompositeDisposable().add(
                getTransactionValueString(mFiatType, transactionSummary)
                        .subscribe(
                                value -> getView().setTransactionValueFiat(value),
                                throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        getView().onDataLoaded();
        getView().setIsDoubleSpend(transactionSummary.isDoubleSpend());
    }

    private void setFee(BigInteger fee) {
        String formattedFee = (mMonetaryUtil.getDisplayAmountWithFormatting(fee.longValue()) + " " + getDisplayUnits());
        getView().setFee(formattedFee);
    }

    private void setTransactionAmountInBtc(BigInteger total) {
        String amountBtc = (
                mMonetaryUtil.getDisplayAmountWithFormatting(
                        total.abs().longValue())
                        + " "
                        + getDisplayUnits());

        getView().setTransactionValueBtc(amountBtc);
    }

    private void setTransactionNote(String txHash) {
        String notes = mPayloadDataManager.getTransactionNotes(txHash);
        getView().setDescription(notes);
    }

    public String getTransactionNote() {
        return mPayloadDataManager.getTransactionNotes(mTransaction.getHash());
    }

    public String getTransactionHash() {
        return mTransaction.getHash();
    }

    @VisibleForTesting
    void setConfirmationStatus(String txHash, long confirmations) {
        if (confirmations >= REQUIRED_CONFIRMATIONS) {
            getView().setStatus(mStringUtils.getString(R.string.transaction_detail_confirmed), txHash);
        } else {
            String pending = mStringUtils.getString(R.string.transaction_detail_pending);
            pending = String.format(Locale.getDefault(), pending, confirmations, REQUIRED_CONFIRMATIONS);
            getView().setStatus(pending, txHash);
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
    void setTransactionColor(TransactionSummary transaction) {
        if (transaction.getDirection() == Direction.TRANSFERRED) {
            getView().setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.product_gray_transferred_50 : R.color.product_gray_transferred);
        } else if (transaction.getDirection() == Direction.SENT) {
            getView().setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.product_red_sent_50 : R.color.product_red_sent);
        } else {
            getView().setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.product_green_received_50 : R.color.product_green_received);
        }
    }

    @VisibleForTesting
    Observable<String> getTransactionValueString(String currency, TransactionSummary transaction) {
        return mExchangeRateFactory.getHistoricPrice(
                transaction.getTotal().abs().longValue(),
                currency,
                transaction.getTime() * 1000)
                .map(aDouble -> {
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
                    return mStringUtils.getString(stringId)
                            + mExchangeRateFactory.getSymbol(mFiatType)
                            + mMonetaryUtil.getFiatFormat(mFiatType).format(aDouble);
                });
    }

    private String getDisplayUnits() {
        return mMonetaryUtil.getBtcUnits()[mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

}
