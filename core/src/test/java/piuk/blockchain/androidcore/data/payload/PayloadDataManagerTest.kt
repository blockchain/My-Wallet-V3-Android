package piuk.blockchain.androidcore.data.payload

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.Balance
import info.blockchain.wallet.metadata.MetadataNodeFactory
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Completable
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.bitcoinj.core.ECKey
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import piuk.blockchain.androidcore.RxTest
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.math.BigInteger
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class PayloadDataManagerTest : RxTest() {

    private lateinit var subject: PayloadDataManager
    private val payloadService: PayloadService = mock()
    private val payloadManager: PayloadManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val privateKeyFactory: PrivateKeyFactory = mock()
    private val rxBus = RxBus()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = PayloadDataManager(
            payloadService,
            privateKeyFactory,
            payloadManager,
            rxBus
        )
    }

    @Test
    @Throws(Exception::class)
    fun initializeFromPayload() {
        // Arrange
        val payload = "{}"
        val password = "PASSWORD"
        whenever(payloadService.initializeFromPayload(payload, password))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.initializeFromPayload(payload, password).test()
        // Assert
        verify(payloadService).initializeFromPayload(payload, password)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun restoreHdWallet() {
        // Arrange
        val mnemonic = "MNEMONIC"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val password = "PASSWORD"
        val mockWallet: Wallet = mock()
        whenever(payloadService.restoreHdWallet(mnemonic, walletName, email, password))
            .thenReturn(Observable.just(mockWallet))
        // Act
        val testObserver = subject.restoreHdWallet(mnemonic, walletName, email, password).test()
        // Assert
        verify(payloadService).restoreHdWallet(mnemonic, walletName, email, password)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    @Throws(Exception::class)
    fun createHdWallet() {
        // Arrange
        val password = "PASSWORD"
        val email = "EMAIL"
        val walletName = "WALLET_NAME"
        val mockWallet: Wallet = mock()
        whenever(payloadService.createHdWallet(password, walletName, email))
            .thenReturn(Observable.just(mockWallet))
        // Act
        val testObserver = subject.createHdWallet(password, walletName, email).test()
        // Assert
        verify(payloadService).createHdWallet(password, walletName, email)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    @Throws(Exception::class)
    fun initializeAndDecrypt() {
        // Arrange
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val password = "PASSWORD"
        whenever(payloadService.initializeAndDecrypt(sharedKey, guid, password))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.initializeAndDecrypt(sharedKey, guid, password).test()
        // Assert
        verify(payloadService).initializeAndDecrypt(sharedKey, guid, password)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun handleQrCode() {
        // Arrange
        val data = "DATA"
        whenever(payloadService.handleQrCode(data)).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.handleQrCode(data).test()
        // Assert
        verify(payloadService).handleQrCode(data)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun upgradeV2toV3() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        val defaultAccountName = "DEFAULT_ACCOUNT_NAME"
        whenever(payloadService.upgradeV2toV3(secondPassword, defaultAccountName))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.upgradeV2toV3(secondPassword, defaultAccountName).test()
        // Assert
        verify(payloadService).upgradeV2toV3(secondPassword, defaultAccountName)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun syncPayloadWithServer() {
        // Arrange
        whenever(payloadService.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.syncPayloadWithServer().test()
        // Assert
        verify(payloadService).syncPayloadWithServer()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun syncPayloadAndPublicKeys() {
        // Arrange
        whenever(payloadService.syncPayloadAndPublicKeys()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.syncPayloadAndPublicKeys().test()
        // Assert
        verify(payloadService).syncPayloadAndPublicKeys()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateAllTransactions() {
        // Arrange
        whenever(payloadService.updateAllTransactions()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateAllTransactions().test()
        // Assert
        verify(payloadService).updateAllTransactions()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateAllBalances() {
        // Arrange
        whenever(payloadService.updateAllBalances()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateAllBalances().test()
        // Assert
        verify(payloadService).updateAllBalances()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateTransactionNotes() {
        // Arrange
        val txHash = "TX_HASH"
        val note = "note"
        whenever(payloadService.updateTransactionNotes(txHash, note))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateTransactionNotes(txHash, note).test()
        // Assert
        verify(payloadService).updateTransactionNotes(txHash, note)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun getBalanceOfAddresses() {
        // Arrange
        val address = "ADDRESS"
        val hashMap: LinkedHashMap<String, Balance> = LinkedHashMap(mapOf(Pair(address, Balance())))
        whenever(payloadService.getBalanceOfAddresses(listOf(address)))
            .thenReturn(Observable.just(hashMap))
        // Act
        val testObserver = subject.getBalanceOfAddresses(listOf(address)).test()
        // Assert
        verify(payloadService).getBalanceOfAddresses(listOf(address))
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(hashMap)
    }

    @Test
    @Throws(Exception::class)
    fun getBalanceOfBchAddresses() {
        // Arrange
        val address = "ADDRESS"
        val hashMap: LinkedHashMap<String, Balance> = LinkedHashMap(mapOf(Pair(address, Balance())))
        whenever(payloadService.getBalanceOfBchAddresses(listOf(address)))
            .thenReturn(Observable.just(hashMap))
        // Act
        val testObserver = subject.getBalanceOfBchAddresses(listOf(address)).test()
        // Assert
        verify(payloadService).getBalanceOfBchAddresses(listOf(address))
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(hashMap)
    }

    @Test
    @Throws(Exception::class)
    fun addressToLabel() {
        // Arrange
        val address = "ADDRESS"
        val label = "label"
        whenever(payloadManager.getLabelFromAddress(address)).thenReturn(label)
        // Act
        val result = subject.addressToLabel(address)
        // Assert
        verify(payloadManager).getLabelFromAddress(address)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual label
    }

    @Test
    @Throws(Exception::class)
    fun `getNextReceiveAddress based on account index`() {
        // Arrange
        val index = 0
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        val address = "ADDRESS"
        whenever(payloadManager.payload.hdWallets.first().accounts).thenReturn(accounts)
        whenever(payloadManager.getNextReceiveAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextReceiveAddress(index).test()
        testScheduler.triggerActions()
        // Assert
        verify(payloadManager).getNextReceiveAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun `getNextReceiveAddress from account`() {
        // Arrange
        val mockAccount: Account = mock()
        val address = "ADDRESS"
        whenever(payloadManager.getNextReceiveAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextReceiveAddress(mockAccount).test()
        testScheduler.triggerActions()
        // Assert
        verify(payloadManager).getNextReceiveAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun getNextReceiveAddressAndReserve() {
        // Arrange
        val accountIndex = 0
        val addressLabel = "ADDRESS_LABEL"
        val address = "ADDRESS"
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        whenever(payloadManager.payload.hdWallets[0].accounts).thenReturn(accounts)
        whenever(payloadManager.getNextReceiveAddressAndReserve(mockAccount, addressLabel))
            .thenReturn(address)
        // Act
        val testObserver =
            subject.getNextReceiveAddressAndReserve(accountIndex, addressLabel).test()
        testScheduler.triggerActions()
        // Assert
        verify(payloadManager).getNextReceiveAddressAndReserve(mockAccount, addressLabel)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun `getNextChangeAddress based on account index`() {
        // Arrange
        val index = 0
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        val address = "ADDRESS"
        whenever(payloadManager.payload.hdWallets[0].accounts).thenReturn(accounts)
        whenever(payloadManager.getNextChangeAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextChangeAddress(index).test()
        testScheduler.triggerActions()
        // Assert
        verify(payloadManager).getNextChangeAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun `getNextChangeAddress from account`() {
        // Arrange
        val mockAccount: Account = mock()
        val address = "ADDRESS"
        whenever(payloadManager.getNextChangeAddress(mockAccount)).thenReturn(address)
        // Act
        val testObserver = subject.getNextChangeAddress(mockAccount).test()
        testScheduler.triggerActions()
        // Assert
        verify(payloadManager).getNextChangeAddress(mockAccount)
        testObserver.assertComplete()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun getAddressECKey() {
        // Arrange
        val mockLegacyAddress: LegacyAddress = mock()
        val secondPassword = "SECOND_PASSWORD"
        val mockEcKey: ECKey = mock()
        whenever(payloadManager.getAddressECKey(mockLegacyAddress, secondPassword))
            .thenReturn(mockEcKey)
        // Act
        val result = subject.getAddressECKey(mockLegacyAddress, secondPassword)
        // Assert
        verify(payloadManager).getAddressECKey(mockLegacyAddress, secondPassword)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual mockEcKey
    }

    @Test
    @Throws(Exception::class)
    fun createNewAccount() {
        // Arrange
        val mockAccount: Account = mock()
        whenever(payloadService.createNewAccount(ArgumentMatchers.anyString(), isNull<String>()))
            .thenReturn(Observable.just(mockAccount))
        // Act
        val observer = subject.createNewAccount("", null).test()
        // Assert
        verify(payloadService).createNewAccount("", null)
        observer.assertNoErrors()
        observer.assertComplete()
        assertEquals(mockAccount, observer.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun setPrivateKeySuccessNoDoubleEncryption() {
        // Arrange
        val mockECKey: ECKey = mock()
        val mockLegacyAddress: LegacyAddress = mock()
        whenever(payloadService.setKeyForLegacyAddress(eq(mockECKey), isNull<String>()))
            .thenReturn(Observable.just(mockLegacyAddress))
        // Act
        val observer = subject.setKeyForLegacyAddress(mockECKey, null).test()
        // Assert
        verify(payloadService).setKeyForLegacyAddress(eq(mockECKey), isNull<String>())
        observer.assertNoErrors()
        observer.assertComplete()
        assertEquals(mockLegacyAddress, observer.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun setKeyForLegacyAddress() {
        // Arrange
        val mockECKey: ECKey = mock()
        val password = "PASSWORD"
        val mockLegacyAddress: LegacyAddress = mock()
        whenever(payloadService.setKeyForLegacyAddress(mockECKey, password))
            .thenReturn(Observable.just(mockLegacyAddress))
        // Act
        val observer = subject.setKeyForLegacyAddress(mockECKey, password).test()
        // Assert
        verify(payloadService).setKeyForLegacyAddress(mockECKey, password)
        observer.assertNoErrors()
        observer.assertComplete()
        assertEquals(mockLegacyAddress, observer.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun addLegacyAddress() {
        // Arrange
        val mockLegacyAddress: LegacyAddress = mock()
        whenever(payloadService.addLegacyAddress(mockLegacyAddress)).thenReturn(Completable.complete())
        // Act
        val observer = subject.addLegacyAddress(mockLegacyAddress).test()
        // Assert
        verify(payloadService).addLegacyAddress(mockLegacyAddress)
        observer.assertNoErrors()
        observer.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun updateLegacyAddress() {
        // Arrange
        val mockLegacyAddress: LegacyAddress = mock()
        whenever(payloadService.updateLegacyAddress(mockLegacyAddress)).thenReturn(Completable.complete())
        // Act
        val observer = subject.updateLegacyAddress(mockLegacyAddress).test()
        // Assert
        verify(payloadService).updateLegacyAddress(mockLegacyAddress)
        observer.assertNoErrors()
        observer.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun getKeyFromImportedData() {
        // Arrange
        val data = "DATA"
        val mockEcKey: ECKey = mock()
        whenever(privateKeyFactory.getKey(PrivateKeyFactory.BASE58, data)).thenReturn(mockEcKey)
        // Act
        val testObserver = subject.getKeyFromImportedData(PrivateKeyFactory.BASE58, data).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(mockEcKey)
    }

    @Test
    @Throws(Exception::class)
    fun `getAccounts returns list of accounts`() {
        // Arrange
        val mockAccount: Account = mock()
        val accounts = listOf(mockAccount)
        whenever(payloadManager.payload.hdWallets.first().accounts)
            .thenReturn(accounts)
        // Act
        val result = subject.accounts
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result shouldEqual accounts
    }

    @Test
    @Throws(Exception::class)
    fun `getAccounts returns empty list`() {
        // Arrange
        whenever(payloadManager.payload).thenReturn(null)
        // Act
        val result = subject.accounts
        // Assert
        verify(payloadManager).payload
        result shouldEqual emptyList()
    }

    @Test
    @Throws(Exception::class)
    fun `getLegacyAddresses returns list of legacy addresses`() {
        // Arrange
        val mockLegacyAddress: LegacyAddress = mock()
        val addresses = listOf(mockLegacyAddress)
        whenever(payloadManager.payload.legacyAddressList).thenReturn(addresses)
        // Act
        val result = subject.legacyAddresses
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result shouldEqual addresses
    }

    @Test
    @Throws(Exception::class)
    fun `getLegacyAddresses returns empty list`() {
        // Arrange
        whenever(payloadManager.payload).thenReturn(null)
        // Act
        val result = subject.legacyAddresses
        // Assert
        verify(payloadManager).payload
        result shouldEqual emptyList()
    }

    @Test
    @Throws(Exception::class)
    fun getAddressBalance() {
        // Arrange
        val address = "ADDRESS"
        val balance = BigInteger.TEN
        whenever(payloadManager.getAddressBalance(address))
            .thenReturn(balance)
        // Act
        val result = subject.getAddressBalance(address)
        // Assert
        verify(payloadManager).getAddressBalance(address)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual balance
    }

    @Test
    @Throws(Exception::class)
    fun getReceiveAddressAtPosition() {
        // Arrange
        val mockAccount: Account = mock()
        val position = 1337
        val address = "ADDRESS"
        whenever(payloadManager.getReceiveAddressAtPosition(mockAccount, position))
            .thenReturn(address)
        // Act
        val result = subject.getReceiveAddressAtPosition(mockAccount, position)
        // Assert
        verify(payloadManager).getReceiveAddressAtPosition(mockAccount, position)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual address
    }

    @Test
    @Throws(Exception::class)
    fun getReceiveAddressAtArbitraryPosition() {
        // Arrange
        val mockAccount: Account = mock()
        val position = 1337
        val address = "ADDRESS"
        whenever(payloadManager.getReceiveAddressAtArbitraryPosition(mockAccount, position))
            .thenReturn(address)
        // Act
        val result = subject.getReceiveAddressAtArbitraryPosition(mockAccount, position)
        // Assert
        verify(payloadManager).getReceiveAddressAtArbitraryPosition(mockAccount, position)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual address
    }

    @Test
    @Throws(Exception::class)
    fun subtractAmountFromAddressBalance() {
        // Arrange
        val address = "ADDRESS"
        val amount = 1_000_000L
        // Act
        subject.subtractAmountFromAddressBalance(address, amount)
        // Assert
        verify(payloadManager).subtractAmountFromAddressBalance(address, BigInteger.valueOf(amount))
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun incrementReceiveAddress() {
        // Arrange
        val mockAccount: Account = mock()
        // Act
        subject.incrementReceiveAddress(mockAccount)
        // Assert
        verify(payloadManager).incrementNextReceiveAddress(mockAccount)
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun incrementChangeAddress() {
        // Arrange
        val mockAccount: Account = mock()
        // Act
        subject.incrementChangeAddress(mockAccount)
        // Assert
        verify(payloadManager).incrementNextChangeAddress(mockAccount)
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun getXpubFromAddress() {
        // Arrange
        val xPub = "X_PUB"
        val address = "ADDRESS"
        whenever(payloadManager.getXpubFromAddress(address))
            .thenReturn(xPub)
        // Act
        val result = subject.getXpubFromAddress(address)
        // Assert
        verify(payloadManager).getXpubFromAddress(address)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual xPub
    }

    @Test
    @Throws(Exception::class)
    fun getXpubFromIndex() {
        // Arrange
        val xPub = "X_PUB"
        val index = 42
        whenever(payloadManager.getXpubFromAccountIndex(index))
            .thenReturn(xPub)
        // Act
        val result = subject.getXpubFromIndex(index)
        // Assert
        verify(payloadManager).getXpubFromAccountIndex(index)
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual xPub
    }

    @Test
    @Throws(Exception::class)
    fun isOwnHDAddress() {
        // Arrange
        val address = "ADDRESS"
        whenever(payloadManager.isOwnHDAddress(address)).thenReturn(true)
        // Act
        val result = subject.isOwnHDAddress(address)
        // Assert
        result shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun loadNodes() {
        // Arrange
        whenever(payloadService.loadNodes()).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.loadNodes().test()
        // Assert
        verify(payloadService).loadNodes()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(true)
    }

    @Test
    @Throws(Exception::class)
    fun generateNodes() {
        // Arrange
        whenever(payloadService.generateNodes())
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.generateNodes().test()
        // Assert
        verify(payloadService).generateNodes()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    @Throws(Exception::class)
    fun getMetadataNodeFactory() {
        // Arrange
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(payloadManager.metadataNodeFactory).thenReturn(mockNodeFactory)
        // Act
        val testObserver = subject.getMetadataNodeFactory().test()
        // Assert
        verify(payloadManager).metadataNodeFactory
        verifyNoMoreInteractions(payloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockNodeFactory)
    }

    @Test
    @Throws(Exception::class)
    fun registerMdid() {
        // Arrange
        val responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}")
        whenever(payloadService.registerMdid()).thenReturn(Observable.just(responseBody))
        // Act
        val testObserver = subject.registerMdid().test()
        // Assert
        verify(payloadService).registerMdid()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(responseBody)
    }

    @Test
    @Throws(Exception::class)
    fun unregisterMdid() {
        // Arrange
        val responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}")
        whenever(payloadService.unregisterMdid()).thenReturn(Observable.just(responseBody))
        // Act
        val testObserver = subject.unregisterMdid().test()
        // Assert
        verify(payloadService).unregisterMdid()
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
        testObserver.assertValue(responseBody)
    }

    @Test
    @Throws(Exception::class)
    fun `getWallet returns wallet`() {
        // Arrange
        val mockWallet: Wallet = mock()
        whenever(payloadManager.payload).thenReturn(mockWallet)
        // Act
        val result = subject.wallet
        // Assert
        verify(payloadManager).payload
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual mockWallet
    }

    @Test
    @Throws(Exception::class)
    fun `getWallet returns null`() {
        // Arrange
        whenever(payloadManager.payload).thenReturn(null)
        // Act
        val result = subject.wallet
        // Assert
        verify(payloadManager).payload
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual null
    }

    @Test
    @Throws(Exception::class)
    fun getDefaultAccountIndex() {
        // Arrange
        val index = 42
        whenever(payloadManager.payload.hdWallets.first().defaultAccountIdx).thenReturn(index)
        // Act
        val result = subject.defaultAccountIndex
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result shouldEqual index
    }

    @Test
    @Throws(Exception::class)
    fun getDefaultAccount() {
        // Arrange
        val index = 42
        val mockAccount: Account = mock()
        whenever(payloadManager.payload.hdWallets.first().defaultAccountIdx)
            .thenReturn(index)
        whenever(payloadManager.payload.hdWallets.first().getAccount(index))
            .thenReturn(mockAccount)
        // Act
        val result = subject.defaultAccount
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result shouldEqual mockAccount
    }

    @Test
    @Throws(Exception::class)
    fun getAccount() {
        // Arrange
        val index = 42
        val mockAccount: Account = mock()
        whenever(payloadManager.payload.hdWallets.first().getAccount(index))
            .thenReturn(mockAccount)
        // Act
        val result = subject.getAccount(index)
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result shouldEqual mockAccount
    }

    @Test
    fun getTransactionNotes() {
        // Arrange
        val txHash = "TX_HASH"
        val note = "NOTES"
        val map = mapOf(txHash to note)
        whenever(payloadManager.payload.txNotes).thenReturn(map)
        // Act
        val result = subject.getTransactionNotes(txHash)
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result `should equal` note
    }

    @Test
    @Throws(Exception::class)
    fun getHDKeysForSigning() {
        // Arrange
        val mockAccount: Account = mock()
        val mockOutputs: SpendableUnspentOutputs = mock()
        val mockEcKey: ECKey = mock()
        whenever(
            payloadManager.payload.hdWallets.first().getHDKeysForSigning(
                mockAccount,
                mockOutputs
            )
        )
            .thenReturn(listOf(mockEcKey))
        // Act
        val result = subject.getHDKeysForSigning(mockAccount, mockOutputs)
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result shouldEqual listOf(mockEcKey)
    }

    @Test
    @Throws(Exception::class)
    fun getPayloadChecksum() {
        // Arrange
        val checkSum = "CHECKSUM"
        whenever(payloadManager.payloadChecksum).thenReturn(checkSum)
        // Act
        val result = subject.payloadChecksum
        // Assert
        verify(payloadManager).payloadChecksum
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual checkSum
    }

    @Test
    @Throws(Exception::class)
    fun getTempPassword() {
        // Arrange
        val tempPassword = "TEMP_PASSWORD"
        whenever(payloadManager.tempPassword).thenReturn(tempPassword)
        // Act
        val result = subject.tempPassword
        // Assert
        verify(payloadManager).tempPassword
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual tempPassword
    }

    @Test
    @Throws(Exception::class)
    fun setTempPassword() {
        // Arrange
        val tempPassword = "TEMP_PASSWORD"
        // Act
        subject.tempPassword = tempPassword
        // Assert
        verify(payloadManager).tempPassword = tempPassword
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    @Throws(Exception::class)
    fun getImportedAddressesBalance() {
        // Arrange
        val balance = BigInteger.TEN
        whenever(payloadManager.importedAddressesBalance).thenReturn(balance)
        // Act
        val result = subject.importedAddressesBalance
        // Assert
        verify(payloadManager).importedAddressesBalance
        verifyNoMoreInteractions(payloadManager)
        result shouldEqual balance
    }

    @Test
    @Throws(Exception::class)
    fun isDoubleEncrypted() {
        // Arrange
        whenever(payloadManager.payload.isDoubleEncryption).thenReturn(true)
        // Act
        val result = subject.isDoubleEncrypted
        // Assert
        result shouldEqual true
    }

    @Test
    @Throws(Exception::class)
    fun getPositionOfAccountFromActiveList() {
        // Arrange
        val index = 1
        val account0 = Account().apply { isArchived = true }
        val account1 = Account()
        val account2 = Account().apply { isArchived = true }
        val account3 = Account()
        whenever(payloadManager.payload.hdWallets.first().accounts)
            .thenReturn(listOf(account0, account1, account2, account3))
        // Act
        val result = subject.getPositionOfAccountFromActiveList(index)
        // Assert
        result shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun getPositionOfAccountInActiveList() {
        // Arrange
        val index = 3
        val account0 = Account().apply { isArchived = true }
        val account1 = Account()
        val account2 = Account().apply { isArchived = true }
        val account3 = Account()
        whenever(payloadManager.payload.hdWallets.first().accounts)
            .thenReturn(listOf(account0, account1, account2, account3))
        // Act
        val result = subject.getPositionOfAccountInActiveList(index)
        // Assert
        result shouldEqual 1
    }
}