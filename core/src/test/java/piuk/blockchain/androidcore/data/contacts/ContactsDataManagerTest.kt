package piuk.blockchain.androidcore.data.contacts

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.metadata.data.Message
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.bitcoinj.crypto.DeterministicKey
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.contacts.datastore.ContactsMapStore
import piuk.blockchain.androidcore.data.contacts.datastore.PendingTransactionListStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.util.NoSuchElementException

class ContactsDataManagerTest : RxTest() {

    private lateinit var subject: ContactsDataManager
    private val contactsService: ContactsService = mock()
    private val pendingTransactionListStore: PendingTransactionListStore = mock()
    private val rxBus: RxBus = mock()
    private val contactsMapStore = ContactsMapStore()

    @Before
    fun setUp() {
        subject = ContactsDataManager(
            contactsService,
            contactsMapStore,
            pendingTransactionListStore,
            rxBus
        )
    }

    @Test
    @Throws(Exception::class)
    fun initContactsService() {
        // Arrange
        val mockMetadataNode: DeterministicKey = mock()
        val mockSharedMetadataNode: DeterministicKey = mock()
        whenever(contactsService.initContactsService(mockMetadataNode, mockSharedMetadataNode))
            .thenReturn(Completable.complete())
        // Act
        val testObserver =
            subject.initContactsService(mockMetadataNode, mockSharedMetadataNode).test()
        // Assert
        verify(contactsService).initContactsService(mockMetadataNode, mockSharedMetadataNode)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchContacts() {
        // Arrange
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        contact2.name = "Has completed transactions"
        val facilitatedTransaction0 = FacilitatedTransaction()
        val facilitatedTransaction1 = FacilitatedTransaction().apply {
            txHash = "TX_HASH"
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
            state = FacilitatedTransaction.STATE_PAYMENT_BROADCASTED
        }
        contact2.addFacilitatedTransaction(facilitatedTransaction0)
        contact2.addFacilitatedTransaction(facilitatedTransaction1)
        whenever(contactsService.fetchContacts()).thenReturn(Completable.complete())
        whenever(contactsService.getContactList()).thenReturn(
            Observable.fromIterable(listOf(contact0, contact1, contact2))
        )
        // Act
        val testObserver = subject.fetchContacts().test()
        // Assert
        verify(contactsService).fetchContacts()
        verify(contactsService).getContactList()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        subject.getTransactionDisplayMap().size shouldEqual 1
    }

    @Test
    @Throws(Exception::class)
    fun saveContacts() {
        // Arrange
        whenever(contactsService.saveContacts()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.saveContacts().test()
        // Assert
        verify(contactsService).saveContacts()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun wipeContacts() {
        // Arrange
        whenever(contactsService.wipeContacts()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.wipeContacts().test()
        // Assert
        verify(contactsService).wipeContacts()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun getContactList() {
        // Arrange
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(contactsService.getContactList()).thenReturn(
            Observable.fromIterable(listOf(contact0, contact1, contact2))
        )
        // Act
        val testObserver = subject.getContactList().toList().test()
        // Assert
        verify(contactsService).getContactList()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun getContactsWithUnreadPaymentRequests() {
        // Arrange
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(contactsService.getContactsWithUnreadPaymentRequests())
            .thenReturn(Observable.fromIterable(listOf(contact0, contact1, contact2)))
        // Act
        val testObserver = subject.getContactsWithUnreadPaymentRequests().toList().test()
        // Assert
        verify(contactsService).getContactsWithUnreadPaymentRequests()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun addContact() {
        // Arrange
        val contact0 = Contact()
        whenever(contactsService.addContact(contact0)).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.addContact(contact0).test()
        // Assert
        verify(contactsService).addContact(contact0)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun removeContact() {
        // Arrange
        val contact0 = Contact()
        whenever(contactsService.removeContact(contact0)).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.removeContact(contact0).test()
        // Assert
        verify(contactsService).removeContact(contact0)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun renameContact() {
        // Arrange
        val contactId = "CONTACT ID"
        val contactName = "CONTACT_NAME"
        whenever(contactsService.renameContact(contactId, contactName))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.renameContact(contactId, contactName).test()
        // Assert
        verify(contactsService).renameContact(contactId, contactName)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun createInvitation() {
        // Arrange
        val sender = Contact()
        val recipient = Contact()
        whenever(contactsService.createInvitation(sender, recipient))
            .thenReturn(Observable.just(sender))
        // Act
        val testObserver = subject.createInvitation(sender, recipient).test()
        // Assert
        verify(contactsService).createInvitation(sender, recipient)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual sender
    }

    @Test
    @Throws(Exception::class)
    fun acceptInvitation() {
        // Arrange
        val invitationUrl = "INVITATION_URL"
        val sender = Contact()
        whenever(contactsService.acceptInvitation(invitationUrl))
            .thenReturn(Observable.just(sender))
        // Act
        val testObserver = subject.acceptInvitation(invitationUrl).test()
        // Assert
        verify(contactsService).acceptInvitation(invitationUrl)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual sender
    }

    @Test
    @Throws(Exception::class)
    fun readInvitationLink() {
        // Arrange
        val invitationUrl = "INVITATION_URL"
        val sender = Contact()
        whenever(contactsService.readInvitationLink(invitationUrl))
            .thenReturn(Observable.just(sender))
        // Act
        val testObserver = subject.readInvitationLink(invitationUrl).test()
        // Assert
        verify(contactsService).readInvitationLink(invitationUrl)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual sender
    }

    @Test
    @Throws(Exception::class)
    fun readInvitationSent() {
        // Arrange
        val recipient = Contact()
        whenever(contactsService.readInvitationSent(recipient))
            .thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.readInvitationSent(recipient).test()
        // Assert
        verify(contactsService).readInvitationSent(recipient)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun requestSendPayment() {
        // Arrange
        val mdid = "MDID"
        val paymentRequest = PaymentRequest()
        whenever(contactsService.requestSendPayment(mdid, paymentRequest))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.requestSendPayment(mdid, paymentRequest).test()
        // Assert
        verify(contactsService).requestSendPayment(mdid, paymentRequest)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun requestReceivePayment() {
        // Arrange
        val mdid = "MDID"
        val paymentRequest = RequestForPaymentRequest()
        whenever(contactsService.requestReceivePayment(mdid, paymentRequest))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.requestReceivePayment(mdid, paymentRequest).test()
        // Assert
        verify(contactsService).requestReceivePayment(mdid, paymentRequest)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentRequestResponse() {
        // Arrange
        val mdid = "MDID"
        val paymentRequest = PaymentRequest()
        val fctxId = "FCTX_ID"
        whenever(contactsService.sendPaymentRequestResponse(mdid, paymentRequest, fctxId))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.sendPaymentRequestResponse(mdid, paymentRequest, fctxId).test()
        // Assert
        verify(contactsService).sendPaymentRequestResponse(mdid, paymentRequest, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentBroadcasted() {
        // Arrange
        val mdid = "MDID"
        val txHash = "TX_HASH"
        val fctxId = "FCTX_ID"
        whenever(contactsService.sendPaymentBroadcasted(mdid, txHash, fctxId))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.sendPaymentBroadcasted(mdid, txHash, fctxId).test()
        // Assert
        verify(contactsService).sendPaymentBroadcasted(mdid, txHash, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentDeclinedResponse() {
        // Arrange
        val mdid = "MDID"
        val fctxId = "FCTX_ID"
        whenever(contactsService.sendPaymentDeclinedResponse(mdid, fctxId))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.sendPaymentDeclinedResponse(mdid, fctxId).test()
        // Assert
        verify(contactsService).sendPaymentDeclinedResponse(mdid, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentCancelledResponse() {
        // Arrange
        val mdid = "MDID"
        val fctxId = "FCTX_ID"
        whenever(contactsService.sendPaymentCancelledResponse(mdid, fctxId))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.sendPaymentCancelledResponse(mdid, fctxId).test()
        // Assert
        verify(contactsService).sendPaymentCancelledResponse(mdid, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchXpub() {
        // Arrange
        val mdid = "MDID"
        val xpub = "XPUB"
        whenever(contactsService.fetchXpub(mdid)).thenReturn(Observable.just(xpub))
        // Act
        val testObserver = subject.fetchXpub(mdid).test()
        // Assert
        verify(contactsService).fetchXpub(mdid)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual xpub
    }

    @Test
    @Throws(Exception::class)
    fun publishXpub() {
        // Arrange
        whenever(contactsService.publishXpub()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.publishXpub().test()
        // Assert
        verify(contactsService).publishXpub()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun getMessages() {
        // Arrange
        val message0 = Message()
        val message1 = Message()
        val message2 = Message()
        val onlyNew = true
        whenever(contactsService.getMessages(onlyNew))
            .thenReturn(Observable.just(listOf(message0, message1, message2)))
        // Act
        val testObserver = subject.getMessages(onlyNew).test()
        // Assert
        verify(contactsService).getMessages(onlyNew)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun readMessage() {
        // Arrange
        val messageId = "MESSAGE_ID"
        val message = Message()
        whenever(contactsService.readMessage(messageId)).thenReturn(Observable.just(message))
        // Act
        val testObserver = subject.readMessage(messageId).test()
        // Assert
        verify(contactsService).readMessage(messageId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual message
    }

    @Test
    @Throws(Exception::class)
    fun markMessageAsRead() {
        // Arrange
        val messageId = "MESSAGE_ID"
        val markAsRead = true
        whenever(contactsService.markMessageAsRead(messageId, markAsRead))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.markMessageAsRead(messageId, markAsRead).test()
        // Assert
        verify(contactsService).markMessageAsRead(messageId, markAsRead)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun refreshFacilitatedTransactions() {
        // Arrange
        // Has completed transaction, should be filtered out
        val contact0 = Contact().apply { name = "contact0" }
        val facilitatedTransaction0 = FacilitatedTransaction().apply {
            txHash = "TX_HASH"
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
        }
        contact0.addFacilitatedTransaction(facilitatedTransaction0)
        // Has pending transaction, ie not completed
        val contact1 = Contact().apply { name = "contact1" }
        val facilitatedTransaction1 = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
        }
        contact1.addFacilitatedTransaction(facilitatedTransaction1)
        // Has no transactions
        val contact2 = Contact().apply { name = "contact2" }
        whenever(contactsService.getContactList())
            .thenReturn(Observable.fromIterable(listOf(contact0, contact1, contact2)))
        // Act
        val testObserver = subject.refreshFacilitatedTransactions().toList().test()
        // Assert
        verify(contactsService).getContactList()
        verify(pendingTransactionListStore).insertTransaction(any())
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 1
        val contactTransactionModel = testObserver.values()[0][0]
        contactTransactionModel.contactName shouldEqual contact1.name
        contactTransactionModel.facilitatedTransaction shouldEqual facilitatedTransaction1
    }

    @Test
    @Throws(Exception::class)
    fun refreshFacilitatedTransactionsFiltersOutDeclined() {
        // Arrange
        // Has declined transaction - should be filtered out
        val contact0 = Contact().apply { name = "contact0" }
        val facilitatedTransaction0 = FacilitatedTransaction().apply {
            txHash = "TX_HASH"
            state = FacilitatedTransaction.STATE_DECLINED
        }
        contact0.addFacilitatedTransaction(facilitatedTransaction0)
        // Has pending transaction, ie not completed
        val contact1 = Contact().apply { name = "contact1" }
        val facilitatedTransaction1 = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
        }
        contact1.addFacilitatedTransaction(facilitatedTransaction1)
        // Has cancelled transaction, should be filtered out
        val contact2 = Contact().apply { name = "contact2" }
        val facilitatedTransaction2 = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_CANCELLED
        }
        contact2.addFacilitatedTransaction(facilitatedTransaction2)
        whenever(contactsService.getContactList())
            .thenReturn(Observable.fromIterable(listOf(contact0, contact1, contact2)))
        // Act
        val testObserver = subject.refreshFacilitatedTransactions().toList().test()
        // Assert
        verify(contactsService).getContactList()
        verify(pendingTransactionListStore).insertTransaction(any())
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 1
        val contactTransactionModel = testObserver.values()[0][0]
        contactTransactionModel.contactName shouldEqual contact1.name
        contactTransactionModel.facilitatedTransaction shouldEqual facilitatedTransaction1
    }

    @Test
    @Throws(Exception::class)
    fun getFacilitatedTransactions() {
        // Arrange
        val contactTransactionModel0 = ContactTransactionModel("", mock())
        val contactTransactionModel1 = ContactTransactionModel("", mock())
        val contactTransactionModel2 = ContactTransactionModel("", mock())
        whenever(pendingTransactionListStore.list)
            .thenReturn(
                listOf(
                    contactTransactionModel0,
                    contactTransactionModel1,
                    contactTransactionModel2
                )
            )
        // Act
        val testObserver = subject.getFacilitatedTransactions().toList().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun getContactFromFctxId() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransaction0 = FacilitatedTransaction()
        val facilitatedTransaction1 = FacilitatedTransaction()
        val facilitatedTransaction2 = FacilitatedTransaction()
        facilitatedTransaction2.id = fctxId
        val contact0 = Contact()
        contact0.addFacilitatedTransaction(facilitatedTransaction0)
        contact0.addFacilitatedTransaction(facilitatedTransaction1)
        contact0.addFacilitatedTransaction(facilitatedTransaction2)
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(contactsService.getContactList())
            .thenReturn(Observable.fromIterable(listOf(contact0, contact1, contact2)))
        // Act
        val testObserver = subject.getContactFromFctxId(fctxId).test()
        // Assert
        verify(contactsService).getContactList()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual contact0
    }

    @Test
    @Throws(Exception::class)
    fun getContactFromFctxIdNotFound() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransaction0 = FacilitatedTransaction()
        val facilitatedTransaction1 = FacilitatedTransaction()
        val facilitatedTransaction2 = FacilitatedTransaction()
        val contact0 = Contact()
        contact0.addFacilitatedTransaction(facilitatedTransaction0)
        contact0.addFacilitatedTransaction(facilitatedTransaction1)
        contact0.addFacilitatedTransaction(facilitatedTransaction2)
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(contactsService.getContactList())
            .thenReturn(Observable.fromIterable(listOf(contact0, contact1, contact2)))
        // Act
        val testObserver = subject.getContactFromFctxId(fctxId).test()
        // Assert
        verify(contactsService).getContactList()
        testObserver.assertError(NoSuchElementException::class.java)
        testObserver.assertNotComplete()
        testObserver.assertNoValues()
    }

    @Test
    @Throws(Exception::class)
    fun deleteFacilitatedTransaction() {
        // Arrange
        val mdid = "MDID"
        val fctxId = "FCTX_ID"
        whenever(
            contactsService.deleteFacilitatedTransaction(
                mdid,
                fctxId
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.deleteFacilitatedTransaction(mdid, fctxId).test()
        // Assert
        verify(contactsService).deleteFacilitatedTransaction(mdid, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun getContactsTransactionMap() {
        // Arrange

        // Act
        val result = subject.getTransactionDisplayMap()
        // Assert
        result shouldEqual subject.getTransactionDisplayMap()
    }

    @Test
    @Throws(Exception::class)
    fun resetContacts() {
        // Arrange

        // Act
        subject.resetContacts()
        // Assert
        verify(pendingTransactionListStore).clearList()
        verify(contactsService).destroy()
        subject.getTransactionDisplayMap().size shouldEqual 0
    }
}