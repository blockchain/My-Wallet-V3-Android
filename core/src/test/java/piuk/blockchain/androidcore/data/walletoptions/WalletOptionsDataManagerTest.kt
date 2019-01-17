package piuk.blockchain.androidcore.data.walletoptions

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import kotlin.test.assertEquals

class WalletOptionsDataManagerTest {

    private lateinit var subject: WalletOptionsDataManager

    private val authService: AuthService = mock()
    private var walletOptionsState = WalletOptionsState()
    private val mockSettingsDataManager: SettingsDataManager =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val explorerUrl: String = "https://blockchain.info/"

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Before
    fun setUp() {
        walletOptionsState.wipe()
        subject = WalletOptionsDataManager(
            authService,
            walletOptionsState,
            mockSettingsDataManager,
            explorerUrl
        )
    }

    @Test
    fun `checkForceUpgrade missing androidUpgrade JSON object`() {
        // Arrange
        val walletOptions: WalletOptions = mock()
        val versionCode = 360
        val sdk = 16
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        // Act
        val testObserver = subject.checkForceUpgrade(versionCode, sdk).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `checkForceUpgrade empty androidUpgrade JSON object`() {
        // Arrange
        val walletOptions: WalletOptions = mock()
        whenever(walletOptions.androidUpgrade).thenReturn(emptyMap())
        val versionCode = 360
        val sdk = 16
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        // Act
        val testObserver = subject.checkForceUpgrade(versionCode, sdk).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `checkForceUpgrade ignore minSdk despite versionCode unsupported`() {
        // Arrange
        val walletOptions: WalletOptions = mock()
        whenever(walletOptions.androidUpgrade).thenReturn(
            mapOf(
                "minSdk" to 18,
                "minVersionCode" to 361
            )
        )
        val versionCode = 360
        val sdk = 16
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        // Act
        val testObserver = subject.checkForceUpgrade(versionCode, sdk).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `checkForceUpgrade versionCode supported, minSdk lower than supplied`() {
        // Arrange
        val walletOptions: WalletOptions = mock()
        whenever(walletOptions.androidUpgrade).thenReturn(
            mapOf(
                "minSdk" to 18,
                "minVersionCode" to 360
            )
        )
        val versionCode = 360
        val sdk = 21
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        // Act
        val testObserver = subject.checkForceUpgrade(versionCode, sdk).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `checkForceUpgrade should force upgrade`() {
        // Arrange
        val walletOptions: WalletOptions = mock()
        whenever(walletOptions.androidUpgrade).thenReturn(
            mapOf(
                "minSdk" to 16,
                "minVersionCode" to 361
            )
        )
        val versionCode = 360
        val sdk = 16
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        // Act
        val testObserver = subject.checkForceUpgrade(versionCode, sdk).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `getBuyWebviewWalletLink wallet-options set`() {
        // Arrange
        val walletOptionsRoot = "https://blockchain.com/wallet"
        val mockOptions: WalletOptions = mock()
        whenever(mockOptions.buyWebviewWalletLink).thenReturn(walletOptionsRoot)
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(mockOptions))
        // Act
        val result = subject.getBuyWebviewWalletLink()
        // Assert
        assertEquals("https://blockchain.com/wallet/#/intermediate", result)
    }

    @Test
    fun `getBuyWebviewWalletLink wallet-options unset`() {
        // Arrange
        val walletOptionsRoot = null
        val mockOptions: WalletOptions = mock()
        whenever(mockOptions.buyWebviewWalletLink).thenReturn(walletOptionsRoot)
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(mockOptions))
        // Act
        val result = subject.getBuyWebviewWalletLink()
        // Assert
        assertEquals("https://blockchain.info/wallet/#/intermediate", result)
    }

    @Test
    fun `get BCH fee`() {
        // Arrange
        val mockOptions: WalletOptions = mock()
        whenever(mockOptions.bchFeePerByte).thenReturn(5)
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(mockOptions))
        // Act
        subject.getBchFee()
            .test()
            .assertValue(5)
    }
}
