package piuk.blockchain.android.ui.dashboard

import com.blockchain.balance.TotalBalance
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import com.blockchain.testutils.lumens
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import java.math.BigDecimal
import java.util.Locale

class AsyncDashboardDataCalculatorTest {

    @Test
    fun `gets correct balances`() {
        val balanceUpdater = mock<BalanceUpdater> {
            on { updateBalances() } `it returns` Completable.complete()
        }
        val mockBalances = mockBalances(
            bitcoin = TotalBalance.Balance(
                spendable = 20.bitcoin(),
                watchOnly = 15.bitcoin(),
                coldStorage = 5.bitcoin()
            ),
            bitcoinCash = TotalBalance.Balance(
                spendable = 29.bitcoinCash(),
                watchOnly = 5.bitcoinCash(),
                coldStorage = 1.bitcoinCash()
            ),
            ether = TotalBalance.Balance(
                spendable = 19.ether(),
                watchOnly = 5.ether(),
                coldStorage = 1.ether()
            ),
            lumens = TotalBalance.Balance(
                spendable = 98.lumens(),
                watchOnly = 6.lumens(),
                coldStorage = 2.lumens()
            )
        )
        val balanceFilter = PublishSubject.create<BalanceFilter>()
        val test = AsyncDashboardDataCalculator(
            mockExchangeRates(
                mapOf(
                    25.bitcoin() to 100.gbp(),
                    20.bitcoin() to 75.gbp(),
                    15.bitcoin() to 200.gbp(),
                    5.bitcoin() to 20.gbp(),
                    30.bitcoinCash() to 300.gbp(),
                    29.bitcoinCash() to 290.gbp(),
                    5.bitcoinCash() to 400.gbp(),
                    1.bitcoinCash() to 1.gbp(),
                    20.ether() to 500.gbp(),
                    19.ether() to 490.gbp(),
                    5.ether() to 50.gbp(),
                    1.ether() to 3.gbp(),
                    100.lumens() to 600.gbp(),
                    98.lumens() to 580.gbp(),
                    6.lumens() to 70.gbp(),
                    2.lumens() to 5.gbp()
                )
            ),
            balanceUpdater,
            mockBalances
        )
            .getPieChartData(balanceFilter)
            .test()
        balanceFilter.onNext(BalanceFilter.Total)
        balanceFilter.onNext(BalanceFilter.Wallet)
        balanceFilter.onNext(BalanceFilter.ColdStorage)
        test.values() `should equal`
            listOf(
                PieChartsState.Data(
                    bitcoin = PieChartsState.Coin(
                        displayable = dataPoint(100.gbp(), 25.bitcoin()),
                        watchOnly = dataPoint(200.gbp(), 15.bitcoin())
                    ),
                    bitcoinCash = PieChartsState.Coin(
                        displayable = dataPoint(300.gbp(), 30.bitcoinCash()),
                        watchOnly = dataPoint(400.gbp(), 5.bitcoinCash())
                    ),
                    ether = PieChartsState.Coin(
                        displayable = dataPoint(500.gbp(), 20.ether()),
                        watchOnly = dataPoint(50.gbp(), 5.ether())
                    ),
                    lumen = PieChartsState.Coin(
                        displayable = dataPoint(600.gbp(), 100.lumens()),
                        watchOnly = dataPoint(70.gbp(), 6.lumens())
                    )
                ),
                PieChartsState.Data(
                    bitcoin = PieChartsState.Coin(
                        displayable = dataPoint(75.gbp(), 20.bitcoin()),
                        watchOnly = dataPoint(200.gbp(), 15.bitcoin())
                    ),
                    bitcoinCash = PieChartsState.Coin(
                        displayable = dataPoint(290.gbp(), 29.bitcoinCash()),
                        watchOnly = dataPoint(400.gbp(), 5.bitcoinCash())
                    ),
                    ether = PieChartsState.Coin(
                        displayable = dataPoint(490.gbp(), 19.ether()),
                        watchOnly = dataPoint(50.gbp(), 5.ether())
                    ),
                    lumen = PieChartsState.Coin(
                        displayable = dataPoint(580.gbp(), 98.lumens()),
                        watchOnly = dataPoint(70.gbp(), 6.lumens())
                    )
                ),
                PieChartsState.Data(
                    bitcoin = PieChartsState.Coin(
                        displayable = dataPoint(20.gbp(), 5.bitcoin()),
                        watchOnly = dataPoint(200.gbp(), 15.bitcoin())
                    ),
                    bitcoinCash = PieChartsState.Coin(
                        displayable = dataPoint(1.gbp(), 1.bitcoinCash()),
                        watchOnly = dataPoint(400.gbp(), 5.bitcoinCash())
                    ),
                    ether = PieChartsState.Coin(
                        displayable = dataPoint(3.gbp(), 1.ether()),
                        watchOnly = dataPoint(50.gbp(), 5.ether())
                    ),
                    lumen = PieChartsState.Coin(
                        displayable = dataPoint(5.gbp(), 2.lumens()),
                        watchOnly = dataPoint(70.gbp(), 6.lumens())
                    )
                )
            )
        verify(balanceUpdater).updateBalances()
        verify(mockBalances).totalBalance(CryptoCurrency.BTC)
        verify(mockBalances).totalBalance(CryptoCurrency.BCH)
        verify(mockBalances).totalBalance(CryptoCurrency.ETHER)
        verify(mockBalances).totalBalance(CryptoCurrency.XLM)
    }

    private fun dataPoint(fiat: FiatValue, cryptoValue: CryptoValue) =
        PieChartsState.DataPoint(fiat, cryptoValue.toStringWithSymbol(Locale.US))

    private fun mockBalances(
        bitcoin: TotalBalance.Balance,
        bitcoinCash: TotalBalance.Balance,
        ether: TotalBalance.Balance,
        lumens: TotalBalance.Balance
    ): TotalBalance =
        mock {
            on {
                totalBalance(CryptoCurrency.BTC)
            } `it returns` Single.just(bitcoin)
            on {
                totalBalance(CryptoCurrency.BCH)
            } `it returns` Single.just(bitcoinCash)
            on {
                totalBalance(CryptoCurrency.ETHER)
            } `it returns` Single.just(ether)
            on {
                totalBalance(CryptoCurrency.XLM)
            } `it returns` Single.just(lumens)
        }

    private fun mockExchangeRates(param: Map<CryptoValue, FiatValue>): FiatExchangeRates {
        val fiatExchangeRates = mock<FiatExchangeRates>()
        whenever(fiatExchangeRates.getFiat(any())) `it returns` FiatValue.fromMajor("USD", BigDecimal.ZERO)
        param.forEach { crypto, fiat ->
            whenever(fiatExchangeRates.getFiat(crypto)) `it returns` fiat
        }
        return fiatExchangeRates
    }
}
