package piuk.blockchain.android.data.bitcoincash

import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.coin.GenericMetadataWallet
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.HDWallet
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import junit.framework.Assert
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class BchDataManagerTest : RxTest() {

    private lateinit var subject: BchDataManager

    private val payloadDataManager: PayloadDataManager = mock()
    private var bchDataStore: BchDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val environmentSettings: EnvironmentConfig = mock()
    private val blockExplorer: BlockExplorer = mock()
    private val stringUtils: StringUtils = mock()
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
            stringUtils,
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
    @Throws(Exception::class)
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearBchAccountDetails()
        // Assert
        verify(bchDataStore).clearData()
        verifyNoMoreInteractions(bchDataStore)
    }

    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun `initBchWallet retrieve existing data payload wo second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        mockSingleMetadata()
        mockRestoringSingleBchWallet("xpub")
        whenever(stringUtils.getString(any())).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun `initBchWallet retrieve existing data payload with second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        mockSingleMetadata()
        mockRestoringSingleBchWallet("xpub")
        whenever(stringUtils.getString(any())).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun `restoreBchWallet with 2nd pw 1 account`() {

        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val xpub = "xpub"
        val metaData = mockRestoringSingleBchWallet(xpub)

        // Act
        subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub)
        verify(metaData.accounts.get(0)).setXpub(xpub)
    }

    @Test
    @Throws(Exception::class)
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
        verify(metaData.accounts.get(0)).setXpub(xpub1)
        verify(metaData.accounts.get(1)).setXpub(xpub2)
    }

    @Test
    @Throws(Exception::class)
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
        verify(metaData.accounts.get(0)).setXpub(xpub)
    }

    @Test
    @Throws(Exception::class)
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
        verify(metaData.accounts.get(0)).setXpub(xpub1)
        verify(metaData.accounts.get(1)).setXpub(xpub2)
    }

    @Test
    @Throws(Exception::class)
    fun `correctBtcOffsetIfNeed btc equal to bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertFalse(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
    fun `correctBtcOffsetIfNeed btc more than bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount, btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertFalse(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
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

        // Act
        val needsSync = subject.correctBtcOffsetIfNeed("label")

        // Assert
        assertTrue(needsSync)
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verify(payloadDataManager, times(btcAccountsNeeded)).wallet
        verify(mockHdWallet, times(btcAccountsNeeded)).addAccount("label 2")
        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount))!!.accounts

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    @Throws(Exception::class)
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
        val needsSync = subject.correctBtcOffsetIfNeed("label")

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

    private fun split(words: String): List<String> {
        return words.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
    }
}