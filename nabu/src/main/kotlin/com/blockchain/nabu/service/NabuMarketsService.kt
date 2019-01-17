package com.blockchain.nabu.service

import com.blockchain.morph.CoinPair
import com.blockchain.morph.exchange.service.FiatPeriodicLimit
import com.blockchain.morph.exchange.service.FiatTradesLimits
import com.blockchain.morph.exchange.service.TradeLimitService
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
) : TradeLimitService {

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

    override fun getTradesLimits(fiatCurrency: String): Single<FiatTradesLimits> {
        return authenticator.authenticate {
            nabuMarkets.getTradesLimits(
                fiatCurrency,
                it.authHeader
            )
        }.map {
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

    fun executeTrade(
        tradeRequest: TradeRequest
    ): Single<NabuTransaction> {
        return authenticator.authenticate {
            nabuMarkets.executeTrade(tradeRequest, it.authHeader)
        }.map { it.map() }
    }

    fun getTrades(userFiatCurrency: String): Single<List<NabuTransaction>> {
        return authenticator.authenticate {
            nabuMarkets.getTrades(userFiatCurrency, it.authHeader)
        }.flattenAsObservable { it }
            .map { it.map() }
            .toList()
    }
}

private fun PeriodicLimit?.toFiat(currencyCode: String) =
    if (this == null) FiatPeriodicLimit(
        null, null, null
    ) else {
        FiatPeriodicLimit(
            limit = limit?.let { FiatValue.fromMajor(currencyCode, it) },
            available = available?.let { FiatValue.fromMajor(currencyCode, it) },
            used = used?.let { FiatValue.fromMajor(currencyCode, it) }
        )
    }

private fun TradeJson.map(): NabuTransaction {
    val coinPair = CoinPair.fromPairCode(this.pair.replace("-", "_"))

    return NabuTransaction(
        id = this.id,
        createdAt = this.createdAt,
        pair = coinPair,
        fee = this.withdrawalFee.toCryptoValue(),
        fiatValue = this.fiatValue.toFiatValue(),
        refundAddress = this.refundAddress,
        depositAddress = this.depositAddress,
        deposit = this.deposit?.toCryptoValue() ?: CryptoValue.zero(coinPair.from),
        withdrawalAddress = this.withdrawalAddress,
        withdrawal = this.withdrawal?.toCryptoValue() ?: CryptoValue.zero(coinPair.to),
        state = this.state,
        hashOut = this.withdrawalTxHash,
        depositTextMemo = this.depositMemo
    )
}

private fun Value.toCryptoValue(): CryptoValue =
    CryptoCurrency.fromSymbolOrThrow(symbol).withMajorValue(value)

private fun Value.toFiatValue(): FiatValue =
    FiatValue.fromMajor(this.symbol, this.value)
