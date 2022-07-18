package com.blockchain.core.custodial

import com.blockchain.core.custodial.domain.TradingStoreService
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

data class TradingAccountBalance(
    val total: Money,
    val withdrawable: Money,
    val pending: Money,
    val hasTransactions: Boolean = false,
)

interface TradingBalanceDataManager {
    fun getBalanceForCurrency(currency: Currency): Observable<TradingAccountBalance>
    fun getActiveAssets(): Single<Set<Currency>>
}

internal class TradingBalanceDataManagerImpl(
    private val tradingStoreService: TradingStoreService
) : TradingBalanceDataManager {
    override fun getBalanceForCurrency(currency: Currency): Observable<TradingAccountBalance> =
        tradingStoreService.getBalanceFor(asset = currency)

    override fun getActiveAssets(): Single<Set<Currency>> =
        tradingStoreService.getActiveAssets()
}
