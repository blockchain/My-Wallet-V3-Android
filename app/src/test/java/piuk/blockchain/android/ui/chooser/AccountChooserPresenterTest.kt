package piuk.blockchain.android.ui.chooser

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.contacts.data.Contact
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import java.util.Arrays

class AccountChooserPresenterTest {

    private lateinit var subject: AccountChooserPresenter
    private var activity: AccountChooserView = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val stringUtils: StringUtils = mock()
    private val contactsDataManager: ContactsDataManager = mock()

    @Before
    fun setUp() {
        subject = AccountChooserPresenter(
            walletAccountHelper,
            stringUtils,
            contactsDataManager
        )
        subject.initView(activity)
    }

    @Test
    fun `onViewReady mode contacts`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.ContactsOnly)
        whenever(activity.isContactsEnabled).thenReturn(true)
        val contact0 = Contact()
        contact0.mdid = "mdid"
        val contact1 = Contact()
        contact1.mdid = "mdid"
        val contact2 = Contact()
        whenever(contactsDataManager.getContactList())
            .thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        verify(contactsDataManager).getContactList()
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(activity).updateUi(captor.capture())
        // Value is 3 as only 2 confirmed contacts plus header
        captor.firstValue.size shouldEqual 3
    }

    @Test
    fun `onViewReady mode contacts no confirmed contacts`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.ContactsOnly)
        whenever(activity.isContactsEnabled).thenReturn(true)
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(contactsDataManager.getContactList())
            .thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        verify(contactsDataManager).getContactList()
        verify(activity).showNoContacts()
    }

    @Test
    fun `onViewReady mode ShapeShift`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.ShapeShift)
        val itemAccount0 = ItemAccount("")
        val itemAccount1 = ItemAccount("")
        val itemAccount2 = ItemAccount("")
        whenever(walletAccountHelper.getHdAccounts())
            .thenReturn(listOf(itemAccount0, itemAccount1, itemAccount2))
        val itemAccount3 = ItemAccount("")
        whenever(walletAccountHelper.getEthAccount())
            .thenReturn(Arrays.asList(itemAccount3))
        whenever(walletAccountHelper.getHdBchAccounts())
            .thenReturn(listOf(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).getHdAccounts()
        verify(walletAccountHelper).getEthAccount()
        verify(walletAccountHelper).getHdBchAccounts()
        verifyNoMoreInteractions(walletAccountHelper)
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 3 headers, 3 BTC accounts, 1 ETH account, 3 BCH accounts
        captor.firstValue.size shouldEqual 10
    }

    @Test
    fun `onViewReady mode bitcoin`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.Bitcoin)
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(walletAccountHelper.getHdAccounts())
            .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(walletAccountHelper.getLegacyAddresses())
            .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).getHdAccounts()
        verify(walletAccountHelper).getLegacyAddresses()
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 3 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size shouldEqual 8
    }

    @Test
    fun `onViewReady mode bitcoin HD only`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.BitcoinHdOnly)
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(walletAccountHelper.getHdAccounts())
            .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).getHdAccounts()
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 3 accounts only
        captor.firstValue.size shouldEqual 3
    }

    @Test
    fun `onViewReady mode bitcoin cash`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.BitcoinCash)
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(walletAccountHelper.getHdBchAccounts())
            .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).getHdBchAccounts()
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 1 header, 3 accounts
        captor.firstValue.size shouldEqual 4
    }

    @Test
    fun `onViewReady mode bitcoin cash send`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.BitcoinCashSend)
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(walletAccountHelper.getHdBchAccounts())
            .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(walletAccountHelper.getLegacyBchAddresses())
            .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).getHdBchAccounts()
        verify(walletAccountHelper).getLegacyBchAddresses()
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 2 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size shouldEqual 8
    }
}