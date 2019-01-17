package piuk.blockchain.androidcore.data.bitcoincash

import com.blockchain.android.testutils.rxInit
import com.blockchain.wallet.DefaultLabels
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.api.data.Balance
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.coin.GenericMetadataWallet
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.HDWallet
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import junit.framework.Assert
import org.amshove.kluent.`it returns`
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.math.BigInteger
import java.util.LinkedHashMap
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BchDataManagerTest {

    @get:Rule
    val rxSchedulers = rxInit {
        ioTrampoline()
    }

    private lateinit var subject: BchDataManager

    private val payloadDataManager: PayloadDataManager = mock()
    private var bchDataStore: BchDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val environmentSettings: EnvironmentConfig = mock()
    private val blockExplorer: BlockExplorer = mock()
    private val defaultLabels: DefaultLabels = mock()
    private val metadataManager: MetadataManager = mock()
    private val rxBus = RxBus()

    @Before
    fun setUp() {
        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(
            BitcoinCashMainNetParams.get()
        )

        subject = BchDataManager(
            payloadDataManager,
            bchDataStore,
            environmentSettings,
            blockExplorer,
            defaultLabels,
            metadataManager,
            rxBus
        )
    }

    private fun mockAbsentMetadata() {
        whenever(metadataManager.fetchMetadata(any())).thenReturn(Observable.just(Optional.absent()))
    }

    private fun mockSingleMetadata(): String {
        val metaData = GenericMetadataWallet()
        val account = GenericMetadataAccount()
        account.label = "account label"
        metaData.addAccount(account)

        whenever(metadataManager.fetchMetadata(any())).thenReturn(
            Observable.just(
                Optional.fromNullable(
                    metaData.toJson()
                )
            )
        )

        return metaData.toJson()
    }

    private fun mockRestoringSingleBchWallet(xpub: String): GenericMetadataWallet {

        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(mutableListOf(btcAccount))
        whenever(btcAccount.xpub).thenReturn(xpub)

        val metaData: GenericMetadataWallet = mock()
        val bchMetaDataAccount: GenericMetadataAccount = mock()
        whenever(metaData.accounts).thenReturn(mutableListOf(bchMetaDataAccount))

        return metaData
    }

    @Test
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearBchAccountDetails()
        // Assert
        verify(bchDataStore).clearData()
        verifyNoMoreInteractions(bchDataStore)
    }

    @Test
    fun `initBchWallet create new metadata payload wo second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        mockAbsentMetadata()
        mockRestoringSingleBchWallet("xpub")

        whenever(bchDataStore.bchMetadata!!.toJson()).thenReturn("{}")
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `initBchWallet retrieve existing data payload wo second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        mockSingleMetadata()
        mockRestoringSingleBchWallet("xpub")
        whenever(defaultLabels[CryptoCurrency.BCH]).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `initBchWallet create new metadata payload with second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)

        // Arrange
        mockAbsentMetadata()
        mockRestoringSingleBchWallet("xpub")

        whenever(bchDataStore.bchMetadata!!.toJson()).thenReturn("{}")
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `initBchWallet retrieve existing data payload with second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        mockSingleMetadata()
        mockRestoringSingleBchWallet("xpub")
        whenever(defaultLabels[CryptoCurrency.BCH]).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `fetchMetadata doesn't exist`() {

        // Arrange
        mockAbsentMetadata()

        // Act
        val testObserver = subject.fetchMetadata("label", 1).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(Optional.absent())
    }

    @Test
    fun `fetchMetadata exists`() {

        // Arrange
        val walletJson = mockSingleMetadata()

        // Act
        val testObserver = subject.fetchMetadata("label", 1).test()

        // Assert
        testObserver.assertComplete()
        Assert.assertEquals(walletJson, testObserver.values()[0].orNull()!!.toJson())
    }

    @Test
    fun `restoreBchWallet with 2nd pw 1 account`() {

        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val xpub = "xpub"
        val metaData = mockRestoringSingleBchWallet(xpub)

        // Act
        subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub)
        verify(metaData.accounts[0]).xpub = xpub
    }

    @Test
    fun `restoreBchWallet with 2nd pw 2 account`() {

        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val xpub1 = "xpub1"
        val xpub2 = "xpub2"
        val btcAccount1: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccount2: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(mutableListOf(btcAccount1, btcAccount2))
        whenever(btcAccount1.xpub).thenReturn(xpub1)
        whenever(btcAccount2.xpub).thenReturn(xpub2)

        val metaData: GenericMetadataWallet = mock()
        val bchMetaDataAccount1: GenericMetadataAccount = mock()
        val bchMetaDataAccount2: GenericMetadataAccount = mock()
        whenever(metaData.accounts).thenReturn(
            mutableListOf(
                bchMetaDataAccount1,
                bchMetaDataAccount2
            )
        )

        // Act
        subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub1)
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub2)
        verify(metaData.accounts[0]).xpub = xpub1
        verify(metaData.accounts[1]).xpub = xpub2
    }

    @Test
    fun `restoreBchWallet no 2nd pw 1 account`() {

        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val xpub = "xpub"
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(mutableListOf(btcAccount))
        whenever(btcAccount.xpub).thenReturn(xpub)

        val metaData: GenericMetadataWallet = mock()
        val bchMetaDataAccount: GenericMetadataAccount = mock()
        whenever(metaData.accounts).thenReturn(mutableListOf(bchMetaDataAccount))

        // Act
        subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addAccount()
        verify(metaData.accounts[0]).xpub = xpub
    }

    @Test
    fun `restoreBchWallet no 2nd pw 2 account`() {

        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val xpub1 = "xpub1"
        val xpub2 = "xpub2"
        val btcAccount1: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccount2: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(mutableListOf(btcAccount1, btcAccount2))
        whenever(btcAccount1.xpub).thenReturn(xpub1)
        whenever(btcAccount2.xpub).thenReturn(xpub2)

        val metaData: GenericMetadataWallet = mock()
        val bchMetaDataAccount1: GenericMetadataAccount = mock()
        val bchMetaDataAccount2: GenericMetadataAccount = mock()
        whenever(metaData.accounts).thenReturn(
            mutableListOf(
                bchMetaDataAccount1,
                bchMetaDataAccount2
            )
        )

        // Act
        subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet, times(2))!!.addAccount()
        verify(metaData.accounts[0]).xpub = xpub1
        verify(metaData.accounts[1]).xpub = xpub2
    }

    @Test
    fun `correctBtcOffsetIfNeed btc equal to bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed()

        // Assert
        assertFalse(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `correctBtcOffsetIfNeed btc more than bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount, btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed()

        // Assert
        assertFalse(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `correctBtcOffsetIfNeed btc 1 less than bch account size`() {
        // Arrange
        val btcAccountsNeeded = 1
        val mockCallCount = 1

        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount, bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        val mockWallet: Wallet = mock()
        val mockHdWallet: HDWallet = mock()
        whenever(btcAccount.xpub).thenReturn("xpub 2")
        whenever(mockHdWallet.addAccount(any())).thenReturn(btcAccount)
        whenever(mockWallet.hdWallets).thenReturn(mutableListOf(mockHdWallet))
        whenever(payloadDataManager.wallet).thenReturn(mockWallet)

        whenever(defaultLabels[CryptoCurrency.BTC]).thenReturn("BTC label")

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed()

        // Assert
        assertTrue(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verify(payloadDataManager, times(btcAccountsNeeded)).wallet
        verify(mockHdWallet, times(btcAccountsNeeded)).addAccount("BTC label 2")
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `correctBtcOffsetIfNeed btc 5 less than bch account size`() {
        // Arrange
        val btcAccountsNeeded = 5
        val mockCallCount = 1

        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts =
            mutableListOf(bchAccount, bchAccount, bchAccount, bchAccount, bchAccount, bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        val mockWallet: Wallet = mock()
        val mockHdWallet: HDWallet = mock()
        whenever(btcAccount.xpub).thenReturn("xpub 2")
        whenever(mockHdWallet.addAccount(any())).thenReturn(btcAccount)

        whenever(mockWallet.hdWallets).thenReturn(mutableListOf(mockHdWallet))
        whenever(payloadDataManager.wallet).thenReturn(mockWallet)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed()

        // Assert
        assertTrue(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verify(payloadDataManager, times(btcAccountsNeeded)).wallet
        verify(mockHdWallet, times(btcAccountsNeeded)).addAccount(any())
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `get balance`() {
        val address = "address"
        val map = LinkedHashMap<String, Balance>().apply {
            put(address, Balance().apply { finalBalance = BigInteger.TEN })
        }

        BchDataManager(
            mock {
                on { getBalanceOfBchAddresses(listOf(address)) } `it returns` Observable.just(map)
            },
            mock(),
            mock(),
            mock(),
            mock(),
            mock(),
            mock()
        ).getBalance(address)
            .test()
            .assertNoErrors()
            .assertValue(BigInteger.TEN)
    }

    @Test
    fun `get balance returns zero on error`() {
        val address = "address"

        BchDataManager(
            mock {
                on { getBalanceOfBchAddresses(listOf(address)) } `it returns` Observable.error(Exception())
            },
            mock(),
            mock(),
            mock(),
            mock(),
            mock(),
            mock()
        ).getBalance(address)
            .test()
            .assertNoErrors()
            .assertValue(BigInteger.ZERO)
    }

    private fun split(words: String): List<String> {
        return words.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
    }
}