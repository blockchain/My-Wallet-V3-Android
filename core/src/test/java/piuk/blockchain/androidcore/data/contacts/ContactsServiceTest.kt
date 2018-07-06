package piuk.blockchain.androidcore.data.contacts

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.contacts.Contacts
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.metadata.data.Message
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.RxTest
import java.util.HashMap

class ContactsServiceTest : RxTest() {

    private lateinit var subject: ContactsService
    private val mockContacts: Contacts = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = ContactsService(mockContacts)
    }

    @Test
    @Throws(Exception::class)
    fun initContactsService() {
        // Arrange

        // Act
        val testObserver = subject.initContactsService(mock(), mock()).test()
        // Assert
        verify(mockContacts).init(any(), any())
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun invalidate() {
        // Arrange

        // Act
        val testObserver = subject.invalidate().test()
        // Assert
        verify(mockContacts).invalidateToken()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchContacts() {
        // Arrange

        // Act
        val testObserver = subject.fetchContacts().test()
        // Assert
        verify(mockContacts).fetch()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun saveContacts() {
        // Arrange

        // Act
        val testObserver = subject.saveContacts().test()
        // Assert
        verify(mockContacts).save()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun wipeContacts() {
        // Arrange

        // Act
        val testObserver = subject.wipeContacts().test()
        // Assert
        verify(mockContacts).wipe()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun destroy() {
        // Arrange

        // Act
        subject.destroy()
        // Assert
        verify(mockContacts).destroy()
    }

    @Test
    @Throws(Exception::class)
    fun getContactList() {
        // Arrange
        val map = HashMap<String, Contact>()
        map["0"] = mock()
        map["1"] = mock()
        map["2"] = mock()
        whenever(mockContacts.contactList).thenReturn(map)
        // Act
        val testObserver = subject.getContactList().toList().test()
        // Assert
        verify(mockContacts).contactList
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun getContactsWithUnreadPaymentRequests() {
        // Arrange
        whenever(mockContacts.digestUnreadPaymentRequests()).thenReturn(
            listOf(
                mock(),
                mock(),
                mock()
            )
        )
        // Act
        val testObserver = subject.getContactsWithUnreadPaymentRequests().test()
        // Assert
        verify(mockContacts).digestUnreadPaymentRequests()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values().size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun addContact() {
        // Arrange
        val mockContact: Contact = mock()
        // Act
        val testObserver = subject.addContact(mockContact).test()
        // Assert
        verify(mockContacts).addContact(mockContact)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun removeContact() {
        // Arrange
        val mockContact: Contact = mock()
        // Act
        val testObserver = subject.removeContact(mockContact).test()
        // Assert
        verify(mockContacts).removeContact(mockContact)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun renameContact() {
        // Arrange
        val contactId = "CONTACT_ID"
        val contactName = "CONTACT_NAME"
        // Act
        val testObserver = subject.renameContact(contactId, contactName).test()
        // Assert
        verify(mockContacts).renameContact(contactId, contactName)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun createInvitation() {
        // Arrange
        val mockSender: Contact = mock()
        val mockRecipient: Contact = mock()
        whenever(mockContacts.createInvitation(any(), any())).thenReturn(mockSender)
        // Act
        val testObserver = subject.createInvitation(mockSender, mockRecipient).test()
        // Assert
        verify(mockContacts).createInvitation(mockSender, mockRecipient)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual mockSender
    }

    @Test
    @Throws(Exception::class)
    fun acceptInvitation() {
        // Arrange
        val mockSender: Contact = mock()
        val inviteString = "INVITE_STRING"
        whenever(mockContacts.acceptInvitationLink(inviteString)).thenReturn(mockSender)
        // Act
        val testObserver = subject.acceptInvitation(inviteString).test()
        // Assert
        verify(mockContacts).acceptInvitationLink(inviteString)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual mockSender
    }

    @Test
    @Throws(Exception::class)
    fun readInvitationLink() {
        // Arrange
        val mockSender: Contact = mock()
        val inviteString = "INVITE_STRING"
        whenever(mockContacts.readInvitationLink(inviteString)).thenReturn(mockSender)
        // Act
        val testObserver = subject.readInvitationLink(inviteString).test()
        // Assert
        verify(mockContacts).readInvitationLink(inviteString)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual mockSender
    }

    @Test
    @Throws(Exception::class)
    fun readInvitationSent() {
        // Arrange
        val mockRecipient: Contact = mock()
        whenever(mockContacts.readInvitationSent(mockRecipient)).thenReturn(true)
        // Act
        val testObserver = subject.readInvitationSent(mockRecipient).test()
        // Assert
        verify(mockContacts).readInvitationSent(mockRecipient)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun requestSendPayment() {
        // Arrange
        val mdid = "MDID"
        val mockRequest: PaymentRequest = mock()
        // Act
        val testObserver = subject.requestSendPayment(mdid, mockRequest).test()
        // Assert
        verify(mockContacts).sendPaymentRequest(mdid, mockRequest)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun requestReceivePayment() {
        // Arrange
        val mdid = "MDID"
        val mockRequest: RequestForPaymentRequest = mock()
        // Act
        val testObserver = subject.requestReceivePayment(mdid, mockRequest).test()
        // Assert
        verify(mockContacts).sendRequestForPaymentRequest(mdid, mockRequest)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentRequestResponse() {
        // Arrange
        val mdid = "MDID"
        val mockRequest: PaymentRequest = mock()
        val fctxId = "FCTX_ID"
        // Act
        val testObserver = subject.sendPaymentRequestResponse(mdid, mockRequest, fctxId).test()
        // Assert
        verify(mockContacts).sendPaymentRequest(mdid, mockRequest, fctxId)
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
        // Act
        val testObserver = subject.sendPaymentBroadcasted(mdid, txHash, fctxId).test()
        // Assert
        verify(mockContacts).sendPaymentBroadcasted(mdid, txHash, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentDeclinedResponse() {
        // Arrange
        val mdid = "MDID"
        val fctxId = "FCTX_ID"
        // Act
        val testObserver = subject.sendPaymentDeclinedResponse(mdid, fctxId).test()
        // Assert
        verify(mockContacts).sendPaymentDeclined(mdid, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun sendPaymentCancelledResponse() {
        // Arrange
        val mdid = "MDID"
        val fctxId = "FCTX_ID"
        // Act
        val testObserver = subject.sendPaymentCancelledResponse(mdid, fctxId).test()
        // Assert
        verify(mockContacts).sendPaymentCancelled(mdid, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchXpub() {
        // Arrange
        val mdid = "MDID"
        val xpub = "XPUB"
        whenever(mockContacts.fetchXpub(mdid)).thenReturn(xpub)
        // Act
        val testObserver = subject.fetchXpub(mdid).test()
        // Assert
        verify(mockContacts).fetchXpub(mdid)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual xpub
    }

    @Test
    @Throws(Exception::class)
    fun publishXpub() {
        // Arrange

        // Act
        val testObserver = subject.publishXpub().test()
        // Assert
        verify(mockContacts).publishXpub()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun getMessages() {
        // Arrange
        val onlyNew = true
        whenever(mockContacts.getMessages(onlyNew)).thenReturn(listOf(mock(), mock(), mock()))
        // Act
        val testObserver = subject.getMessages(onlyNew).test()
        // Assert
        verify(mockContacts).getMessages(onlyNew)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0].size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun readMessage() {
        // Arrange
        val messageId = "MESSAGE_ID"
        val mockMessage: Message = mock()
        whenever(mockContacts.readMessage(messageId)).thenReturn(mockMessage)
        // Act
        val testObserver = subject.readMessage(messageId).test()
        // Assert
        verify(mockContacts).readMessage(messageId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual mockMessage
    }

    @Test
    @Throws(Exception::class)
    fun markMessageAsRead() {
        // Arrange
        val messageId = "MESSAGE_ID"
        val markAsRead = true
        // Act
        val testObserver = subject.markMessageAsRead(messageId, markAsRead).test()
        // Assert
        verify(mockContacts).markMessageAsRead(messageId, markAsRead)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun deleteFacilitatedTransaction() {
        // Arrange
        val messageId = "MESSAGE_ID"
        val fctxId = "FCTX_ID"
        // Act
        val testObserver = subject.deleteFacilitatedTransaction(messageId, fctxId).test()
        // Assert
        verify(mockContacts).deleteFacilitatedTransaction(messageId, fctxId)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }
}