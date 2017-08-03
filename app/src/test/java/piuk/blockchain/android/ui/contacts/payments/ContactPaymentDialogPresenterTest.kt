//package piuk.blockchain.android.ui.contacts.payments
//
//import android.os.Bundle
//import com.nhaarman.mockito_kotlin.*
//import info.blockchain.api.data.UnspentOutput
//import info.blockchain.api.data.UnspentOutputs
//import info.blockchain.wallet.api.data.FeeOptions
//import info.blockchain.wallet.contacts.data.Contact
//import info.blockchain.wallet.payload.data.Account
//import info.blockchain.wallet.payload.data.Wallet
//import info.blockchain.wallet.payment.SpendableUnspentOutputs
//import io.reactivex.Observable
//import org.amshove.kluent.shouldEqual
//import org.apache.commons.lang3.tuple.Pair
//import org.bitcoinj.core.ECKey
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.Mockito.RETURNS_DEEP_STUBS
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config
//import piuk.blockchain.android.BlockchainTestApplication
//import piuk.blockchain.android.BuildConfig
//import piuk.blockchain.android.data.cache.DynamicFeeCache
//import piuk.blockchain.android.data.contacts.ContactsDataManager
//import piuk.blockchain.android.data.datamanagers.FeeDataManager
//import piuk.blockchain.android.data.payload.PayloadDataManager
//import piuk.blockchain.android.data.payments.SendDataManager
//import piuk.blockchain.android.ui.account.ItemAccount
//import piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialog.ARGUMENT_CONTACT_ID
//import piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialog.ARGUMENT_URI
//import piuk.blockchain.android.ui.customviews.ToastCustom
//import piuk.blockchain.android.ui.receive.WalletAccountHelper
//import piuk.blockchain.android.ui.send.PendingTransaction
//import piuk.blockchain.android.util.ExchangeRateFactory
//import piuk.blockchain.android.util.PrefsUtil
//import java.math.BigInteger
//import java.util.*
//import kotlin.test.assertFalse
//import kotlin.test.assertNotNull
//import kotlin.test.assertTrue
//
//@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
//@RunWith(RobolectricTestRunner::class)
//class ContactPaymentDialogPresenterTest {
//
//    private lateinit var subject: ContactPaymentDialogPresenter
//    private val mockContactsDataManager: ContactsDataManager = mock()
//    private val mockPrefsUtil: PrefsUtil = mock()
//    private val mockExchangeRateFactory: ExchangeRateFactory = mock()
//    private val mockWalletAccountHelper: WalletAccountHelper = mock()
//    private val mockPayloadDataManager: PayloadDataManager = mock()
//    private val mockSendDataManager: SendDataManager = mock()
//    private val mockDynamicFeeCache: DynamicFeeCache = mock()
//    private val mockFeeDataManager: FeeDataManager = mock()
//    private val mockActivity: ContactPaymentDialogView = mock()
//    private val mockFeeOptions: FeeOptions = mock(defaultAnswer = RETURNS_DEEP_STUBS)
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        whenever(mockFeeDataManager.feeOptions).thenReturn(Observable.just(mockFeeOptions))
//        whenever(mockDynamicFeeCache.feeOptions).thenReturn(mockFeeOptions)
//        subject = ContactPaymentDialogPresenter(
//                mockContactsDataManager,
//                mockPrefsUtil,
//                mockExchangeRateFactory,
//                mockWalletAccountHelper,
//                mockPayloadDataManager,
//                mockSendDataManager,
//                mockDynamicFeeCache,
//                mockFeeDataManager
//        )
//        subject.initView(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onViewReadyBundleIsNull() {
//        // Arrange
//        whenever(mockActivity.fragmentBundle).thenReturn(null)
//        // Act
//        subject.onViewReady()
//        // Assert
//        verify(mockActivity).fragmentBundle
//        verify(mockActivity).finishPage(false)
//        verifyNoMoreInteractions(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onViewReadyBundleIsEmpty() {
//        // Arrange
//        val bundle = Bundle()
//        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
//        // Act
//        subject.onViewReady()
//        // Assert
//        verify(mockActivity).fragmentBundle
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verify(mockActivity).finishPage(false)
//        verifyNoMoreInteractions(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onViewReadyContactNotFound() {
//        // Arrange
//        val bundle = Bundle()
//        val contactId = "CONTACT_ID"
//        bundle.putString(ARGUMENT_CONTACT_ID, contactId)
//        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
//        whenever(mockContactsDataManager.getContactList()).thenReturn(Observable.just(Contact()))
//        // Act
//        subject.onViewReady()
//        // Assert
//        verify(mockActivity).fragmentBundle
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verify(mockActivity).finishPage(false)
//        verifyNoMoreInteractions(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onViewReadyInvalidBitcoinAddress() {
//        // Arrange
//        val bundle = Bundle()
//        val contactId = "CONTACT_ID"
//        val contactName = "CONTACT_NAME"
//        val contact = Contact().apply {
//            id = contactId
//            name = contactName
//        }
//        val uri = "bitcoin:1LTMmoCoAh5S3SJqCkdCEw9TD?amount=0.12345678"
//        bundle.putString(ARGUMENT_CONTACT_ID, contactId)
//        bundle.putString(ARGUMENT_URI, uri)
//        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
//        whenever(mockContactsDataManager.getContactList()).thenReturn(Observable.just(contact))
//        // Act
//        subject.onViewReady()
//        // Assert
//        verify(mockActivity).fragmentBundle
//        verify(mockActivity).setContactName(contactName)
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verify(mockActivity).finishPage(false)
//        verifyNoMoreInteractions(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onViewReady() {
//        // Arrange
//        val bundle = Bundle()
//        val contactId = "CONTACT_ID"
//        val contactName = "CONTACT_NAME"
//        val contact = Contact().apply {
//            id = contactId
//            name = contactName
//        }
//        val uri = "bitcoin:1LTMmoCoAh5S3SJqCkdCEw9TDfagAyxkRU?amount=0.12345678"
//        bundle.putString(ARGUMENT_CONTACT_ID, contactId)
//        bundle.putString(ARGUMENT_URI, uri)
//        whenever(mockActivity.fragmentBundle).thenReturn(bundle)
//        whenever(mockContactsDataManager.getContactList()).thenReturn(Observable.just(contact))
//        // Act
//        subject.onViewReady()
//        // Assert
//        verify(mockActivity).fragmentBundle
//        verify(mockActivity).setContactName(contactName)
//        subject.pendingTransaction.receivingAddress shouldEqual "1LTMmoCoAh5S3SJqCkdCEw9TDfagAyxkRU"
//        subject.pendingTransaction.bigIntAmount shouldEqual BigInteger.valueOf(12345678L)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getSendFromList() {
//        // Arrange
//        val list = listOf(ItemAccount())
//        whenever(mockWalletAccountHelper.getHdAccounts(true))
//                .thenReturn(list)
//        // Act
//        val result = subject.sendFromList
//        // Assert
//        verify(mockWalletAccountHelper).getHdAccounts(true)
//        verifyNoMoreInteractions(mockWalletAccountHelper)
//        result shouldEqual list
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getDefaultAccountPosition() {
//        // Arrange
//        val defaultIndex = 1337
//        val positionToReturn = 9001
//        whenever(mockPayloadDataManager.defaultAccountIndex).thenReturn(defaultIndex)
//        whenever(mockPayloadDataManager.getPositionOfAccountInActiveList(defaultIndex))
//                .thenReturn(positionToReturn)
//        // Act
//        val result = subject.defaultAccountPosition
//        // Assert
//        verify(mockPayloadDataManager).defaultAccountIndex
//        verify(mockPayloadDataManager).getPositionOfAccountInActiveList(defaultIndex)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//        result shouldEqual positionToReturn
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun accountSelectedNoFunds() {
//        // Arrange
//        val accountPosition = 1337
//        val xPub = "X_PUB"
//        val account = Account().apply {
//            label = "ACCOUNT_LABEL"
//            xpub = xPub
//        }
//        whenever(mockPayloadDataManager.getPositionOfAccountFromActiveList(accountPosition))
//                .thenReturn(accountPosition)
//        whenever(mockPayloadDataManager.getAccount(accountPosition)).thenReturn(account)
//        whenever(mockSendDataManager.getUnspentOutputs(xPub))
//                .thenReturn(Observable.error { Throwable() })
//        // Act
//        subject.accountSelected(accountPosition)
//        // Assert
//        verify(mockPayloadDataManager).getPositionOfAccountFromActiveList(accountPosition)
//        verify(mockPayloadDataManager).getAccount(accountPosition)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//        verify(mockSendDataManager).getUnspentOutputs(xPub)
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verifyNoMoreInteractions(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun accountSelectedFeeListNull() {
//        // Arrange
//        val accountPosition = 1337
//        val xPub = "X_PUB"
//        val account = Account().apply {
//            label = "ACCOUNT_LABEL"
//            xpub = xPub
//        }
//        whenever(mockPayloadDataManager.getPositionOfAccountFromActiveList(accountPosition))
//                .thenReturn(accountPosition)
//        whenever(mockPayloadDataManager.getAccount(accountPosition)).thenReturn(account)
//        val unspentOutputs = UnspentOutputs()
//        whenever(mockSendDataManager.getUnspentOutputs(xPub)).thenReturn(Observable.just(unspentOutputs))
//        whenever(mockDynamicFeeCache.feeOptions).thenReturn(null)
//        subject.feeOptions = null
//        // Act
//        subject.accountSelected(accountPosition)
//        // Assert
//        verify(mockPayloadDataManager).getPositionOfAccountFromActiveList(accountPosition)
//        verify(mockPayloadDataManager).getAccount(accountPosition)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//        verify(mockSendDataManager).getUnspentOutputs(xPub)
//        verify(mockActivity).finishPage(false)
//        verifyNoMoreInteractions(mockActivity)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun accountSelected() {
//        // Arrange
//        val accountPosition = 1337
//        val xPub = "X_PUB"
//        val account = Account().apply {
//            label = "ACCOUNT_LABEL"
//            xpub = xPub
//        }
//        val unspentOutputs = UnspentOutputs()
//        val spendableUnspent = SpendableUnspentOutputs().apply { absoluteFee = BigInteger.TEN }
//        val pair = Pair.of(BigInteger.TEN, BigInteger.TEN)
//        val amount = BigInteger.valueOf(12345678L)
//        whenever(mockPayloadDataManager.getPositionOfAccountFromActiveList(accountPosition))
//                .thenReturn(accountPosition)
//        whenever(mockPayloadDataManager.getAccount(accountPosition)).thenReturn(account)
//        whenever(mockSendDataManager.getUnspentOutputs(xPub)).thenReturn(Observable.just(unspentOutputs))
//        whenever(mockFeeOptions.regularFee).thenReturn(1L)
//        whenever(mockSendDataManager.getSpendableCoins(eq(unspentOutputs), eq(amount), any()))
//                .thenReturn(spendableUnspent)
//        whenever(mockSendDataManager.getSweepableCoins(eq(unspentOutputs), any())).thenReturn(pair)
//        whenever(mockPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("USD")
//        subject.pendingTransaction.bigIntAmount = amount
//        // Act
//        subject.accountSelected(accountPosition)
//        // Assert
//        verify(mockPayloadDataManager).getPositionOfAccountFromActiveList(accountPosition)
//        verify(mockPayloadDataManager).getAccount(accountPosition)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//        verify(mockSendDataManager).getUnspentOutputs(xPub)
//        assertNotNull(subject.pendingTransaction.sendingObject)
//        verify(mockActivity).updatePaymentAmountBtc(any())
//        verify(mockActivity).updatePaymentAmountFiat(any())
//        verify(mockActivity).updateFeeAmountBtc(any())
//        verify(mockActivity).updateFeeAmountFiat(any())
//        verify(mockActivity).setPaymentButtonEnabled(any())
//        verify(mockActivity).onUiUpdated()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onSendClickedException() {
//        // Arrange
//        val account = Account()
//        subject.pendingTransaction.sendingObject = ItemAccount("", "", null, null, account, null)
//        whenever(mockPayloadDataManager.getNextChangeAddress(account))
//                .thenReturn(Observable.error { Throwable() })
//        // Act
//        subject.onSendClicked(null)
//        // Assert
//        verify(mockActivity).showProgressDialog()
//        verify(mockActivity).hideProgressDialog()
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verifyNoMoreInteractions(mockActivity)
//        verify(mockPayloadDataManager).getNextChangeAddress(account)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onSendClicked() {
//        // Arrange
//        val changeAddress = "CHANGE_ADDRESS"
//        val receiveAddress = "RECEIVE_ADDRESS"
//        val xPub = "X_PUB"
//        val account = Account().apply { xpub = xPub }
//        val spendableOutputs = SpendableUnspentOutputs()
//        val keys = listOf(ECKey())
//        val txHash = "TX_HASH"
//        subject.pendingTransaction.apply {
//            unspentOutputBundle = spendableOutputs
//            sendingObject = ItemAccount("", "", null, null, account, null)
//            bigIntAmount = BigInteger.TEN
//            bigIntFee = BigInteger.TEN
//            receivingAddress = receiveAddress
//        }
//        whenever(mockPayloadDataManager.getNextChangeAddress(account))
//                .thenReturn(Observable.just(changeAddress))
//        whenever(mockPayloadDataManager.isDoubleEncrypted).thenReturn(false)
//        whenever(mockPayloadDataManager.getHDKeysForSigning(account, spendableOutputs))
//                .thenReturn(keys)
//        whenever(mockSendDataManager.submitPayment(
//                spendableOutputs,
//                keys,
//                receiveAddress,
//                changeAddress,
//                BigInteger.TEN,
//                BigInteger.TEN)).thenReturn(Observable.just(txHash))
//        // Act
//        subject.onSendClicked(null)
//        // Assert
//        verify(mockActivity).showProgressDialog()
//        verify(mockActivity).hideProgressDialog()
//        verify(mockActivity).onShowTransactionSuccess(null, txHash, null, 10L)
//        verifyNoMoreInteractions(mockActivity)
//        verify(mockPayloadDataManager).getNextChangeAddress(account)
//        verify(mockPayloadDataManager).isDoubleEncrypted
//        verify(mockPayloadDataManager).getHDKeysForSigning(account, spendableOutputs)
//        verify(mockPayloadDataManager).incrementChangeAddress(account)
//        verify(mockPayloadDataManager).incrementReceiveAddress(account)
//        verify(mockPayloadDataManager).subtractAmountFromAddressBalance(xPub, 20L)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//        verify(mockSendDataManager).submitPayment(
//                spendableOutputs,
//                keys,
//                receiveAddress,
//                changeAddress,
//                BigInteger.TEN,
//                BigInteger.TEN)
//        verifyNoMoreInteractions(mockSendDataManager)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun onSendClickedSecondPassword() {
//        // Arrange
//        val changeAddress = "CHANGE_ADDRESS"
//        val receiveAddress = "RECEIVE_ADDRESS"
//        val xPub = "X_PUB"
//        val account = Account().apply { xpub = xPub }
//        val spendableOutputs = SpendableUnspentOutputs()
//        val keys = listOf(ECKey())
//        val txHash = "TX_HASH"
//        val mockWallet: Wallet = mock()
//        subject.pendingTransaction.apply {
//            unspentOutputBundle = spendableOutputs
//            sendingObject = ItemAccount("", "", null, null, account, null)
//            bigIntAmount = BigInteger.TEN
//            bigIntFee = BigInteger.TEN
//            receivingAddress = receiveAddress
//        }
//        whenever(mockPayloadDataManager.getNextChangeAddress(account))
//                .thenReturn(Observable.just(changeAddress))
//        whenever(mockPayloadDataManager.isDoubleEncrypted).thenReturn(true)
//        whenever(mockPayloadDataManager.getHDKeysForSigning(account, spendableOutputs))
//                .thenReturn(keys)
//        whenever(mockSendDataManager.submitPayment(
//                spendableOutputs,
//                keys,
//                receiveAddress,
//                changeAddress,
//                BigInteger.TEN,
//                BigInteger.TEN)).thenReturn(Observable.just(txHash))
//        whenever(mockPayloadDataManager.wallet).thenReturn(mockWallet)
//        // Act
//        subject.onSendClicked("password")
//        // Assert
//        verify(mockActivity).showProgressDialog()
//        verify(mockActivity).hideProgressDialog()
//        verify(mockActivity).onShowTransactionSuccess(null, txHash, null, 10L)
//        verifyNoMoreInteractions(mockActivity)
//        verify(mockPayloadDataManager).wallet
//        verify(mockPayloadDataManager).getNextChangeAddress(account)
//        verify(mockPayloadDataManager).isDoubleEncrypted
//        verify(mockPayloadDataManager).getHDKeysForSigning(account, spendableOutputs)
//        verify(mockPayloadDataManager).incrementChangeAddress(account)
//        verify(mockPayloadDataManager).incrementReceiveAddress(account)
//        verify(mockPayloadDataManager).subtractAmountFromAddressBalance(xPub, 20L)
//        verifyNoMoreInteractions(mockPayloadDataManager)
//        verify(mockSendDataManager).submitPayment(
//                spendableOutputs,
//                keys,
//                receiveAddress,
//                changeAddress,
//                BigInteger.TEN,
//                BigInteger.TEN)
//        verifyNoMoreInteractions(mockSendDataManager)
//    }
//
//    @Test
//    fun isValidSpendNoConfirmedFunds() {
//        // Arrange
//        val pendingTransaction = PendingTransaction()
//        // Act
//        val result = subject.isValidSpend(pendingTransaction)
//        // Assert
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verifyNoMoreInteractions(mockActivity)
//        assertFalse(result)
//    }
//
//    @Test
//    fun isValidSpendNotEnoughFunds() {
//        // Arrange
//        val spendableOutputs = SpendableUnspentOutputs().apply { spendableOutputs = Collections.emptyList() }
//        val pendingTransaction = PendingTransaction().apply {
//            unspentOutputBundle = spendableOutputs
//            bigIntAmount = BigInteger.TEN
//        }
//        subject.maxAvailable = BigInteger.ZERO
//        // Act
//        val result = subject.isValidSpend(pendingTransaction)
//        // Assert
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verifyNoMoreInteractions(mockActivity)
//        assertFalse(result)
//    }
//
//    @Test
//    fun isValidSpendOutputsIsEmpty() {
//        // Arrange
//        val spendableOutputs = SpendableUnspentOutputs().apply { spendableOutputs = Collections.emptyList() }
//        val pendingTransaction = PendingTransaction().apply {
//            unspentOutputBundle = spendableOutputs
//            bigIntAmount = BigInteger.ZERO
//        }
//        subject.maxAvailable = BigInteger.TEN
//        // Act
//        val result = subject.isValidSpend(pendingTransaction)
//        // Assert
//        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
//        verifyNoMoreInteractions(mockActivity)
//        assertFalse(result)
//    }
//
//    @Test
//    fun isValidSpend() {
//        // Arrange
//        val spendableOutputs = SpendableUnspentOutputs().apply { spendableOutputs = listOf(UnspentOutput()) }
//        val pendingTransaction = PendingTransaction().apply {
//            unspentOutputBundle = spendableOutputs
//            bigIntAmount = BigInteger.ZERO
//        }
//        subject.maxAvailable = BigInteger.TEN
//        // Act
//        val result = subject.isValidSpend(pendingTransaction)
//        // Assert
//        verifyZeroInteractions(mockActivity)
//        assertTrue(result)
//    }
//
//}