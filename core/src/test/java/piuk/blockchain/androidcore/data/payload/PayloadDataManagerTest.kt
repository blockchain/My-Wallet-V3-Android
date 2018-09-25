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
import io.reactivex.schedulers.TestScheduler
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import com.blockchain.android.testutils.rxInit
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.math.BigInteger
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class PayloadDataManagerTest {

    private lateinit var subject: PayloadDataManager
    private val payloadService: PayloadService = mock()
    private val payloadManager: PayloadManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val privateKeyFactory: PrivateKeyFactory = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val rxBus = RxBus()
    private val mainNetParams = BitcoinMainNetParams.get()
    private val testScheduler = TestScheduler()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computation(testScheduler)
    }

    @Before
    fun setUp() {

        whenever(environmentConfig.bitcoinNetworkParameters).thenReturn(mainNetParams)

        subject = PayloadDataManager(
            payloadService,
            privateKeyFactory,
            payloadManager,
            environmentConfig,
            rxBus
        )
    }

    @Test
    fun initializeFromPayload() {
        // Arrange
        val payload = "{}"
        val password = "PASSWORD"
        whenever(payloadService.initializeFromPayload(mainNetParams, payload, password))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.initializeFromPayload(payload, password).test()
        // Assert
        verify(payloadService).initializeFromPayload(mainNetParams, payload, password)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
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
    fun initializeAndDecrypt() {
        // Arrange
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val password = "PASSWORD"
        whenever(payloadService.initializeAndDecrypt(mainNetParams, sharedKey, guid, password))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.initializeAndDecrypt(sharedKey, guid, password).test()
        // Assert
        verify(payloadService).initializeAndDecrypt(mainNetParams, sharedKey, guid, password)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
    fun handleQrCode() {
        // Arrange
        val data = "DATA"
        whenever(
            payloadService.handleQrCode(
                mainNetParams,
                data
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.handleQrCode(data).test()
        // Assert
        verify(payloadService).handleQrCode(mainNetParams, data)
        verifyNoMoreInteractions(payloadService)
        testObserver.assertComplete()
    }

    @Test
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
    fun createNewAccount() {
        // Arrange
        val mockAccount: Account = mock()
        whenever(payloadService.createNewAccount(eq(mainNetParams), anyString(), isNull()))
            .thenReturn(Observable.just(mockAccount))
        // Act
        val observer = subject.createNewAccount("", null).test()
        // Assert
        verify(payloadService).createNewAccount(mainNetParams, "", null)
        observer.assertNoErrors()
        observer.assertComplete()
        assertEquals(mockAccount, observer.values()[0])
    }

    @Test
    fun setPrivateKeySuccessNoDoubleEncryption() {
        // Arrange
        val mockECKey: ECKey = mock()
        val mockLegacyAddress: LegacyAddress = mock()
        whenever(payloadService.setKeyForLegacyAddress(eq(mockECKey), isNull()))
            .thenReturn(Observable.just(mockLegacyAddress))
        // Act
        val observer = subject.setKeyForLegacyAddress(mockECKey, null).test()
        // Assert
        verify(payloadService).setKeyForLegacyAddress(eq(mockECKey), isNull())
        observer.assertNoErrors()
        observer.assertComplete()
        assertEquals(mockLegacyAddress, observer.values()[0])
    }

    @Test
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
    fun `getMetadataNodeFactory returns node`() {
        // Arrange
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(payloadManager.metadataNodeFactory).thenReturn(mockNodeFactory)
        // Act
        val testObserver = subject.getMetadataNodeFactory().test()
        // Assert
        verify(payloadManager, atLeastOnce()).metadataNodeFactory
        verifyNoMoreInteractions(payloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockNodeFactory)
    }

    @Test
    fun `getMetadataNodeFactory null, attempt node setup`() {
        // Arrange
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(payloadManager.metadataNodeFactory)
            .thenReturn(null)
            .thenReturn(mockNodeFactory)
        whenever(payloadService.loadNodes()).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.getMetadataNodeFactory().test()
        // Assert
        verify(payloadManager, atLeastOnce()).metadataNodeFactory
        verifyNoMoreInteractions(payloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockNodeFactory)
    }

    @Test
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
    fun isDoubleEncrypted() {
        // Arrange
        whenever(payloadManager.payload.isDoubleEncryption).thenReturn(true)
        // Act
        val result = subject.isDoubleEncrypted
        // Assert
        result shouldEqual true
    }

    @Test
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