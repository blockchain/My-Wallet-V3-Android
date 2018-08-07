package info.blockchain.wallet.prices

import com.blockchain.testutils.rxInit
import info.blockchain.balance.CryptoCurrency
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CachedIndicativeFiatPriceServiceSharingTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val rx = rxInit {
        computation(testScheduler)
    }

    @Test
    fun `given unsubscribed, api calls stop`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BTC,
            "USD"
        )
            .givenPrice(99.0)

        val sub = givenCachedIndicativeFiatPriceService(mockPriceApi)
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .subscribe()

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        mockPriceApi.verifyNumberOfApiCalls(2)

        sub.dispose()

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        mockPriceApi.verifyNumberOfApiCalls(2)
    }

    @Test
    fun `given two subscriptions, api calls do not stop`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BTC,
            "USD"
        )
            .givenPrice(99.0)

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)
        val sub1 = service
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        val sub2 = service
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .test()

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        mockPriceApi.verifyNumberOfApiCalls(2)

        sub1.cancel()

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        mockPriceApi.verifyNumberOfApiCalls(3)

        sub2.values().map { it.rate } `should equal` listOf(
            BigDecimal.valueOf(99.0),
            BigDecimal.valueOf(99.0),
            BigDecimal.valueOf(99.0)
        )
    }

    @Test
    fun `given two of two unsubscribed, api calls stop`() {
        val mockPriceApi = MockCurrentPriceApi(
            CryptoCurrency.BTC,
            "USD"
        )
            .givenPrice(99.0)

        val service = givenCachedIndicativeFiatPriceService(mockPriceApi)

        val sub1 = service
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .subscribe()

        val sub2 = service
            .indicativeRateStream(from = CryptoCurrency.BTC, toFiat = "USD")
            .subscribe()

        mockPriceApi.verifyNumberOfApiCalls(1)

        sub1.dispose()
        sub2.dispose()

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        mockPriceApi.verifyNumberOfApiCalls(1)
    }
}
