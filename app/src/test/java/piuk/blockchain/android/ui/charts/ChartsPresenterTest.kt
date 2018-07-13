package piuk.blockchain.android.ui.charts

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.charts.models.ChartDatumDto
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.util.Locale

class ChartsPresenterTest {

    private lateinit var subject: ChartsPresenter
    private val chartsDataManager: ChartsDataManager = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val view: ChartsView = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()

    @Before
    fun setUp() {

        subject = ChartsPresenter(
            chartsDataManager,
            exchangeRateFactory,
            prefsUtil,
            currencyFormatManager
        )

        subject.initView(view)
    }

    @Test
    fun `onViewReady success`() {
        // Arrange
        val chartData = ChartDatumDto(mock(PriceDatum::class))
        val fiat = "USD"
        whenever(view.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(view.locale).thenReturn(Locale.UK)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn(fiat)
        whenever(exchangeRateFactory.getLastBtcPrice(fiat)).thenReturn(13950.0)
        whenever(chartsDataManager.getMonthPrice(CryptoCurrency.BTC, fiat))
            .thenReturn(Observable.just(chartData))
        // Act
        subject.onViewReady()
        // Assert
        subject.selectedTimeSpan `should be` TimeSpan.MONTH
        verify(view, atLeastOnce()).cryptoCurrency
        verify(view, atLeastOnce()).locale
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.TimeSpanUpdated(any(TimeSpan::class)))
        verify(view).updateChartState(ChartsState.Loading)
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.Data(any(), eq("US$")))
//        verifyNoMoreInteractions(view)
        verify(chartsDataManager).getMonthPrice(CryptoCurrency.BTC, fiat)
        verifyNoMoreInteractions(chartsDataManager)
        verify(exchangeRateFactory).getLastBtcPrice(fiat)
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(prefsUtil, atLeastOnce()).getValue(
            PrefsUtil.KEY_SELECTED_FIAT,
            PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `onViewReady failure`() {
        // Arrange
        val fiat = "USD"
        whenever(view.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(view.locale).thenReturn(Locale.UK)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn(fiat)
        whenever(exchangeRateFactory.getLastBtcPrice(fiat)).thenReturn(13950.0)
        whenever(chartsDataManager.getMonthPrice(CryptoCurrency.BTC, fiat))
            .thenReturn(Observable.error(Throwable()))
        // Act
        subject.onViewReady()
        // Assert
        subject.selectedTimeSpan `should be` TimeSpan.MONTH
        verify(view, atLeastOnce()).cryptoCurrency
        verify(view, atLeastOnce()).locale
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.TimeSpanUpdated(any(TimeSpan::class)))
        verify(view).updateChartState(ChartsState.Loading)
        verify(view).updateChartState(ChartsState.Error)
//        verifyNoMoreInteractions(view)
        verify(chartsDataManager).getMonthPrice(CryptoCurrency.BTC, fiat)
        verifyNoMoreInteractions(chartsDataManager)
        verify(exchangeRateFactory).getLastBtcPrice(fiat)
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(prefsUtil, atLeastOnce()).getValue(
            PrefsUtil.KEY_SELECTED_FIAT,
            PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `setSelectedTimeSpan day`() {
        // Arrange
        val chartData = ChartDatumDto(mock(PriceDatum::class))
        val fiat = "USD"
        whenever(view.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(view.locale).thenReturn(Locale.UK)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn(fiat)
        whenever(exchangeRateFactory.getLastBtcPrice(fiat)).thenReturn(13950.0)
        whenever(chartsDataManager.getDayPrice(CryptoCurrency.BTC, fiat))
            .thenReturn(Observable.just(chartData))
        // Act
        subject.selectedTimeSpan = TimeSpan.DAY
        // Assert
        subject.selectedTimeSpan `should be` TimeSpan.DAY
        verify(view, atLeastOnce()).cryptoCurrency
        verify(view, atLeastOnce()).locale
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.TimeSpanUpdated(any(TimeSpan::class)))
        verify(view).updateChartState(ChartsState.Loading)
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.Data(any(), eq("US$")))
//        verifyNoMoreInteractions(view)
        verify(chartsDataManager).getDayPrice(CryptoCurrency.BTC, fiat)
        verifyNoMoreInteractions(chartsDataManager)
        verify(exchangeRateFactory).getLastBtcPrice(fiat)
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(prefsUtil, atLeastOnce()).getValue(
            PrefsUtil.KEY_SELECTED_FIAT,
            PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `setSelectedTimeSpan week ETH`() {
        // Arrange
        val chartData = ChartDatumDto(mock(PriceDatum::class))
        val fiat = "USD"
        whenever(view.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        whenever(view.locale).thenReturn(Locale.UK)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn(fiat)
        whenever(exchangeRateFactory.getLastEthPrice(fiat)).thenReturn(1281.78)
        whenever(chartsDataManager.getWeekPrice(CryptoCurrency.ETHER, fiat))
            .thenReturn(Observable.just(chartData))
        // Act
        subject.selectedTimeSpan = TimeSpan.WEEK
        // Assert
        subject.selectedTimeSpan `should be` TimeSpan.WEEK
        verify(view, atLeastOnce()).cryptoCurrency
        verify(view, atLeastOnce()).locale
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.TimeSpanUpdated(any(TimeSpan::class)))
        verify(view).updateChartState(ChartsState.Loading)
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.Data(any(), eq("US$")))
//        verifyNoMoreInteractions(view)
        verify(chartsDataManager).getWeekPrice(CryptoCurrency.ETHER, fiat)
        verifyNoMoreInteractions(chartsDataManager)
        verify(exchangeRateFactory).getLastEthPrice(fiat)
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(prefsUtil, atLeastOnce()).getValue(
            PrefsUtil.KEY_SELECTED_FIAT,
            PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `setSelectedTimeSpan year ETH`() {
        // Arrange
        val chartData = ChartDatumDto(mock(PriceDatum::class))
        val fiat = "USD"
        whenever(view.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        whenever(view.locale).thenReturn(Locale.UK)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn(fiat)
        whenever(exchangeRateFactory.getLastEthPrice(fiat)).thenReturn(1281.78)
        whenever(chartsDataManager.getYearPrice(CryptoCurrency.ETHER, fiat))
            .thenReturn(Observable.just(chartData))
        // Act
        subject.selectedTimeSpan = TimeSpan.YEAR
        // Assert
        subject.selectedTimeSpan `should be` TimeSpan.YEAR
        verify(view, atLeastOnce()).cryptoCurrency
        verify(view, atLeastOnce()).locale
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.TimeSpanUpdated(any(TimeSpan::class)))
        verify(view).updateChartState(ChartsState.Loading)
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.Data(any(), eq("US$")))
//        verifyNoMoreInteractions(view)
        verify(chartsDataManager).getYearPrice(CryptoCurrency.ETHER, fiat)
        verifyNoMoreInteractions(chartsDataManager)
        verify(exchangeRateFactory).getLastEthPrice(fiat)
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(prefsUtil, atLeastOnce()).getValue(
            PrefsUtil.KEY_SELECTED_FIAT,
            PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `setSelectedTimeSpan all time BCH`() {
        // Arrange
        val chartData = ChartDatumDto(mock(PriceDatum::class))
        val fiat = "USD"
        whenever(view.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
        whenever(view.locale).thenReturn(Locale.UK)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn(fiat)
        whenever(exchangeRateFactory.getLastBchPrice(fiat)).thenReturn(1281.78)
        whenever(chartsDataManager.getAllTimePrice(CryptoCurrency.BCH, fiat))
            .thenReturn(Observable.just(chartData))
        // Act
        subject.selectedTimeSpan = TimeSpan.ALL_TIME
        // Assert
        subject.selectedTimeSpan `should be` TimeSpan.ALL_TIME
        verify(view, atLeastOnce()).cryptoCurrency
        verify(view, atLeastOnce()).locale
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.TimeSpanUpdated(any(TimeSpan::class)))
        verify(view).updateChartState(ChartsState.Loading)
        // TODO: Mockito currently doesn't play nicely with child classes of sealed parents
//        verify(view).updateChartState(ChartsState.Data(any(), eq("US$")))
//        verifyNoMoreInteractions(view)
        verify(chartsDataManager).getAllTimePrice(CryptoCurrency.BCH, fiat)
        verifyNoMoreInteractions(chartsDataManager)
        verify(exchangeRateFactory).getLastBchPrice(fiat)
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(prefsUtil, atLeastOnce()).getValue(
            PrefsUtil.KEY_SELECTED_FIAT,
            PrefsUtil.DEFAULT_CURRENCY
        )
        verifyNoMoreInteractions(prefsUtil)
    }
}