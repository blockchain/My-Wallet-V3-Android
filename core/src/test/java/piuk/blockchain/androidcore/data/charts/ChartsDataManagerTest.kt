package piuk.blockchain.androidcore.data.charts

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.Scale
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.data.rxjava.RxBus

class ChartsDataManagerTest : RxTest() {

    private lateinit var subject: ChartsDataManager
    private val historicPriceApi: PriceApi = mock()
    private val rxBus = RxBus()

    @Before
    fun setUp() {
        subject = ChartsDataManager(historicPriceApi, rxBus)
    }

    @Test
    fun `getAllTimePrice BTC`() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                btc.symbol,
                fiat,
                FIRST_BTC_ENTRY_TIME,
                Scale.FIVE_DAYS
            )
        ).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getAllTimePrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            btc.symbol,
            fiat,
            FIRST_BTC_ENTRY_TIME,
            Scale.FIVE_DAYS
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun `getAllTimePrice ETH`() {
        // Arrange
        val eth = CryptoCurrency.ETHER
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eth.symbol,
                fiat,
                FIRST_ETH_ENTRY_TIME,
                Scale.FIVE_DAYS
            )
        ).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getAllTimePrice(eth, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eth.symbol,
            fiat,
            FIRST_ETH_ENTRY_TIME,
            Scale.FIVE_DAYS
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getYearPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.ONE_DAY)
            )
        ).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getYearPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.symbol),
            eq(fiat),
            any(),
            eq(Scale.ONE_DAY)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getMonthPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.TWO_HOURS)
            )
        ).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getMonthPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.symbol),
            eq(fiat),
            any(),
            eq(Scale.TWO_HOURS)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getWeekPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.ONE_HOUR)
            )
        ).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getWeekPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.symbol),
            eq(fiat),
            any(),
            eq(Scale.ONE_HOUR)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getDayPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.FIFTEEN_MINUTES)
            )
        ).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getDayPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.symbol),
            eq(fiat),
            any(),
            eq(Scale.FIFTEEN_MINUTES)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }
}