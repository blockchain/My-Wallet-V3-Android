package piuk.blockchain.androidbuysell.datamanagers

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.ReplaySubject
import org.junit.Before
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.androidbuysell.RxTest
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.services.BuyConditions
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import kotlin.test.Test

class BuyDataManagerTest : RxTest() {

    private lateinit var subject: BuyDataManager
    private val mockSettingsDataManager: SettingsDataManager =
        mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockAuthDataManager: AuthDataManager = mock()
    private val mockPayloadDataManager: PayloadDataManager =
        mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockExchangeService: ExchangeService = mock()

    private val mockWalletOptions: WalletOptions = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockSettings: Settings = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val mockExchangeData: ExchangeData = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val buyConditions: BuyConditions = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        val walletOptionsSource = mockWalletOptionsReplay()
        val exchangeDataSource = mockExchangeDataReplay()
        val walletSettingsSource = mockWalletSettingsReplay()

        whenever(buyConditions.walletOptionsSource).thenReturn(walletOptionsSource)
        whenever(buyConditions.walletSettingsSource).thenReturn(walletSettingsSource)
        whenever(buyConditions.exchangeDataSource).thenReturn(exchangeDataSource)

        subject = BuyDataManager(
            mockSettingsDataManager,
            mockAuthDataManager,
            mockPayloadDataManager,
            buyConditions,
            mockExchangeService
        )
    }

    private fun mockWalletOptionsReplay(): ReplaySubject<WalletOptions> {
        val source = ReplaySubject.create<WalletOptions>()
        val o1: Observer<WalletOptions> = mock()
        source.subscribe(o1)

        source.onNext(mockWalletOptions)
        source.onComplete()

        whenever(mockAuthDataManager.getWalletOptions()).thenReturn(
            Observable.just(
                mockWalletOptions
            )
        )

        return source
    }

    private fun mockWalletSettingsReplay(): ReplaySubject<Settings> {
        val source = ReplaySubject.create<Settings>()
        val o1: Observer<Settings> = mock()
        source.subscribe(o1)

        source.onNext(mockSettings)
        source.onComplete()

        whenever(mockSettingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        return source
    }

    private fun mockExchangeDataReplay(): ReplaySubject<ExchangeData> {
        val source = ReplaySubject.create<ExchangeData>()
        val o1: Observer<ExchangeData> = mock()
        source.subscribe(o1)

        source.onNext(mockExchangeData)
        source.onComplete()

        whenever(mockExchangeService.getExchangeMetaData()).thenReturn(
            Observable.just(
                mockExchangeData
            )
        )

        return source
    }

    @Test
    fun isBuyRolledOut() {
        isBuyRolledOut(0.0, false)
        isBuyRolledOut(0.3, false)
        isBuyRolledOut(0.5, true)
        isBuyRolledOut(1.0, true)
    }

    private fun isBuyRolledOut(percentage: Double, expectedResult: Boolean) {
        // Arrange
        whenever(mockPayloadDataManager.wallet!!.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(percentage)

        // Act
        val testObserver = subject.isBuyRolledOut.test()

        // Assert
        verify(mockPayloadDataManager, atLeastOnce()).wallet
        verify(mockWalletOptions, atLeastOnce()).rolloutPercentage
        verifyNoMoreInteractions(mockPayloadDataManager)
        verifyNoMoreInteractions(mockWalletOptions)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(expectedResult)
    }

    @Test
    fun `isCoinifyAllowed is sepa country, no account`() {
        // Arrange
        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockExchangeData.coinify!!.user).thenReturn(0)

        // Act
        val testObserver = subject.isCoinifyAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isCoinifyAllowed is sepa country, with account`() {
        // Arrange
        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockExchangeData.coinify!!.user).thenReturn(100)

        // Act
        val testObserver = subject.isCoinifyAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isCoinifyAllowed not sepa country, no account`() {
        // Arrange
        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("RSA"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockExchangeData.coinify!!.user).thenReturn(0)

        // Act
        val testObserver = subject.isCoinifyAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isCoinifyAllowed not sepa country, with account`() {
        // Arrange
        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("RSA"))
        whenever(mockSettings.countryCode).thenReturn("GB")
        whenever(mockExchangeData.coinify!!.user).thenReturn(100)

        // Act
        val testObserver = subject.isCoinifyAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isUnocoinAllowed is india country, is invited, has account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.unocoin.countries).thenReturn(listOf("IND"))
        whenever(mockSettings.countryCode).thenReturn("IND")
        whenever(mockSettings.invited["unocoin"]).thenReturn(true)
        whenever(mockWalletOptions.androidFlags.containsKey("showUnocoin")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showUnocoin"]).thenReturn(true)

        // Act
        val testObserver = subject.isUnocoinAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isUnocoinAllowed not india country, is invited, has account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.unocoin.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("IND")
        whenever(mockSettings.invited.containsKey("unocoin")).thenReturn(true)
        whenever(mockSettings.invited["unocoin"]).thenReturn(true)
        whenever(mockWalletOptions.androidFlags.containsKey("showUnocoin")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showUnocoin"]).thenReturn(true)

        // Act
        val testObserver = subject.isUnocoinAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isUnocoinAllowed not india country, not invited, has account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.unocoin.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("IND")
        whenever(mockSettings.invited["unocoin"]).thenReturn(false)
        whenever(mockWalletOptions.androidFlags.containsKey("showUnocoin")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showUnocoin"]).thenReturn(true)

        // Act
        val testObserver = subject.isUnocoinAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isUnocoinAllowed not india country, not invited, no account, androidDisabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.unocoin.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("IND")
        whenever(mockSettings.invited["unocoin"]).thenReturn(false)
        whenever(mockWalletOptions.androidFlags.containsKey("showUnocoin")).thenReturn(false)
        whenever(mockWalletOptions.androidFlags["showUnocoin"]).thenReturn(false)

        // Act
        val testObserver = subject.isUnocoinAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isSfoxAllowed is USA country and state, has account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn("some-user-id")

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isSfoxAllowed is USA country, wrong state, has account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("TX")

        whenever(mockExchangeData.sfox!!.user).thenReturn("some-user-id")

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isSfoxAllowed not USA country, has account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn("some-user-id")

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isSfoxAllowed is USA country and state, no account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isSfoxAllowed is USA country, wrong state, no account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("TX")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isSfoxAllowed not USA country, no account, androidEnabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isSfoxAllowed is USA country and state, no account, androidDisabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(false)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isSfoxAllowed is USA country, wrong state, no account, androidDisabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("TX")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(false)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `isSfoxAllowed not USA country, no account, androidDisabled`() {
        // Arrange
        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(false)

        // Act
        val testObserver = subject.isSfoxAllowed.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `canBuy isAllowed and androidEnabled`() {

        // individual cases have been tested above

        // Arrange
        whenever(mockPayloadDataManager.wallet!!.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(1.0)

        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("NY")

        whenever(mockExchangeData.sfox!!.user).thenReturn("some-user-id")

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.canBuy.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `canBuy isAllowed but not rolled out`() {

        // individual cases have been tested above

        // Arrange
        whenever(mockPayloadDataManager.wallet!!.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(0.0)

        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("GB"))
        whenever(mockSettings.countryCode).thenReturn("GB")

        whenever(mockExchangeData.coinify!!.user).thenReturn(100)

        // Act
        val testObserver = subject.canBuy.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `canBuy notAllowed and androidEnabled`() {

        // individual cases have been tested above

        // Arrange
        whenever(mockPayloadDataManager.wallet!!.guid).thenReturn("7279615c-23eb-4a1c-92df-2440acea8e1a")
        whenever(mockWalletOptions.rolloutPercentage).thenReturn(1.0)

        whenever(mockWalletOptions.partners.sfox.countries).thenReturn(listOf("USA"))
        whenever(mockSettings.countryCode).thenReturn("USA")
        whenever(mockWalletOptions.partners.sfox.states).thenReturn(listOf("NY"))
        whenever(mockSettings.state).thenReturn("TX")

        whenever(mockExchangeData.sfox!!.user).thenReturn(null)

        whenever(mockWalletOptions.androidFlags.containsKey("showSfox")).thenReturn(true)
        whenever(mockWalletOptions.androidFlags["showSfox"]).thenReturn(true)

        // Act
        val testObserver = subject.canBuy.test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun getCountryCode() {
        // Arrange
        whenever(mockSettings.countryCode).thenReturn("GB")
        // Act
        val testObserver = subject.countryCode.test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("GB")
    }

    @Test
    fun `isInCoinifyCountry true`() {
        // Arrange
        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("GB"))
        // Act
        val testObserver = subject.isInCoinifyCountry("GB").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `isInCoinifyCountry false`() {
        // Arrange
        whenever(mockWalletOptions.partners.coinify.countries).thenReturn(listOf("GB"))
        // Act
        val testObserver = subject.isInCoinifyCountry("ZA").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }
}