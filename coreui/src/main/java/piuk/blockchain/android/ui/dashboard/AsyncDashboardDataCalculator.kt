package piuk.blockchain.android.ui.dashboard

import com.blockchain.balance.TotalBalance
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.toObservable

import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toFiat

class AsyncDashboardDataCalculator(
    private val fiatExchangeRates: FiatExchangeRates,
    private val balanceUpdater: BalanceUpdater,
    private val totalBalance: TotalBalance
) : DashboardData {

    override fun getPieChartData(balanceFilter: Observable<BalanceFilter>): Observable<PieChartsState.Data> =
        balanceUpdater.updateBalances()
            .andThen(
                pieChartData(balanceFilter)
            )

    private fun pieChartData(balanceFilter: Observable<BalanceFilter>) =
        Observables.combineLatest(
            balancesForEveryDashboardCurrency().toObservable(),
            balanceFilter
        ).map { (balanceMap, balanceMode) ->
            balanceMap.values.map { value ->
                value.spendable.currency to value
                    .filterByMode(balanceMode)
                    .toPieChartCoin(fiatExchangeRates)
            }.toMap()
        }.map {
            PieChartsState.Data(
                bitcoin = it.coin(CryptoCurrency.BTC),
                bitcoinCash = it.coin(CryptoCurrency.BCH),
                ether = it.coin(CryptoCurrency.ETHER),
                lumen = it.coin(CryptoCurrency.XLM)
            )
        }

    private fun balancesForEveryDashboardCurrency() =
        DashboardConfig.currencies.toObservable()
            .flatMapSingle {
                totalBalance.totalBalance(it)
            }
            .toMap(
                { it.spendable.currency },
                { it }
            )

    private fun TotalBalance.Balance.filterByMode(balanceFilter: BalanceFilter) =
        when (balanceFilter) {
            BalanceFilter.Total -> this
            BalanceFilter.Wallet -> copy(coldStorage = coldStorage.toZero())
            BalanceFilter.ColdStorage -> copy(spendable = spendable.toZero())
        }

    private fun Map<CryptoCurrency, PieChartsState.Coin>.coin(btc: CryptoCurrency) =
        this[btc] ?: zeroCoin(btc, fiatExchangeRates)
}

fun zeroCoin(currency: CryptoCurrency, fiatExchangeRates: FiatExchangeRates) =
    TotalBalance.Balance.zero(currency).toPieChartCoin(fiatExchangeRates)

private fun TotalBalance.Balance.toPieChartCoin(fiatExchangeRates: FiatExchangeRates) =
    PieChartsState.Coin(
        displayable = spendableAndColdStorage.toPieChartDataPoint(fiatExchangeRates),
        watchOnly = watchOnly.toPieChartDataPoint(fiatExchangeRates)
    )

fun CryptoValue.toPieChartDataPoint(fiatExchangeRates: FiatExchangeRates) =
    PieChartsState.DataPoint(
        fiatValue = toFiat(fiatExchangeRates),
        cryptoValueString = formatWithUnit()
    )
