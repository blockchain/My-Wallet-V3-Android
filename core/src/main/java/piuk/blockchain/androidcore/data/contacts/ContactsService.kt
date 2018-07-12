package piuk.blockchain.androidcore.data.contacts

import info.blockchain.wallet.contacts.Contacts
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.metadata.data.Message
import io.reactivex.Completable
import io.reactivex.Observable
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.androidcore.utils.annotations.RequiresAccessToken
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import javax.inject.Inject

class ContactsService @Inject constructor(private val contacts: Contacts) {

    // /////////////////////////////////////////////////////////////////////////
    // INIT METHODS AND AUTH
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Initialises the Contacts service
     *
     * @param metaDataHDNode The key for the metadata service
     * @param sharedMetaDataHDNode The key for the shared metadata service
     *
     * @return A [Completable] object
     */
    @WebRequest
    internal fun initContactsService(
        metaDataHDNode: DeterministicKey,
        sharedMetaDataHDNode: DeterministicKey
    ) = Completable.fromCallable {
        contacts.init(metaDataHDNode, sharedMetaDataHDNode)
    }

    /**
     * Invalidates the access token for re-authing
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    internal fun invalidate() = Completable.fromCallable {
        contacts.invalidateToken()
    }

    // /////////////////////////////////////////////////////////////////////////
    // CONTACTS SPECIFIC
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Fetches an updated version of the contacts list
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun fetchContacts() = Completable.fromCallable {
        contacts.fetch()
    }

    /**
     * Saves the contacts list that's currently in memory to the metadata endpoint
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun saveContacts() = Completable.fromCallable {
        contacts.save()
    }

    /**
     * Completely wipes your contact list from the metadata endpoint. Wipes memory also.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun wipeContacts() = Completable.fromCallable {
        contacts.wipe()
    }

    /**
     * Resets the [Contacts] object to prevent issues when logging in/out.
     */
    internal fun destroy() = contacts.destroy()

    /**
     * Returns a stream of [Contact] objects, comprising a list of users. List can be empty.
     *
     * @return A stream of [Contact] objects
     */
    internal fun getContactList() = Observable.defer {
        Observable.fromIterable(contacts.contactList.values)
    }

    /**
     * Returns a stream of [Contact] objects, comprising of a list of users with [ ] objects that
     * need responding to.
     *
     * @return A stream of [Contact] objects
     */
    @WebRequest
    internal fun getContactsWithUnreadPaymentRequests() = Observable.defer {
        Observable.fromIterable(contacts.digestUnreadPaymentRequests())
    }

    /**
     * Inserts a contact into the locally stored Contacts list. Saves this list to server.
     *
     * @param contact The [Contact] to be stored
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun addContact(contact: Contact) = Completable.fromCallable {
        contacts.addContact(contact)
    }

    /**
     * Removes a contact from the locally stored Contacts list. Saves updated list to server.
     */
    @WebRequest
    internal fun removeContact(contact: Contact) = Completable.fromCallable {
        contacts.removeContact(contact)
    }

    /**
     * Renames a [Contact] and then saves the changes to the server.
     *
     * @param contactId The ID of the Contact you wish to update
     * @param name The new name for the Contact
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun renameContact(contactId: String, name: String) = Completable.fromCallable {
        contacts.renameContact(contactId, name)
    }

    // /////////////////////////////////////////////////////////////////////////
    // SHARING SPECIFIC
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails My details that will be visible in invitation url
     * @param recipientDetails Recipient details
     * @return A [Contact] object, which is an updated version of the mydetails object, ie
     * it's the sender's own contact details
     */
    @WebRequest
    @RequiresAccessToken
    internal fun createInvitation(myDetails: Contact, recipientDetails: Contact) =
        Observable.fromCallable { contacts.createInvitation(myDetails, recipientDetails) }

    /**
     * Accepts an invitation from another user
     *
     * @param url An invitation url
     *
     * @return A [Contact] object containing the other user
     */
    @WebRequest
    @RequiresAccessToken
    internal fun acceptInvitation(url: String) =
        Observable.fromCallable { contacts.acceptInvitationLink(url) }

    /**
     * Returns some Contact information from an invitation link
     *
     * @param url The URL which has been sent to the user
     *
     * @return An [Observable] wrapping a Contact
     */
    @WebRequest
    @RequiresAccessToken
    internal fun readInvitationLink(url: String) =
        Observable.fromCallable { contacts.readInvitationLink(url) }

    /**
     * Allows the user to poll to check if the passed Contact has accepted their invite
     *
     * @param contact The [Contact] to be queried
     *
     * @return An [Observable] wrapping a boolean value, returning true if the invitation has
     * been accepted
     */
    @WebRequest
    @RequiresAccessToken
    internal fun readInvitationSent(contact: Contact) =
        Observable.fromCallable { contacts.readInvitationSent(contact) }

    // /////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUEST SPECIFIC
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Sends a new payment request without the need to ask for a receive address from the recipient.
     *
     * @param mdid The recipient's MDID
     * @param request A [PaymentRequest] object containing the request details, ie the amount
     *                and an optional note
     *
     * @return A [Completable] object
     */
    @WebRequest
    @RequiresAccessToken
    internal fun requestSendPayment(mdid: String, request: PaymentRequest) =
        Completable.fromCallable {
            contacts.sendPaymentRequest(mdid, request)
        }

    /**
     * Requests that another user receive bitcoin from current user

     * @param mdid The recipient's MDID
     * @param request A [PaymentRequest] object containing the request details, ie the amount
     *                and an optional note, the receive address
     *
     * @return A [Completable] object
     */
    @WebRequest
    @RequiresAccessToken
    internal fun requestReceivePayment(mdid: String, request: RequestForPaymentRequest) =
        Completable.fromCallable {
            contacts.sendRequestForPaymentRequest(mdid, request)
        }

    /**
     * Sends a response to a payment request containing a [PaymentRequest], which contains a
     * bitcoin address belonging to the user.
     *
     * @param mdid The recipient's MDID
     * @param paymentRequest A [PaymentRequest] object
     * @param facilitatedTxId The ID of the [FacilitatedTransaction]
     *
     * @return A [Completable] object
     */
    @WebRequest
    @RequiresAccessToken
    internal fun sendPaymentRequestResponse(
        mdid: String,
        paymentRequest: PaymentRequest,
        facilitatedTxId: String
    ) = Completable.fromCallable {
        contacts.sendPaymentRequest(mdid, paymentRequest, facilitatedTxId)
    }

    /**
     * Sends notification that a transaction has been processed.
     *
     * @param mdid The recipient's MDID
     * @param txHash The transaction hash
     * @param facilitatedTxId The ID of the [FacilitatedTransaction]
     *
     * @return A [Completable] object
     */
    @WebRequest
    @RequiresAccessToken
    internal fun sendPaymentBroadcasted(
        mdid: String,
        txHash: String,
        facilitatedTxId: String
    ) = Completable.fromCallable {
        contacts.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
    }

    /**
     * Sends a response to a payment request declining the offer of payment.
     *
     * @param mdid The recipient's MDID
     * @param fctxId The ID of the [FacilitatedTransaction] to be declined
     *
     * @return A [Completable] object
     */
    @WebRequest
    @RequiresAccessToken
    internal fun sendPaymentDeclinedResponse(mdid: String, fctxId: String) =
        Completable.fromCallable {
            contacts.sendPaymentDeclined(mdid, fctxId)
        }

    /**
     * Informs the recipient of a payment request that the request has been cancelled.
     *
     * @param mdid The recipient's MDID
     * @param fctxId The ID of the [FacilitatedTransaction] to be cancelled
     *
     * @return A [Completable] object
     */
    @WebRequest
    @RequiresAccessToken
    internal fun sendPaymentCancelledResponse(mdid: String, fctxId: String) =
        Completable.fromCallable {
            contacts.sendPaymentCancelled(mdid, fctxId)
        }

    // /////////////////////////////////////////////////////////////////////////
    // XPUB AND MDID SPECIFIC
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns the XPub associated with an MDID, should the user already be in your trusted contacts
     * list
     *
     * @param mdid The MDID of the user you wish to query
     *
     * @return A [Observable] wrapping a String
     */
    @WebRequest
    internal fun fetchXpub(mdid: String) = Observable.fromCallable { contacts.fetchXpub(mdid) }

    /**
     * Publishes the user's XPub to the metadata service
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun publishXpub() = Completable.fromCallable {
        contacts.publishXpub()
    }

    // /////////////////////////////////////////////////////////////////////////
    // MESSAGES SPECIFIC
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of [Message] objects, with a flag to only return those which haven't
     * been read yet. Can return an empty list.
     *
     * @param onlyNew If true, returns only the unread messages
     *
     * @return An [Observable] wrapping a list of Message objects
     */
    @WebRequest
    @RequiresAccessToken
    internal fun getMessages(onlyNew: Boolean) =
        Observable.fromCallable { contacts.getMessages(onlyNew) }

    /**
     * Allows users to read a particular message by retrieving it from the Shared Metadata service
     *
     * @param messageId The ID of the message to be read
     *
     * @return An [Observable] wrapping a [Message]
     */
    @WebRequest
    @RequiresAccessToken
    internal fun readMessage(messageId: String) =
        Observable.fromCallable { contacts.readMessage(messageId) }

    /**
     * Marks a message as read or unread
     *
     * @param messageId The ID of the message to be marked as read/unread
     * @param markAsRead A flag setting the read status
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    @RequiresAccessToken
    internal fun markMessageAsRead(messageId: String, markAsRead: Boolean) =
        Completable.fromCallable {
            contacts.markMessageAsRead(messageId, markAsRead)
        }

    // /////////////////////////////////////////////////////////////////////////
    // FACILITATED TRANSACTIONS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Deletes a [FacilitatedTransaction] object from a [Contact] and then syncs the
     * Contact list with the server.
     *
     * @param mdid The Contact's MDID
     * @param fctxId The FacilitatedTransaction's ID
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun deleteFacilitatedTransaction(mdid: String, fctxId: String) =
        Completable.fromCallable {
            contacts.deleteFacilitatedTransaction(mdid, fctxId)
        }
}
