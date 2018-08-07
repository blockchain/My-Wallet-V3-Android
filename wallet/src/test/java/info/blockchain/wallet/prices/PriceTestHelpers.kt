package info.blockchain.wallet.prices

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class MockCurrentPriceApi(
    private val base: CryptoCurrency,
    private val quoteFiat: String,
    private val currentPriceApi: CurrentPriceApi = mock()
) : CurrentPriceApi by currentPriceApi {

    fun givenPrice(price: Double, delaySeconds: Long = 0): MockCurrentPriceApi {
        whenever(currentPriceApi.currentPrice(base, quoteFiat)) `it returns` if (delaySeconds == 0L) {
            Single.just(BigDecimal.valueOf(price))
        } else {
            Single.timer(delaySeconds, TimeUnit.SECONDS)
                .map { BigDecimal.valueOf(price) }
        }
        return this
    }

    fun givenAnError() {
        whenever(
            currentPriceApi.currentPrice(
                base,
                quoteFiat
            )
        ) `it returns` Single.error<BigDecimal>(RuntimeException())
    }

    fun verifyNumberOfApiCalls(expectedCalls: Int) {
        verify(currentPriceApi, times(expectedCalls))
            .currentPrice(base, quoteFiat)
        verifyNoMoreInteractions(currentPriceApi)
    }
}

fun givenCachedIndicativeFiatPriceService(currentPriceApi: CurrentPriceApi) =
    currentPriceApi.toCachedIndicativeFiatPriceService()

fun <T> Observable<T>.assertSingle(): T =
    test().values().single()
