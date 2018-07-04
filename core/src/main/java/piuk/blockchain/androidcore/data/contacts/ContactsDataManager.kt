package piuk.blockchain.androidcore.data.contacts

import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.metadata.data.Message
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel
import piuk.blockchain.androidcore.data.contacts.datastore.ContactsMapStore
import piuk.blockchain.androidcore.data.contacts.datastore.PendingTransactionListStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.annotations.Mockable
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.util.*
import javax.inject.Inject

/**
 * A manager for handling all Metadata/Shared Metadata/Contacts based operations. Using this class
 * requires careful initialisation, which should be done as follows:
 *
 * 1) Load the metadata nodes from the metadata service using [PayloadDataManager.loadNodes]. This
 * will return false if the nodes cannot be found.
 *
 * 2) Generate nodes if necessary. If step 1 returns false, the nodes must be generated using
 * [PayloadDataManager.generateNodes]. In theory, this means that the nodes only need to be
 * generated once, and thus users with a second password only need to be prompted to enter their
 * password once.
 *
 * 3) Init the Contacts Service using [initContactsService], passing in the appropriate nodes loaded
 * by [PayloadDataManager.loadNodes].
 *
 * 4) Register the user's derived MDID with the Shared Metadata service using
 * [PayloadDataManager.registerMdid].
 *
 * 5) Finally, publish the user's XPub to the Shared Metadata service via [publishXpub].
 */
@Mockable
@PresenterScope
class ContactsDataManager @Inject constructor(
        private val contactsService: ContactsService,
        private val contactsMapStore: ContactsMapStore,
        private val pendingTransactionListStore: PendingTransactionListStore,
        rxBus: RxBus
) {

    private val rxPinning: RxPinning = RxPinning(rxBus)

    /**
     * Initialises the Contacts service.
     *
     * @param metadataNode       A [DeterministicKey] representing the Metadata Node
     * @param sharedMetadataNode A [DeterministicKey] representing the Shared Metadata node
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun initContactsService(
            metadataNode: DeterministicKey,
            sharedMetadataNode: DeterministicKey
    ): Completable {
        return rxPinning.call {
            contactsService.initContactsService(metadataNode, sharedMetadataNode)
        }.applySchedulers()
    }

    /**
     * Invalidates the access token for re-authing, if needed.
     */
    private fun invalidate(): Completable {
        return rxPinning.call { contactsService.invalidate() }
                .applySchedulers()
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Fetches an updated version of the contacts list and parses [FacilitatedTransaction]
     * objects into a map if completed.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun fetchContacts(): Completable {
        return rxPinning.call { contactsService.fetchContacts() }
                .andThen(contactsService.getContactList())
                .doOnNext { contact ->
                    contactsMapStore.displayMap.putAll(
                            contact.facilitatedTransactions.values
                                    .filter { !it.txHash.isNullOrEmpty() }
                                    .associateBy({ it.txHash }, {
                                        ContactTransactionDisplayModel(
                                                it.role,
                                                it.state,
                                                it.note ?: "",
                                                contact.name
                                        )
                                    })
                    )
                }
                .toList()
                .ignoreElement()
                .applySchedulers()
    }

    /**
     * Saves the contacts list that's currently in memory to the metadata endpoint
     *
     * @return A [Completable] object, ie an asynchronous void operation≈≈
     */
    fun saveContacts(): Completable = rxPinning.call { contactsService.saveContacts() }
            .applySchedulers()

    /**
     * Completely wipes your contact list from the metadata endpoint. Does not update memory.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun wipeContacts(): Completable = rxPinning.call { contactsService.wipeContacts() }
            .applySchedulers()

    /**
     * Returns a stream of [Contact] objects, comprising a list of users. List can be empty.
     *
     * @return A stream of [Contact] objects
     */
    fun getContactList(): Observable<Contact> = contactsService.getContactList()
            .applySchedulers()

    /**
     * Returns a stream of [Contact] objects, comprising of a list of users with [ ] objects that
     * need responding to.
     *
     * @return A stream of [Contact] objects
     */
    fun getContactsWithUnreadPaymentRequests(): Observable<Contact> =
            callWithToken(contactsService.getContactsWithUnreadPaymentRequests())
                    .applySchedulers()

    /**
     * Inserts a contact into the locally stored Contacts list. Saves this list to server.
     *
     * @param contact The [Contact] to be stored
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun addContact(contact: Contact): Completable =
            rxPinning.call { contactsService.addContact(contact) }
                    .applySchedulers()

    /**
     * Removes a contact from the locally stored Contacts list. Saves updated list to server.
     *
     * @param contact The [Contact] to be stored
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun removeContact(contact: Contact): Completable =
            rxPinning.call { contactsService.removeContact(contact) }
                    .applySchedulers()

    /**
     * Renames a [Contact] and then saves the changes to the server.
     *
     * @param contactId The ID of the Contact you wish to update
     * @param name      The new name for the Contact
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun renameContact(contactId: String, name: String): Completable =
            rxPinning.call { contactsService.renameContact(contactId, name) }
                    .applySchedulers()

    ///////////////////////////////////////////////////////////////////////////
    // INVITATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails        My details that will be visible in invitation url
     * @param recipientDetails Recipient details
     *
     * @return A [Contact] object, which is an updated version of the mydetails object, ie
     * it's the sender's own contact details
     */
    fun createInvitation(myDetails: Contact, recipientDetails: Contact): Observable<Contact> {
        return callWithToken(contactsService.createInvitation(myDetails, recipientDetails))
                .applySchedulers()
    }

    /**
     * Accepts an invitation from another user
     *
     * @param invitationUrl An invitation url
     *
     * @return A [Contact] object representing the other user
     */
    fun acceptInvitation(invitationUrl: String): Observable<Contact> {
        return callWithToken(contactsService.acceptInvitation(invitationUrl))
                .applySchedulers()
    }

    /**
     * Returns some Contact information from an invitation link
     *
     * @param url The URL which has been sent to the user
     *
     * @return An [Observable] wrapping a Contact
     */
    fun readInvitationLink(url: String): Observable<Contact> {
        return callWithToken(contactsService.readInvitationLink(url))
                .applySchedulers()
    }

    /**
     * Allows the user to poll to check if the passed Contact has accepted their invite
     *
     * @param contact The [Contact] to be queried
     *
     * @return An [Observable] wrapping a boolean value, returning true if the invitation has
     * been accepted
     */
    fun readInvitationSent(contact: Contact): Observable<Boolean> {
        return callWithToken(contactsService.readInvitationSent(contact))
                .applySchedulers()
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUESTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Requests that another user sends you a payment
     *
     * @param mdid    The recipient's MDID
     * @param request A [PaymentRequest] object containing the request details, ie the amount
     *                and an optional note
     *
     * @return A [Completable] object
     */
    fun requestSendPayment(mdid: String, request: PaymentRequest): Completable {
        return callWithToken(contactsService.requestSendPayment(mdid, request))
                .applySchedulers()
    }

    /**
     * Requests that another user receive bitcoin from current user
     *
     * @param mdid    The recipient's MDID
     * @param request A [PaymentRequest] object containing the request details, ie the amount
     *                and an optional note, the receive address
     *
     * @return A [Completable] object
     */
    fun requestReceivePayment(mdid: String, request: RequestForPaymentRequest): Completable {
        return callWithToken(contactsService.requestReceivePayment(mdid, request))
                .applySchedulers()
    }

    /**
     * Sends a response to a payment request containing a [PaymentRequest], which contains a
     * bitcoin address belonging to the user.
     *
     * @param mdid            The recipient's MDID*
     * @param paymentRequest  A [PaymentRequest] object
     * @param facilitatedTxId The ID of the [FacilitatedTransaction]
     *
     * @return A [Completable] object
     */
    fun sendPaymentRequestResponse(
            mdid: String,
            paymentRequest: PaymentRequest,
            facilitatedTxId: String
    ): Completable {
        return callWithToken(
                contactsService.sendPaymentRequestResponse(mdid, paymentRequest, facilitatedTxId)
        ).applySchedulers()
    }

    /**
     * Sends notification that a transaction has been processed.
     *
     * @param mdid            The recipient's MDID
     * @param txHash          The transaction hash
     * @param facilitatedTxId The ID of the [FacilitatedTransaction]
     *
     * @return A [Completable] object
     */
    fun sendPaymentBroadcasted(mdid: String, txHash: String, facilitatedTxId: String): Completable {
        return callWithToken(contactsService.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId))
                .applySchedulers()
    }

    /**
     * Sends a response to a payment request declining the offer of payment.
     *
     * @param mdid   The recipient's MDID
     * @param fctxId The ID of the [FacilitatedTransaction] to be declined
     *
     * @return A [Completable] object
     */
    fun sendPaymentDeclinedResponse(mdid: String, fctxId: String): Completable {
        return callWithToken(contactsService.sendPaymentDeclinedResponse(mdid, fctxId))
                .applySchedulers()
    }

    /**
     * Informs the recipient of a payment request that the request has been cancelled.
     *
     * @param mdid   The recipient's MDID
     * @param fctxId The ID of the [FacilitatedTransaction] to be cancelled
     *
     * @return A [Completable] object
     */
    fun sendPaymentCancelledResponse(mdid: String, fctxId: String): Completable {
        return callWithToken(contactsService.sendPaymentCancelledResponse(mdid, fctxId))
                .applySchedulers()
    }

    ///////////////////////////////////////////////////////////////////////////
    // XPUB AND MDID HANDLING
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the XPub associated with an MDID, should the user already be in your trusted contacts
     * list
     *
     * @param mdid The MDID of the user you wish to query
     *
     * @return A [Observable] wrapping a String
     */
    fun fetchXpub(mdid: String): Observable<String> =
            rxPinning.call<String> { contactsService.fetchXpub(mdid) }
                    .applySchedulers()

    /**
     * Publishes the user's XPub to the metadata service
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun publishXpub(): Completable = rxPinning.call { contactsService.publishXpub() }
            .applySchedulers()

    ///////////////////////////////////////////////////////////////////////////
    // MESSAGES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of [Message] objects, with a flag to only return those which haven't
     * been read yet. Can return an empty list.
     *
     * @param onlyNew If true, returns only the unread messages
     *
     * @return An [Observable] wrapping a list of Message objects
     */
    fun getMessages(onlyNew: Boolean): Observable<List<Message>> {
        return callWithToken(contactsService.getMessages(onlyNew))
                .applySchedulers()
    }

    /**
     * Allows users to read a particular message by retrieving it from the Shared Metadata service
     *
     * @param messageId The ID of the message to be read
     *
     * @return An [Observable] wrapping a [Message]
     */
    fun readMessage(messageId: String): Observable<Message> {
        return callWithToken(contactsService.readMessage(messageId))
                .applySchedulers()
    }

    /**
     * Marks a message as read or unread
     *
     * @param messageId  The ID of the message to be marked as read/unread
     * @param markAsRead A flag setting the read status
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun markMessageAsRead(messageId: String, markAsRead: Boolean): Completable {
        return callWithToken(contactsService.markMessageAsRead(messageId, markAsRead))
                .applySchedulers()
    }

    ///////////////////////////////////////////////////////////////////////////
    // FACILITATED TRANSACTIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Finds and returns a stream of [ContactTransactionModel] objects and stores them locally
     * where the transaction is yet to be completed, ie the hash is empty. Intended to be used to
     * display a list of transactions with another user in the balance page, and therefore this list
     * does not contain completed, cancelled or declined transactions.

     * @return An [Observable] stream of [ContactTransactionModel] objects
     */
    fun refreshFacilitatedTransactions(): Observable<ContactTransactionModel> {
        pendingTransactionListStore.clearList()
        return getContactList()
                .flatMapIterable { contact ->
                    val transactions = ArrayList<ContactTransactionModel>()
                    for (it in contact.facilitatedTransactions.values) {
                        // If hash is null, transaction has not been completed
                        if ((it.txHash == null || it.txHash.isEmpty())
                                // Filter out cancelled and declined transactions
                                && it.state != FacilitatedTransaction.STATE_CANCELLED
                                && it.state != FacilitatedTransaction.STATE_DECLINED
                        ) {

                            val model = ContactTransactionModel(contact.name, it)
                            pendingTransactionListStore.insertTransaction(model)
                            transactions.add(model)
                        }
                    }
                    return@flatMapIterable transactions
                }
    }

    /**
     * Returns a stream of [ContactTransactionModel] objects from disk where the transaction
     * is yet to be completed, ie the hash is empty.
     *
     * @return An [Observable] stream of [ContactTransactionModel] objects
     */
    fun getFacilitatedTransactions(): Observable<ContactTransactionModel> =
            Observable.fromIterable(pendingTransactionListStore.list)

    /**
     * Returns a [Contact] object from a given FacilitatedTransaction ID. It's possible that
     * the Observable will return an empty object, but very unlikely.
     *
     * @param fctxId The [FacilitatedTransaction] ID.
     *
     * @return A [Single] emitting a [Contact] object or will emit a [ ] if the Contact isn't found.
     */
    fun getContactFromFctxId(fctxId: String): Single<Contact> = getContactList()
            .filter { it.facilitatedTransactions[fctxId] != null }
            .firstOrError()

    /**
     * Deletes a [FacilitatedTransaction] object from a [Contact] and then syncs the
     * Contact list with the server.
     *
     * @param mdid   The Contact's MDID
     *
     * @param fctxId The FacilitatedTransaction's ID
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun deleteFacilitatedTransaction(mdid: String, fctxId: String): Completable {
        return callWithToken(contactsService.deleteFacilitatedTransaction(mdid, fctxId))
                .applySchedulers()
    }

    /**
     * Returns a [MutableMap] containing [ContactTransactionDisplayModel] objects keyed to a Tx hash
     * for convenient display in lists
     */
    fun getTransactionDisplayMap() = contactsMapStore.displayMap

    /**
     * Clears all data in the [PendingTransactionListStore].
     */
    fun resetContacts() {
        contactsService.destroy()
        pendingTransactionListStore.clearList()
        contactsMapStore.clearData()
    }

    ///////////////////////////////////////////////////////////////////////////
    // TOKEN FUNCTIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calls a function and invalidates the access token on failure before calling the original
     * function again, which will trigger getting another access token. Called via [RxPinning]
     * which propagates an error to the UI when SSL pinning fails.
     */
    private fun <T> callWithToken(observable: Observable<T>): Observable<T> {
        return rxPinning.call<T> { getRetry(observable) }
    }

    /**
     * Calls a function and invalidates the access token on failure before calling the original
     * function again, which will trigger getting another access token. Called via [RxPinning]
     * which propagates an error to the UI when SSL pinning fails.
     */
    private fun callWithToken(completable: Completable): Completable =
            rxPinning.call { getRetry(completable) }

    private fun <T> getRetry(observable: Observable<T>): Observable<T> =
            Observable.defer<T> { observable }
                    .doOnError { invalidate() }
                    .retry(1)

    private fun getRetry(completable: Completable): Completable = Completable.defer { completable }
            .doOnError { invalidate() }
            .retry(1)

}
