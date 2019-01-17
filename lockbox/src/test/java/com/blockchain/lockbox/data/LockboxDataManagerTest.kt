package com.blockchain.lockbox.data

import com.blockchain.android.testutils.rxInit
import com.blockchain.lockbox.data.models.LockboxMetadata
import com.blockchain.metadata.MetadataRepository
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.serialization.fromMoshiJson
import com.blockchain.testutils.getStringFromResource
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Single
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LockboxDataManagerTest {

    private lateinit var subject: LockboxDataManager
    private val metadataManager: MetadataRepository = mock()
    private val remoteConfiguration: FeatureFlag = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = LockboxDataManager(metadataManager, remoteConfiguration)
    }

    @Test
    fun `should not be available`() {
        givenNotAvailable()
        subject.isLockboxAvailable()
            .test()
            .assertValue(false)
    }

    @Test
    fun `should be available`() {
        givenAvailable()
        subject.isLockboxAvailable()
            .test()
            .assertValue(true)
    }

    @Test
    fun `should return no lockbox on error`() {
        // Arrange
        givenAvailable()
        whenever(metadataManager.loadMetadata(LockboxMetadata.MetaDataType, LockboxMetadata::class.java))
            .thenReturn(Maybe.error { Throwable() })
        // Act
        val testObserver = subject.hasLockbox().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(false)
    }

    @Test
    fun `should return no lockbox as no data present`() {
        // Arrange
        givenAvailable()
        givenNoLockboxMetaData()
        // Act
        val testObserver = subject.hasLockbox().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(false)
    }

    @Test
    fun `should return no lockbox as devices empty`() {
        // Arrange
        givenAvailable()
        whenever(metadataManager.loadMetadata(LockboxMetadata.MetaDataType, LockboxMetadata::class.java))
            .thenReturn(Maybe.just(LockboxMetadata(devices = emptyList())))
        // Act
        val testObserver = subject.hasLockbox().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(false)
    }

    @Test
    fun `should return has lockbox`() {
        // Arrange
        givenAvailable()
        givenLockboxMetadata("lockbox/LockboxCompleteMetadata.json")
        // Act
        val testObserver = subject.hasLockbox().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(true)
    }

    @Test
    fun `can list all accounts on a single device`() {
        givenAvailable()
        givenLockboxMetadata("lockbox/LockboxCompleteMetadata.json")
        subject.accounts()
            .test()
            .assertComplete()
            .values()
            .single() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Ledger - BTC Wallet", "xpubBTC"),
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "Ledger - BCH Wallet", "xpubBCH"),
                AccountReference.Ethereum("Ledger - ETH Wallet", "addrETH"),
                AccountReference.Xlm("Ledger - XLM Wallet", "publicKeyXLM")
            )
    }

    @Test
    fun `can list all accounts on multiple devices`() {
        givenAvailable()
        givenLockboxMetadata("lockbox/LockboxMultideviceMetadata.json")
        subject.accounts()
            .test()
            .assertComplete()
            .values()
            .single() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Ledger - BTC Wallet", "xpubBTC"),
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "Ledger - BCH Wallet", "xpubBCH"),
                AccountReference.Ethereum("Ledger - ETH Wallet", "addrETH"),
                AccountReference.Xlm("Ledger - XLM Wallet", "publicKeyXLM"),
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Ledger2 - BTC Wallet 1", "xpubBTC21"),
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Ledger2 - BTC Wallet 2", "xpubBTC22"),
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "Ledger2 - BCH Wallet 1", "xpubBCH21"),
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "Ledger2 - BCH Wallet 2", "xpubBCH22"),
                AccountReference.Ethereum("Ledger2 - ETH Wallet 1", "addrETH21"),
                AccountReference.Ethereum("Ledger2 - ETH Wallet 2", "addrETH22"),
                AccountReference.Xlm("Ledger2 - XLM Wallet 1", "publicKeyXLM21"),
                AccountReference.Xlm("Ledger2 - XLM Wallet 2", "publicKeyXLM22")
            )
    }

    @Test
    fun `can list zero accounts`() {
        givenAvailable()
        givenLockboxMetadata("lockbox/LockboxNoAccountsMetadata.json")
        subject.accounts()
            .test()
            .assertComplete()
            .values()
            .single() `should equal` emptyList()
    }

    @Test
    fun `can list zero accounts when no lockbox`() {
        givenAvailable()
        givenNoLockboxMetaData()
        subject.accounts()
            .test()
            .assertComplete()
            .values()
            .single() `should equal` emptyList()
    }

    private fun givenNoLockboxMetaData() {
        whenever(metadataManager.loadMetadata(LockboxMetadata.MetaDataType, LockboxMetadata::class.java))
            .thenReturn(Maybe.empty())
    }

    private fun givenLockboxMetadata(lockboxMetaDataJsonResourceName: String) {
        whenever(metadataManager.loadMetadata(LockboxMetadata.MetaDataType, LockboxMetadata::class.java))
            .thenReturn(
                Maybe.just(
                    LockboxMetadata::class.fromMoshiJson(getStringFromResource(lockboxMetaDataJsonResourceName))
                )
            )
    }

    private fun givenAvailable() {
        whenever(remoteConfiguration.enabled).thenReturn(Single.just(true))
    }

    private fun givenNotAvailable() {
        whenever(remoteConfiguration.enabled).thenReturn(Single.just(false))
    }
}
