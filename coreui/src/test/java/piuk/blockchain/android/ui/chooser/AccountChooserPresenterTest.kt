package piuk.blockchain.android.ui.chooser

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.contacts.data.Contact
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager

class AccountChooserPresenterTest {

    private lateinit var subject: AccountChooserPresenter
    private var activity: AccountChooserView = mock()
    private val walletAccountHelper: AccountListing = mock()
    private val stringUtils: StringUtils = mock {
        on { getString(any()) } `it returns` ""
    }
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
        val contact0 = Contact().apply {
            name = "Contact 1"
            mdid = "mdid"
        }
        val contact1 = Contact().apply {
            name = "Contact 2"
            mdid = "mdid"
        }
        val contact2 = Contact().apply {
            name = "Contact 3"
        }
        val contact3 = Contact().apply {
            mdid = "mdid"
        }
        whenever(contactsDataManager.getContactList())
            .thenReturn(Observable.just(contact0, contact1, contact2, contact3))
        // Act
        subject.onViewReady()
        // Assert
        verify(contactsDataManager).getContactList()
        val captor = argumentCaptor<List<AccountChooserItem>>()
        verify(activity).updateUi(captor.capture())
        // Value is 3 as only 2 confirmed contacts with names plus header
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
    fun `onViewReady mode Exchange`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.Exchange)
        val itemAccount0 = getItemAccount0()
        val itemAccount1 = getItemAccount0()
        val itemAccount2 = getItemAccount0()
        whenever(walletAccountHelper.accountList(CryptoCurrency.BTC))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        val itemAccount3 = getItemAccount0()
        whenever(walletAccountHelper.accountList(CryptoCurrency.ETHER))
            .thenReturn(
                Observable.just(listOf(itemAccount3))
            )
        whenever(walletAccountHelper.accountList(CryptoCurrency.BCH))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).accountList(CryptoCurrency.BTC)
        verify(walletAccountHelper).accountList(CryptoCurrency.ETHER)
        verify(walletAccountHelper).accountList(CryptoCurrency.BCH)
        verifyNoMoreInteractions(walletAccountHelper)
        val captor = argumentCaptor<List<AccountChooserItem>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 3 headers, 3 BTC accounts, 1 ETH account, 3 BCH accounts
        captor.firstValue.size shouldEqual 10
    }

    @Test
    fun `onViewReady mode bitcoin`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.Bitcoin)
        val itemAccount0 = getItemAccount01()
        val itemAccount1 = getItemAccount01()
        val itemAccount2 = getItemAccount01()
        whenever(walletAccountHelper.accountList(CryptoCurrency.BTC))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        whenever(walletAccountHelper.importedList(CryptoCurrency.BTC))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).accountList(CryptoCurrency.BTC)
        verify(walletAccountHelper).importedList(CryptoCurrency.BTC)
        val captor = argumentCaptor<List<AccountChooserItem>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 3 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size shouldEqual 8
    }

    @Test
    fun `onViewReady mode bitcoin HD only`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.BitcoinHdOnly)
        val itemAccount0 = getItemAccount01()
        val itemAccount1 = getItemAccount01()
        val itemAccount2 = getItemAccount01()
        whenever(walletAccountHelper.accountList(CryptoCurrency.BTC))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).accountList(CryptoCurrency.BTC)
        val captor = argumentCaptor<List<AccountChooserItem>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 3 accounts only
        captor.firstValue.size shouldEqual 3
    }

    @Test
    fun `onViewReady mode bitcoin cash`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.BitcoinCash)
        val itemAccount0 = getItemAccount01()
        val itemAccount1 = getItemAccount01()
        val itemAccount2 = getItemAccount01()
        whenever(walletAccountHelper.accountList(CryptoCurrency.BCH))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).accountList(CryptoCurrency.BCH)
        val captor = argumentCaptor<List<AccountChooserItem>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 1 header, 3 accounts
        captor.firstValue.size shouldEqual 4
    }

    @Test
    fun `onViewReady mode bitcoin cash send`() {
        // Arrange
        whenever(activity.accountMode).thenReturn(AccountMode.BitcoinCashSend)
        val itemAccount0 = getItemAccount01()
        val itemAccount1 = getItemAccount01()
        val itemAccount2 = getItemAccount01()
        whenever(walletAccountHelper.accountList(CryptoCurrency.BCH))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        whenever(walletAccountHelper.importedList(CryptoCurrency.BCH))
            .thenReturn(Observable.just(listOf(itemAccount0, itemAccount1, itemAccount2)))
        // Act
        subject.onViewReady()
        // Assert
        verify(walletAccountHelper).accountList(CryptoCurrency.BCH)
        verify(walletAccountHelper).importedList(CryptoCurrency.BCH)
        val captor = argumentCaptor<List<AccountChooserItem>>()
        verify(activity).updateUi(captor.capture())
        // Value includes 2 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size shouldEqual 8
    }

    private fun getItemAccount0() = AccountChooserItem.Contact("", null)
    private fun getItemAccount01() = AccountChooserItem.Contact("", null)
}
