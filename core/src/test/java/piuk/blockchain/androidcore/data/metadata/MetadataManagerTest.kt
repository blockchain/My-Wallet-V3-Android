package piuk.blockchain.androidcore.data.metadata

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.MetadataNodeFactory
import io.reactivex.Completable
import io.reactivex.Observable
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.blockchain.android.testutils.rxInit
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.MetadataUtils

class MetadataManagerTest {

    private lateinit var subject: MetadataManager
    private val payloadDataManager: PayloadDataManager = mock()
    private val metadataUtils: MetadataUtils = mock()
    private val rxBus: RxBus = RxBus()
    private val networkParameters = BitcoinMainNetParams.get()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = MetadataManager(
            payloadDataManager,
            metadataUtils,
            rxBus
        )
    }

    @Test
    fun `attemptMetadataSetup load success`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        val metadataNodeFactory: MetadataNodeFactory = mock()

        val key: DeterministicKey = mock()
        whenever(payloadDataManager.getMetadataNodeFactory())
            .thenReturn(Observable.just(metadataNodeFactory))
        whenever(metadataNodeFactory.metadataNode).thenReturn(key)

        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).loadNodes()
        verify(payloadDataManager).getMetadataNodeFactory()
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `attemptMetadataSetup load fails wo 2nd pw`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)

        val key: DeterministicKey = mock()
        val metadataNodeFactory: MetadataNodeFactory = mock()
        whenever(metadataNodeFactory.metadataNode).thenReturn(key)

        whenever(payloadDataManager.generateAndReturnNodes())
            .thenReturn(Observable.just(metadataNodeFactory))

        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).loadNodes()
        verify(payloadDataManager).generateAndReturnNodes()
        verify(payloadDataManager).isDoubleEncrypted
    }

    @Test
    fun `attemptMetadataSetup load fails with 2nd pw`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        // Act
        val testObserver = subject.attemptMetadataSetup().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(InvalidCredentialsException::class.java)
    }

    @Test
    fun `generateAndSetupMetadata load success`() {
        // Arrange
        whenever(payloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        val metadataNodeFactory: MetadataNodeFactory = mock()
        val key: DeterministicKey = mock()
        whenever(payloadDataManager.generateNodes()).thenReturn(Completable.complete())
        whenever(payloadDataManager.getMetadataNodeFactory())
            .thenReturn(Observable.just(metadataNodeFactory))
        whenever(metadataNodeFactory.metadataNode).thenReturn(key)

        // Act
        val testObserver = subject.decryptAndSetupMetadata(networkParameters, "hello").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).decryptHDWallet(networkParameters, "hello")
        verify(payloadDataManager).generateNodes()
        verify(payloadDataManager).loadNodes()
        verify(payloadDataManager).getMetadataNodeFactory()
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun saveToMetadata() {
        // Arrange
        val type = 1337
        val data = "DATA"
        val factory: MetadataNodeFactory = mock()
        val node: DeterministicKey = mock()
        val metadata: Metadata = mock()
        whenever(payloadDataManager.getMetadataNodeFactory()).thenReturn(Observable.just(factory))
        whenever(factory.metadataNode).thenReturn(node)
        whenever(metadataUtils.getMetadataNode(node, type)).thenReturn(metadata)
        // Act
        val testObserver = subject.saveToMetadata(data, type).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(payloadDataManager).getMetadataNodeFactory()
        verify(factory).metadataNode
        verify(metadataUtils).getMetadataNode(node, type)
        verify(metadata).putMetadata(data)
    }
}