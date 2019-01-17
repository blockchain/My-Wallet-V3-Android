package piuk.blockchain.androidcore.data.ethereum

import com.blockchain.android.testutils.rxInit
import com.blockchain.logging.LastTxUpdater
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import info.blockchain.wallet.ethereum.data.EthLatestBlock
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.bitcoinj.core.ECKey
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import java.math.BigInteger

class EthDataManagerTest {

    private lateinit var subject: EthDataManager
    private val payloadManager: PayloadManager = mock()
    private val ethAccountApi: EthAccountApi = mock()
    private val ethDataStore: EthDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val lastTxUpdater: LastTxUpdater = mock()
    private val rxBus = RxBus()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = EthDataManager(
            payloadManager,
            ethAccountApi,
            ethDataStore,
            walletOptionsDataManager,
            metadataManager,
            environmentSettings,
            lastTxUpdater,
            rxBus
        )
    }

    @Test
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearEthAccountDetails()
        // Assert
        verify(ethDataStore).clearData()
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun fetchEthAddress() {
        // Arrange
        val ethAddress = "ADDRESS"
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)
        val ethAddressResponseMap: EthAddressResponseMap = mock()
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.just(ethAddressResponseMap))
        // Act
        val testObserver = subject.fetchEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verify(ethDataStore).ethAddressResponse = any(CombinedEthModel::class)
        verifyZeroInteractions(ethDataStore)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun fetchEthAddressTestnet() {
        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.TESTNET)
        // Act
        val testObserver = subject.fetchEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore).ethAddressResponse = null
        verifyZeroInteractions(ethDataStore)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `get balance found`() {
        // Arrange
        val ethAddress = "ADDRESS"
        val ethAddressResponseMap: EthAddressResponseMap = mock(defaultAnswer = RETURNS_DEEP_STUBS)
        val response: EthAddressResponse = mock()
        whenever(response.balance).thenReturn(BigInteger.TEN)
        whenever(ethAddressResponseMap.ethAddressResponseMap.values).thenReturn(mutableListOf(response))
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.just(ethAddressResponseMap))
        // Act
        val testObserver = subject.getBalance(ethAddress).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(BigInteger.TEN)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `get balance error, still returns value`() {
        // Arrange
        val ethAddress = "ADDRESS"
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.error(Exception()))
        // Act
        val testObserver = subject.getBalance(ethAddress).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(BigInteger.ZERO)
        verify(ethAccountApi).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getEthResponseModel() {
        // Arrange

        // Act
        subject.getEthResponseModel()
        // Assert
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun getEthWallet() {
        // Arrange

        // Act
        subject.getEthWallet()
        // Assert
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun `getEthTransactions response found with 3 transactions`() {
        // Arrange
        val combinedEthModel: CombinedEthModel = mock()
        val ethTransaction: EthTransaction = mock()
        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getTransactions())
            .thenReturn(listOf(ethTransaction, ethTransaction, ethTransaction))
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val values = testObserver.values()
        values `should contain` ethTransaction
        values.size `should equal to` 3
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun `getEthTransactions response not found`() {
        // Arrange
        whenever(ethDataStore.ethAddressResponse).thenReturn(null)
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun `isLastTxPending response not found`() {
        // Arrange
        val ethHash = "HASH"
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethDataStore.ethWallet!!.lastTransactionHash).thenReturn(ethHash)
        whenever(walletOptionsDataManager.getLastEthTransactionFuse()).thenReturn(
            Observable.just(
                600
            )
        )
        whenever(ethDataStore.ethWallet!!.lastTransactionTimestamp).thenReturn(0L)

        val ethAddress = "ADDRESS"
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(null)

        val ethAddressResponse: EthAddressResponse = mock()
        val ethAddressResponseMap: EthAddressResponseMap = mock()
        whenever(ethAddressResponseMap.ethAddressResponseMap)
            .thenReturn(mutableMapOf(Pair("", ethAddressResponse)))

        val ethTransaction: EthTransaction = mock()
        whenever(ethTransaction.hash).thenReturn(ethHash)
        whenever(ethAddressResponse.transactions)
            .thenReturn(listOf(ethTransaction, ethTransaction, ethTransaction))
        val combinedEthModel: CombinedEthModel = mock()
        whenever(combinedEthModel.getTransactions()).thenReturn(
            listOf(
                ethTransaction,
                ethTransaction,
                ethTransaction
            )
        )
        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)

        // Act
        val testObserver = subject.isLastTxPending().test()
        // Assert
        testObserver.assertNotComplete()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verify(ethDataStore, atLeastOnce()).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun `isLastTxPending last tx unprocessed, just submitted`() {

        val isProcessed = false
        val sent = System.currentTimeMillis()
        val shouldBePending = true

        isLastTxPending(isProcessed, sent, shouldBePending)
    }

    @Test
    fun `isLastTxPending last tx processed`() {

        val isProcessed = true
        val sent = System.currentTimeMillis() // irrelevant
        val shouldBePending = false

        isLastTxPending(isProcessed, sent, shouldBePending)
    }

    @Test
    fun `isLastTxPending last tx unprocessed, legacy processed`() {
        // legacy txs won't have last_tx_timestamp in metadata
        val isProcessed = false
        val sent = 0L // irrelevant
        val shouldBePending = false
        isLastTxPending(isProcessed, sent, shouldBePending)
    }

    @Test
    fun `isLastTxPending last tx unprocessed, legacy dropped`() {
        // legacy txs won't have last_tx_timestamp in metadata
        val isProcessed = false
        val sent = 0L
        val shouldBePending = false
        isLastTxPending(isProcessed, sent, shouldBePending)
    }

    @Test
    fun `isLastTxPending last tx unprocessed, 1min before being dropped`() {

        // 23h59m - 1 min before drop time
        val isProcessed = false
        val sent = System.currentTimeMillis() - (86340L * 1000)
        val shouldBePending = true

        isLastTxPending(isProcessed, sent, shouldBePending)
    }

    @Ignore // This is incredibly flakey
    @Test
    fun `isLastTxPending last tx unprocessed, just dropped`() {

        // 24h - on drop time
        val isProcessed = false
        val sent = System.currentTimeMillis() - (86400L * 1000)
        val shouldBePending = false

        isLastTxPending(isProcessed, sent, shouldBePending)
    }

    @Test
    fun `isLastTxPending last tx unprocessed, 1min after being dropped`() {

        // 24h01m - 1 min passed drop time
        val txProcessed = true
        val sent = System.currentTimeMillis() - (86460L * 1000)
        val shouldBePending = false

        isLastTxPending(txProcessed, sent, shouldBePending)
    }

    private fun isLastTxPending(
        isProcessed: Boolean,
        timeLastTxSent: Long,
        hasPendingTransaction: Boolean
    ) {

        // Arrange
        val lastTxHash = "hash1"
        var existingHash = "hash2"

        if (isProcessed) {
            // Server flagged last tx hash as processed
            existingHash = lastTxHash
        }
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethDataStore.ethWallet!!.lastTransactionHash).thenReturn(lastTxHash)
        whenever(walletOptionsDataManager.getLastEthTransactionFuse()).thenReturn(
            Observable.just(
                86400L
            )
        )
        whenever(ethDataStore.ethWallet!!.lastTransactionTimestamp).thenReturn(timeLastTxSent)

        val ethAddress = "ADDRESS"
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)
        val ethAddressResponseMap: EthAddressResponseMap = mock()
        whenever(ethAccountApi.getEthAddress(listOf(ethAddress)))
            .thenReturn(Observable.just(ethAddressResponseMap))

        val ethAddressResponse: EthAddressResponse = mock()
        whenever(ethAddressResponseMap.ethAddressResponseMap)
            .thenReturn(mutableMapOf(Pair("", ethAddressResponse)))

        val ethTransaction: EthTransaction = mock()
        whenever(ethTransaction.hash).thenReturn(existingHash)
        whenever(ethAddressResponse.transactions)
            .thenReturn(listOf(ethTransaction, ethTransaction, ethTransaction))

        val combinedEthModel: CombinedEthModel = mock()
        whenever(combinedEthModel.getTransactions()).thenReturn(
            listOf(
                ethTransaction,
                ethTransaction,
                ethTransaction
            )
        )
        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)

        // Act
        val testObserver = subject.isLastTxPending().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hasPendingTransaction)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verify(ethAccountApi, atLeastOnce()).getEthAddress(listOf(ethAddress))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getLatestBlock() {
        // Arrange
        val latestBlock: EthLatestBlock = mock()
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethAccountApi.latestBlock).thenReturn(Observable.just(latestBlock))
        // Act
        val testObserver = subject.getLatestBlock().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(latestBlock)
        verify(ethAccountApi).latestBlock
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getLatestBlockTestnet() {
        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.TESTNET)
        // Act
        val testObserver = subject.getLatestBlock().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getIfContract() {
        // Arrange
        val address = "ADDRESS"
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethAccountApi.getIfContract(address)).thenReturn(Observable.just(true))
        // Act
        val testObserver = subject.getIfContract(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
        verify(ethAccountApi).getIfContract(address)
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun getIfContractTestnet() {
        // Arrange
        val address = "ADDRESS"
        whenever(environmentSettings.environment).thenReturn(Environment.TESTNET)
        // Act
        val testObserver = subject.getIfContract(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `getTransactionNotes returns string object`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet!!.txNotes[hash]).thenReturn(notes)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should equal` notes
    }

    @Test
    fun `getTransactionNotes returns null object as wallet is missing`() {
        // Arrange
        val hash = "HASH"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should equal` null
    }

    @Test
    fun `updateTransactionNotes success`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        val ethereumWallet: EthereumWallet = mock()
        whenever(ethDataStore.ethWallet).thenReturn(ethereumWallet)
        whenever(ethDataStore.ethWallet!!.toJson()).thenReturn("{}")
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(metadataManager).saveToMetadata(any(), any())
        verifyNoMoreInteractions(metadataManager)
    }

    @Test
    fun `updateTransactionNotes wallet not found`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(IllegalStateException::class.java)
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    // TODO: This is not at all testable
    @Test
    fun initEthereumWallet() {
        // Arrange

        // Act

        // Assert
    }

    // TODO: This isn't testable either, wrap [RawTransaction] class in interface
    @Test
    fun createEthTransaction() {
        // Arrange

        // Act

        // Assert
    }

    @Test
    fun signEthTransaction() {
        // Arrange
        val rawTransaction: RawTransaction = mock()
        val ecKey: ECKey = mock()
        val byteArray = ByteArray(32)
        whenever(ethDataStore.ethWallet!!.account!!.signTransaction(rawTransaction, ecKey))
            .thenReturn(byteArray)
        // Act
        val testObserver = subject.signEthTransaction(rawTransaction, ecKey).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(byteArray)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun pushEthTx() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethAccountApi.pushTx(any(String::class))).thenReturn(Observable.just(hash))
        whenever(lastTxUpdater.updateLastTxTime()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.pushEthTx(byteArray).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethAccountApi).pushTx(any(String::class))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun `pushEthTx returns hash despite update last tx failing`() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethAccountApi.pushTx(any(String::class))).thenReturn(Observable.just(hash))
        whenever(lastTxUpdater.updateLastTxTime()).thenReturn(Completable.error(Exception()))
        // Act
        val testObserver = subject.pushEthTx(byteArray).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethAccountApi).pushTx(any(String::class))
        verifyNoMoreInteractions(ethAccountApi)
    }

    @Test
    fun setLastTxHashObservable() {
        // Arrange
        val hash = "HASH"
        val timestamp = System.currentTimeMillis()
        val ethereumWallet: EthereumWallet = mock()
        whenever(ethDataStore.ethWallet).thenReturn(ethereumWallet)
        whenever(ethDataStore.ethWallet!!.toJson()).thenReturn("{}")
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.setLastTxHashObservable(hash, timestamp).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(hash)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(ethereumWallet).lastTransactionHash = hash
        verify(ethereumWallet).lastTransactionTimestamp = timestamp
        verify(ethDataStore.ethWallet)!!.toJson()
        verifyNoMoreInteractions(ethereumWallet)
        verify(metadataManager).saveToMetadata(any(), any())
        verifyNoMoreInteractions(metadataManager)
    }
}