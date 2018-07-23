package piuk.blockchain.android.ui.contacts.detail;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.data.Account;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel;
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.androidcoreui.ui.base.BasePresenter;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager;
import piuk.blockchain.androidcore.data.contacts.ContactsPredicates;
import piuk.blockchain.androidcore.data.contacts.comparators.FctxDateComparator;
import piuk.blockchain.androidcore.data.currency.BTCDenomination;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.currency.CurrencyState;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import timber.log.Timber;

import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.KEY_BUNDLE_CONTACT_ID;

public class ContactDetailPresenter extends BasePresenter<ContactDetailView> {

    private Observable<NotificationPayload> notificationObservable;
    @VisibleForTesting List<Object> displayList = new ArrayList<>();
    @VisibleForTesting Contact contact;
    private ContactsDataManager contactsDataManager;
    private PayloadDataManager payloadDataManager;
    private PrefsUtil prefsUtil;
    private RxBus rxBus;
    private TransactionListDataManager transactionListDataManager;
    private CurrencyState currencyState;
    private CurrencyFormatManager currencyFormatManager;

    @Inject
    ContactDetailPresenter(ContactsDataManager contactsDataManager,
                           PayloadDataManager payloadDataManager,
                           PrefsUtil prefsUtil,
                           RxBus rxBus,
                           TransactionListDataManager transactionListDataManager,
                           CurrencyState currencyState,
                           CurrencyFormatManager currencyFormatManager) {

        this.contactsDataManager = contactsDataManager;
        this.payloadDataManager = payloadDataManager;
        this.prefsUtil = prefsUtil;
        this.rxBus = rxBus;
        this.transactionListDataManager = transactionListDataManager;
        this.currencyState = currencyState;
        this.currencyFormatManager = currencyFormatManager;
    }

    @Override
    public void onViewReady() {
        subscribeToNotifications();
        setupViewModel();
    }

    PrefsUtil getPrefsUtil() {
        return prefsUtil;
    }

    Map<String, ContactTransactionDisplayModel> getTransactionDisplayMap() {
        return contactsDataManager.getTransactionDisplayMap();
    }

    void onDeleteContactClicked() {
        getView().showDeleteUserDialog();
    }

    void onDeleteContactConfirmed() {
        getView().showProgressDialog();
        getCompositeDisposable().add(
                contactsDataManager.removeContact(contact)
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(() -> {
                            // Quit page, show toast
                            getView().showToast(R.string.contacts_delete_contact_success, ToastCustom.TYPE_OK);
                            getView().finishPage();
                        }, throwable -> getView().showToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR)));
    }

    void onRenameContactClicked() {
        getView().showRenameDialog(contact.getName());
    }

    void onContactRenamed(String name) {
        //noinspection StatementWithEmptyBody
        if (name.equals(contact.getName())) {
            // No problem here
        } else if (name.isEmpty()) {
            getView().showToast(R.string.contacts_rename_invalid_name, ToastCustom.TYPE_ERROR);
        } else {
            getView().showProgressDialog();

            getCompositeDisposable().add(
                    contactsDataManager.renameContact(contact.getId(), name)
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(
                                    () -> {
                                        onViewReady();
                                        getView().showToast(R.string.contacts_rename_success, ToastCustom.TYPE_OK);
                                    },
                                    throwable -> getView().showToast(R.string.contacts_rename_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    void onTransactionClicked(String fctxId) {
        FacilitatedTransaction transaction = contact.getFacilitatedTransactions().get(fctxId);

        if (transaction == null) {
            getView().showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR);
        } else {

            // Payment request sent, waiting for address from recipient
            if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {

                getView().showWaitingForAddressDialog();

                // Payment request sent, waiting for payment
            } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {

                getView().showWaitingForPaymentDialog();

                // Received payment request, need to send address to sender
            } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {

                List<String> accountNames = new ArrayList<>();
                //noinspection Convert2streamapi
                for (Account account : payloadDataManager.getWallet().getHdWallets().get(0).getAccounts()) {
                    if (!account.isArchived()) {
                        accountNames.add(account.getLabel());
                    }
                }

                if (accountNames.size() == 1) {
                    // Only one account, ask if you want to send an address
                    getView().showSendAddressDialog(fctxId);
                } else {
                    // Show dialog allowing user to select which account they want to use
                    getView().showAccountChoiceDialog(accountNames, fctxId);
                }

                // Waiting for payment
            } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {

                getView().initiatePayment(
                        transaction.toBitcoinURI(),
                        contact.getId(),
                        contact.getMdid(),
                        transaction.getId());

            } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {

                getView().showPayOrDeclineDialog(fctxId,
                        getBalanceString(transaction.getIntendedAmount()),
                        contact.getName(),
                        transaction.getNote());
            }
        }
    }

    void onCompletedTransactionClicked(int position) {
        if (displayList.get(position) instanceof TransactionSummary) {
            TransactionSummary summary = (TransactionSummary) displayList.get(position);
            getView().showTransactionDetail(summary.getHash());
        }
    }

    void declineTransaction(String fctxId) {
        getView().showTransactionDeclineDialog(fctxId);
    }

    void onTransactionLongClicked(String fctxId) {
        getCompositeDisposable().add(
                contactsDataManager.getFacilitatedTransactions()
                        .filter(contactTransactionModel -> contactTransactionModel.getFacilitatedTransaction().getId().equals(fctxId))
                        .subscribe(contactTransactionModel -> {
                            FacilitatedTransaction fctx = contactTransactionModel.getFacilitatedTransaction();

                            if (fctx.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)
                                    && fctx.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {
                                getView().showTransactionCancelDialog(fctxId);

                            } else if (fctx.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                                    && fctx.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {
                                getView().showTransactionCancelDialog(fctxId);
                            }
                        }, Timber::e));
    }

    void confirmDeclineTransaction(String fctxId) {
        getCompositeDisposable().add(
                contactsDataManager.getContactFromFctxId(fctxId)
                        .flatMapCompletable(contact -> contactsDataManager.sendPaymentDeclinedResponse(contact.getMdid(), fctxId))
                        .doOnError(throwable -> contactsDataManager.fetchContacts())
                        .doAfterTerminate(this::setupViewModel)
                        .subscribe(
                                () -> getView().showToast(R.string.contacts_pending_transaction_decline_success, ToastCustom.TYPE_OK),
                                throwable -> getView().showToast(R.string.contacts_pending_transaction_decline_failure, ToastCustom.TYPE_ERROR)));
    }

    void confirmCancelTransaction(String fctxId) {
        getCompositeDisposable().add(
                contactsDataManager.getContactFromFctxId(fctxId)
                        .flatMapCompletable(contact -> contactsDataManager.sendPaymentCancelledResponse(contact.getMdid(), fctxId))
                        .doOnError(throwable -> contactsDataManager.fetchContacts())
                        .doAfterTerminate(this::setupViewModel)
                        .subscribe(
                                () -> getView().showToast(R.string.contacts_pending_transaction_cancel_success, ToastCustom.TYPE_OK),
                                throwable -> getView().showToast(R.string.contacts_pending_transaction_cancel_failure, ToastCustom.TYPE_ERROR)));
    }

    void onAccountChosen(int accountPosition, String fctxId) {
        getView().showProgressDialog();
        FacilitatedTransaction transaction = contact.getFacilitatedTransactions().get(fctxId);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setIntendedAmount(transaction.getIntendedAmount());
        paymentRequest.setId(fctxId);

        getCompositeDisposable().add(
                payloadDataManager.getNextReceiveAddressAndReserve(payloadDataManager.getPositionOfAccountInActiveList(accountPosition), "Payment request " + fctxId)
                        .doOnNext(paymentRequest::setAddress)
                        .flatMapCompletable(s -> contactsDataManager.sendPaymentRequestResponse(contact.getMdid(), paymentRequest, fctxId))
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(
                                () -> {
                                    getView().showToast(R.string.contacts_address_sent_success, ToastCustom.TYPE_OK);
                                    setupViewModel();
                                },
                                throwable -> getView().showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)));

    }

    void onPaymentRequestAccepted(String fctxId) {
        contactsDataManager.getContactFromFctxId(fctxId)
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .subscribe(contact -> {
                    FacilitatedTransaction transaction = contact.getFacilitatedTransactions().get(fctxId);
                    if (transaction == null) {
                        getView().showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR);
                    } else {
                        // Need to send payment to recipient
                        getView().initiatePayment(transaction.toBitcoinURI(),
                                contact.getId(),
                                contact.getMdid(),
                                transaction.getId());
                    }
                }, throwable -> {
                    Timber.e(throwable);
                    getView().showToast(
                            R.string.contacts_not_found_error,
                            ToastCustom.TYPE_ERROR);
                });
    }


    void onBtcFormatChanged(boolean isBtc) {
        currencyState.setDisplayingCryptoCurrency(isBtc);
    }

    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);
        getCompositeDisposable().add(
                notificationObservable
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> {
                                    if (notificationPayload.getType() != null
                                            && notificationPayload.getType().equals(NotificationPayload.NotificationType.PAYMENT)) {
                                        setupViewModel();
                                    }
                                },
                                Timber::e));
    }

    private void setupViewModel() {
        Bundle bundle = getView().getPageBundle();
        if (bundle != null && bundle.getString(KEY_BUNDLE_CONTACT_ID) != null) {
            String id = bundle.getString(KEY_BUNDLE_CONTACT_ID);

            getCompositeDisposable().add(
                    // Get contacts list
                    contactsDataManager.getContactList()
                            // Find current contact
                            .filter(ContactsPredicates.filterById(id))
                            // Shouldn't be necessary but checks for only one value and returns a Single
                            .firstOrError()
                            // Update UI
                            .doOnSuccess(contact -> {
                                this.contact = contact;
                                getView().updateContactName(contact.getName());
                                sortAndUpdateTransactions(contact.getFacilitatedTransactions().values());
                            })
                            // Contact not found, quit page
                            .doOnError(throwable -> showErrorAndQuitPage())
                            // Update contacts in case of new FacilitatedTransactions
                            .flatMapCompletable(contact -> contactsDataManager.fetchContacts())
                            .subscribe(
                                    // Update with FacilitatedTransactions, UI handles diff
                                    () -> sortAndUpdateTransactions(contact.getFacilitatedTransactions().values()),
                                    // Show error if updating contacts failed
                                    throwable -> getView().showToast(R.string.contacts_digesting_messages_failed, ToastCustom.TYPE_ERROR)));
        } else {
            showErrorAndQuitPage();
        }
    }

    @VisibleForTesting
    void sortAndUpdateTransactions(Collection<FacilitatedTransaction> values) {
        List<FacilitatedTransaction> facilitatedTransactions = new ArrayList<>(values);
        Collections.sort(facilitatedTransactions, new FctxDateComparator());
        Collections.reverse(facilitatedTransactions);
        displayList.clear();

        for (FacilitatedTransaction fctx : facilitatedTransactions) {
            if (fctx.getTxHash() != null && !fctx.getTxHash().isEmpty()) {
                // Do something
                TransactionSummary summary = new TransactionSummary();
                summary.setHash(fctx.getTxHash());
                summary.setTime(fctx.getLastUpdated());
                summary.setTotal(BigInteger.valueOf(fctx.getIntendedAmount()));
                if (transactionListDataManager.getTxConfirmationsMap().containsKey(summary.getHash())) {
                    summary.setConfirmations(transactionListDataManager.getTxConfirmationsMap().get(summary.getHash()));
                } else {
                    // Assume confirmed
                    summary.setConfirmations(CryptoCurrency.BTC.getRequiredConfirmations());
                }

                if (fctx.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)
                        || fctx.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    summary.setDirection(TransactionSummary.Direction.SENT);
                } else {
                    summary.setDirection(TransactionSummary.Direction.RECEIVED);
                }
                displayList.add(summary);
            } else {
                // Do something else
                displayList.add(new ContactTransactionModel(contact.getName(), fctx));
            }

        }

        getView().onTransactionsUpdated(displayList, currencyState.isDisplayingCryptoCurrency());
    }

    private String getBalanceString(long btcBalance) {
        return currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(
                BigDecimal.valueOf(btcBalance), null, BTCDenomination.SATOSHI);
    }

    private void showErrorAndQuitPage() {
        getView().showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
        getView().finishPage();
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
    }
}
