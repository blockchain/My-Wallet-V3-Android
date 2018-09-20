package com.blockchain.nabu.service

import com.blockchain.morph.CoinPair
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.NabuMarkets
import com.blockchain.nabu.api.NabuTransaction
import com.blockchain.nabu.api.PeriodicLimit
import com.blockchain.nabu.api.TradeJson
import com.blockchain.nabu.api.TradeRequest
import com.blockchain.nabu.api.Value
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValue
import io.reactivex.Single

class NabuMarketsService internal constructor(
    private val nabuMarkets: NabuMarkets,
    private val authenticator: Authenticator
) {

    fun getTradingConfig(tradingPair: CoinPair): Single<TradingConfig> {
        return authenticator.authenticate {
            nabuMarkets.getTradingConfig(
                tradingPair.pairCodeUpper,
                it.authHeader
            ).map {
                TradingConfig(
                    minOrderSize = CryptoValue.fromMajor(
                        tradingPair.from,
                        it.minOrderSize
                    )
                )
            }
        }
    }

    fun getTradesLimits(): Single<FiatTradesLimits> {
        return authenticator.authenticate {
            nabuMarkets.getTradesLimits(
                it.authHeader
            ).map {
                FiatTradesLimits(
                    minOrder = FiatValue.fromMajor(it.currency, it.minOrder),
                    maxOrder = FiatValue.fromMajor(it.currency, it.maxOrder),
                    maxPossibleOrder = FiatValue.fromMajor(it.currency, it.maxPossibleOrder),
                    daily = it.daily.toFiat(it.currency),
                    weekly = it.weekly.toFiat(it.currency),
                    annual = it.annual.toFiat(it.currency)
                )
            }
        }
    }

    fun executeTrade(
        tradeRequest: TradeRequest
    ): Single<NabuTransaction> {
        return authenticator.authenticate {
            nabuMarkets.executeTrade(tradeRequest, it.authHeader)
        }.map { it.map() }
    }

    fun getTrades(): Single<List<NabuTransaction>> {
        return authenticator.authenticate {
            nabuMarkets.getTrades(it.authHeader)
        }.flattenAsObservable { it }
            .map { it.map() }
            .toList()
    }
}

private fun PeriodicLimit.toFiat(currencyCode: String) =
    FiatPeriodicLimit(
        limit = FiatValue.fromMajor(currencyCode, limit),
        available = FiatValue.fromMajor(currencyCode, available),
        used = FiatValue.fromMajor(currencyCode, used)
    )

private fun TradeJson.map(): NabuTransaction {
    val coinPair = CoinPair.fromPairCode(this.pair.replace("-", "_"))

    return NabuTransaction(
        id = this.id,
        createdAt = this.createdAt,
        pair = coinPair,
        rate = this.rate,
        fee = this.withdrawalFee.toCryptoValue(),
        fiatValue = this.fiatValue.toFiatValue(),
        refundAddress = this.refundAddress,
        depositAddress = this.depositAddress,
        deposit = this.deposit.toCryptoValue(),
        withdrawalAddress = this.withdrawalAddress,
        withdrawal = this.withdrawal.toCryptoValue(),
        state = this.state,
        hashOut = this.withdrawalTxHash
    )
}

private fun Value.toCryptoValue(): CryptoValue =
    CryptoCurrency.fromSymbolOrThrow(symbol).withMajorValue(value)

private fun Value.toFiatValue(): FiatValue =
    FiatValue.fromMajor(this.symbol, this.value)
