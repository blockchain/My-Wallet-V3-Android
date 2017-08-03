package piuk.blockchain.android.ui.contacts.list;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.commons.lang3.NotImplementedException;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.exceptions.DecryptionException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import timber.log.Timber;

public class ContactsListPresenter extends BasePresenter<ContactsListView> {

    private Observable<NotificationPayload> notificationObservable;
    @VisibleForTesting String link;
    private ContactsDataManager contactsDataManager;
    private PayloadDataManager payloadDataManager;
    private RxBus rxBus;
    @VisibleForTesting TreeMap<String, Contact> contactList;

    @VisibleForTesting Contact recipient;
    @VisibleForTesting Contact sender;
    @VisibleForTesting String uri;

    @Inject
    ContactsListPresenter(ContactsDataManager contactsDataManager,
                          PayloadDataManager payloadDataManager,
                          RxBus rxBus) {
        this.contactsDataManager = contactsDataManager;
        this.payloadDataManager = payloadDataManager;
        this.rxBus = rxBus;
    }

    @Override
    public void onViewReady() {
        // Subscribe to notification events
        subscribeToNotifications();

        Intent intent = getView().getPageIntent();
        if (intent != null && intent.hasExtra(ContactsListActivity.EXTRA_METADATA_URI)) {
            link = intent.getStringExtra(ContactsListActivity.EXTRA_METADATA_URI);
            intent.removeExtra(ContactsListActivity.EXTRA_METADATA_URI);
        }

        // Set up page if Metadata initialized
        attemptPageSetup(true);
    }

    void initContactsService(@Nullable String secondPassword) {
        getView().setUiState(UiState.LOADING);
        getCompositeDisposable().add(
                payloadDataManager.generateNodes(secondPassword)
                        .andThen(payloadDataManager.getMetadataNodeFactory())
                        .flatMapCompletable(metadataNodeFactory -> contactsDataManager.initContactsService(
                                metadataNodeFactory.getMetadataNode(),
                                metadataNodeFactory.getSharedMetadataNode()))
                        .subscribe(this::registerMdid,
                                throwable -> {
                                    getView().setUiState(UiState.FAILURE);
                                    if (throwable instanceof DecryptionException) {
                                        getView().showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR);
                                    } else {
                                        getView().showToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
                                    }
                                }));
    }

    // TODO: 30/03/2017 Move this into the registerNodeForMetaDataService function
    private void registerMdid() {
        getCompositeDisposable().add(
                payloadDataManager.registerMdid()
                        .flatMapCompletable(responseBody -> contactsDataManager.publishXpub())
                        .subscribe(
                                () -> attemptPageSetup(false),
                                throwable -> {
                                    getView().setUiState(UiState.FAILURE);
                                    getView().showToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
                                }));
    }

    @VisibleForTesting
    void refreshContacts() {
        getView().setUiState(UiState.LOADING);
        getCompositeDisposable().add(
                contactsDataManager.fetchContacts()
                        .andThen(contactsDataManager.getContactList())
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> getView().setUiState(UiState.FAILURE)));
    }

    private void loadContacts() {
        getView().setUiState(UiState.LOADING);
        getCompositeDisposable().add(
                contactsDataManager.getContactList()
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> getView().setUiState(UiState.FAILURE)));
    }

    // TODO: 19/01/2017 This is pretty gross and I'm certain it can be Rx-ified in the future
    private void handleContactListUpdate(List<Contact> contacts) {
        List<ContactsListItem> list = new ArrayList<>();
        List<Contact> pending = new ArrayList<>();

        getCompositeDisposable().add(
                contactsDataManager.getContactsWithUnreadPaymentRequests()
                        .toList()
                        .subscribe(actionRequired -> {
                            contactList = new TreeMap<>();
                            for (Contact contact : contacts) {
                                contactList.put(contact.getId(), contact);

                                list.add(new ContactsListItem(
                                        contact.getId(),
                                        contact.getName(),
                                        contact.getMdid() != null && !contact.getMdid().isEmpty()
                                                ? ContactsListItem.Status.TRUSTED
                                                : ContactsListItem.Status.PENDING,
                                        contact.getCreated(),
                                        isInList(actionRequired, contact)));

                                if (contact.getMdid() == null || contact.getMdid().isEmpty()) {
                                    pending.add(contact);
                                }
                            }

                            checkStatusOfPendingContacts(pending);
                            updateUI(list);

                        }, throwable -> {
                            getView().onContactsLoaded(new ArrayList<>());
                            getView().setUiState(UiState.FAILURE);
                        }));
    }

    private void updateUI(List<ContactsListItem> list) {
        if (!list.isEmpty()) {
            getView().setUiState(UiState.CONTENT);
            getView().onContactsLoaded(list);
        } else {
            getView().onContactsLoaded(new ArrayList<>());
            getView().setUiState(UiState.EMPTY);
        }
    }

    private boolean isInList(List<Contact> contacts, Contact toBeFound) {
        for (Contact contact : contacts) {
            if (contact.getId().equals(toBeFound.getId())) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    void checkStatusOfPendingContacts(List<Contact> pending) {
        for (int i = 0; i < pending.size(); i++) {
            final Contact contact = pending.get(i);
            getCompositeDisposable().add(
                    contactsDataManager.readInvitationSent(contact)
                            .subscribe(
                                    success -> {
                                        if (success) loadContacts();
                                    },
                                    throwable -> {
                                        // No-op
                                    }));
        }
    }

    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);

        getCompositeDisposable().add(
                notificationObservable
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> refreshContacts(),
                                Timber::e));
        new NoSuchElementException("");
    }

    @VisibleForTesting
    void handleLink(String data) {
        getView().showProgressDialog();
        getCompositeDisposable().add(
                contactsDataManager.acceptInvitation(data)
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(
                                contact -> {
                                    link = null;
                                    refreshContacts();
                                    getView().showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
                                }, throwable -> {

                                    if(throwable instanceof NoSuchElementException) {
                                        getView().showToast(R.string.contacts_invite_uri_used, ToastCustom.TYPE_ERROR);
                                    } else {
                                        getView().showToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR);
                                    }
                                    link = null;
                                    refreshContacts();

                                }));
    }

    private void attemptPageSetup(boolean firstAttempt) {
        if (firstAttempt) {
            getView().setUiState(UiState.LOADING);
            getCompositeDisposable().add(
                    payloadDataManager.loadNodes()
                            .subscribe(
                                    success -> {
                                        if (success) {
                                            if (link != null) {
                                                handleLink(link);
                                            } else {
                                                refreshContacts();
                                            }
                                        } else {
                                            // Not set up, most likely has a second password enabled
                                            if (payloadDataManager.isDoubleEncrypted()) {
                                                getView().showSecondPasswordDialog();
                                                getView().setUiState(UiState.FAILURE);
                                            } else {
                                                initContactsService(null);
                                            }
                                        }
                                    }, throwable -> getView().setUiState(UiState.FAILURE)));
        } else {
            getView().setUiState(UiState.FAILURE);
        }
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
    }

    void setNameOfSender(String nameOfSender) {
        sender = new Contact();
        sender.setName(nameOfSender);
    }

    void setNameOfRecipient(String nameOfRecipient) {
        recipient = new Contact();
        recipient.setName(nameOfRecipient);
    }

    String getRecipient() {
        return recipient.getName();
    }

    void clearContactNames() {
        recipient = null;
        sender = null;
    }

    void createLink() {
        if (uri == null) {
            getView().showProgressDialog();

            getCompositeDisposable().add(
                    contactsDataManager.publishXpub()
                            .andThen(contactsDataManager.createInvitation(sender, recipient))
                            .map(Contact::createURI)
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(
                                    uri -> {
                                        this.uri = uri;
                                        generateIntent(uri);
                                    },
                                    throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
        } else {
            // Prevents contact being added more than once, as well as unnecessary web calls
            generateIntent(uri);
        }
    }

    private void generateIntent(String uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, uri);
        intent.setType("text/plain");
        getView().onLinkGenerated(intent);
    }

    public void resendInvite(String id) {
        setNameOfRecipient(contactList.get(id).getName());
        getView().showSenderNameDialog();
    }

    void onDeleteContactConfirmed(String id) {
        getView().showProgressDialog();
        getCompositeDisposable().add(
                contactsDataManager.removeContact(contactList.get(id))
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(() -> {
                            getView().showToast(R.string.contacts_delete_contact_success, ToastCustom.TYPE_OK);
                            refreshContacts();
                        }, throwable -> getView().showToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR)));
    }
}
