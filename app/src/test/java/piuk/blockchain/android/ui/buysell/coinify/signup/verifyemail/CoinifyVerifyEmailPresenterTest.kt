package piuk.blockchain.android.ui.buysell.coinify.signup.verifyemail

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.Trader
import piuk.blockchain.androidbuysell.models.coinify.TraderResponse
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import java.util.concurrent.TimeUnit

class CoinifyVerifyEmailPresenterTest : RxTest() {

    private lateinit var subject: CoinifyVerifyEmailPresenter

    private val view: CoinifyVerifyEmailView = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val currencyState: CurrencyState = mock()
    private val stringUtils: StringUtils = mock()

    @Before
    fun setup() {
        subject =
            CoinifyVerifyEmailPresenter(
                settingsDataManager,
                walletOptionsDataManager,
                payloadDataManager,
                exchangeService,
                coinifyDataManager,
                metadataManager,
                currencyState,
                stringUtils
            )
        subject.initView(view)
    }

    @Test
    fun `onViewReady unverified email`() {

        // Arrange
        val email = "hello@email.com"

        val settings: Settings = mock()
        whenever(settings.isEmailVerified).thenReturn(false)
        whenever(settings.email).thenReturn(email)
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(settings))
        whenever(settingsDataManager.updateEmail(any())).thenReturn(Observable.empty())

        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS)

        // Assert
        verify(view).onEnableContinueButton(false)
        verify(view).onShowUnverifiedEmail(email)
        verify(view, times(2)).showLoading(any())
        verify(settingsDataManager).updateEmail(email)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady verified email`() {

        // Arrange
        val email = "hello@email.com"

        val settings: Settings = mock()
        whenever(settings.isEmailVerified).thenReturn(true)
        whenever(settings.email).thenReturn(email)
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(settings))
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS)

        // Assert
        verify(view).onEnableContinueButton(true)
        verify(view).onShowVerifiedEmail(email)
        verify(view, times(2)).showLoading(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady unexpected error`() {
        // Arrange
        whenever(settingsDataManager.fetchSettings())
            .thenReturn(Observable.error(Throwable("Forced fail")))

        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS)

        // Assert
        verify(view).onShowErrorAndClose()
        verify(view, times(2)).showLoading(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onContinueClicked no verified email`() {

        // Arrange

        // Act
        subject.onContinueClicked("UK")

        // Assert
        verify(view).onShowErrorAndClose()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun onContinueClicked() {

        // Arrange
        subject.setVerifiedEmailAndDisplay("hey@email.com")
        whenever(walletOptionsDataManager.getCoinifyPartnerId()).thenReturn(Observable.just(123))
        whenever(currencyState.fiatUnit).thenReturn("GBP")

        whenever(payloadDataManager.guid).thenReturn("guid")
        whenever(payloadDataManager.sharedKey).thenReturn("sharedKey")

        val mockTraderResponse: TraderResponse = mock()
        val mockTrader: Trader = mock()
        val mockExchangeData: ExchangeData = mock()
        val mockKycResponse: KycResponse = mock()

        whenever(mockTrader.id).thenReturn(555)
        whenever(mockTraderResponse.trader).thenReturn(mockTrader)
        whenever(mockTraderResponse.offlineToken).thenReturn("token")

        whenever(
            coinifyDataManager.getEmailTokenAndSignUp(
                any(), any(),
                any(), any(), any(), any()
            )
        ).thenReturn(
            Observable.just(mockTraderResponse)
                .singleOrError()
        )
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(mockExchangeData))
        whenever(metadataManager.saveToMetadata(any(), any()))
            .thenReturn(Completable.complete())

        whenever(coinifyDataManager.startKycReview(any()))
            .thenReturn(Observable.just(mockKycResponse).singleOrError())

        // Act
        subject.onContinueClicked("UK")

        // Assert
        verify(view, atLeastOnce()).onShowVerifiedEmail("hey@email.com")
        verify(view, atLeastOnce()).onStartSignUpSuccess()
        verify(view).showLoading(true)
        verify(view).showLoading(false)
        verifyNoMoreInteractions(view)
    }
}