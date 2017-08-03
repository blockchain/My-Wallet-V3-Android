package piuk.blockchain.android.ui.chooser

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.data.contacts.ContactsDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import java.util.*


class AccountChooserPresenterTest {

    private lateinit var subject: AccountChooserPresenter
    private var mockActivity: AccountChooserView = mock()
    private var mockWalletAccountHelper: WalletAccountHelper = mock()
    private var mockStringUtils: StringUtils = mock()
    private var mockContactsManager: ContactsDataManager = mock()
    private var mockAccessState: AccessState = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject = AccountChooserPresenter(
                mockWalletAccountHelper,
                mockStringUtils,
                mockContactsManager,
                mockAccessState
        )
        subject.initView(mockActivity)
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun onViewReadyRequestTypeNull() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(null)
        // Act
        subject.onViewReady()
        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeContact() {
        if (BuildConfig.CONTACTS_ENABLED) {
            // Arrange
            whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.CONTACT)
            val contact0 = Contact()
            contact0.mdid = "mdid"
            val contact1 = Contact()
            contact1.mdid = "mdid"
            val contact2 = Contact()
            whenever(mockContactsManager.getContactList())
                    .thenReturn(Observable.just(contact0, contact1, contact2))
            // Act
            subject.onViewReady()
            // Assert
            verify(mockContactsManager).getContactList()
            val captor = argumentCaptor<List<ItemAccount>>()
            verify(mockActivity).updateUi(captor.capture())
            // Value is 3 as only 2 confirmed contacts plus header
            captor.firstValue.size shouldEqual 3
        }
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeContactNoConfirmedContacts() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.CONTACT)
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(mockContactsManager.getContactList())
                .thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        if (BuildConfig.CONTACTS_ENABLED) {
            verify(mockContactsManager).getContactList()
        }
        verify(mockActivity).showNoContacts()
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeRequest() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.REQUEST)
        val itemAccount0 = ItemAccount("", "", null, null, null, null)
        val itemAccount1 = ItemAccount("", "", null, null, null, null)
        val itemAccount2 = ItemAccount("", "", null, null, null, null)
        whenever(mockWalletAccountHelper.getHdAccounts(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 2 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size shouldEqual 8
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeSend() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.SEND)
        val contact0 = Contact()
        contact0.mdid = "mdid"
        val contact1 = Contact()
        contact1.mdid = "mdid"
        val contact2 = Contact()
        whenever(mockContactsManager.getContactList())
                .thenReturn(Observable.just(contact0, contact1, contact2))
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(mockWalletAccountHelper.getHdAccounts(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        if (BuildConfig.CONTACTS_ENABLED) {
            verify(mockContactsManager).getContactList()
        }
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 3 headers, 3 accounts, 3 legacy addresses, 2 confirmed contacts
        if (BuildConfig.CONTACTS_ENABLED) {
            captor.firstValue.size shouldEqual 11
        } else {
            captor.firstValue.size shouldEqual 8
        }

        println(BuildConfig.CONTACTS_ENABLED)
    }

}






