package info.blockchain.wallet.prices

import com.blockchain.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import info.blockchain.balance.CryptoCurrency
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class CachedIndicativeFiatPriceServiceInverseTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val rx = rxInit {
        computation(testScheduler)
    }

    @Test
    fun `immediate initial request`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BTC,
            "GBP"
        ).givenPrice(99.0)

        givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(fromFiat = "GBP", to = CryptoCurrency.BTC)
            .assertSingle()
            .apply {
                rate `should equal` BigDecimal.valueOf(1 / 99.0)
                from `should equal` "GBP"
                to `should equal` CryptoCurrency.BTC
            }
        mockPriceApi.verifyNumberOfApiCalls(1)
    }

    @Test
    fun `opposite requests are cached, so multiple subscriptions do not cause multiple hits to server`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BCH,
            "USD"
        ).givenPrice(99.0)

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)
        service
            .indicativeRateStream(fromFiat = "USD", to = CryptoCurrency.BCH)
            .test()

        service
            .indicativeRateStream(from = CryptoCurrency.BCH, toFiat = "USD")
            .test()

        mockPriceApi.verifyNumberOfApiCalls(1)
    }

    @Test
    fun `different pairs result in multiple calls`() {
        val mockPriceApi = mock<CurrentPriceApi>()

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)
        service
            .indicativeRateStream(fromFiat = "GBP", to = CryptoCurrency.BCH)
            .test()

        service
            .indicativeRateStream(from = CryptoCurrency.BCH, toFiat = "USD")
            .test()

        service
            .indicativeRateStream(from = CryptoCurrency.ETHER, toFiat = "GBP")
            .test()

        verify(mockPriceApi).currentPrice(CryptoCurrency.BCH, "GBP")
        verify(mockPriceApi).currentPrice(CryptoCurrency.BCH, "USD")
        verify(mockPriceApi).currentPrice(CryptoCurrency.ETHER, "GBP")
        verifyNoMoreInteractions(mockPriceApi)
    }
}
