package com.blockchain.morph.homebrew.dataadapters

import com.blockchain.morph.CoinPair
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeOrder
import com.blockchain.nabu.api.NabuTransaction
import com.blockchain.nabu.api.TransactionState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import java.math.BigDecimal

internal class NabuTradeAdapter(private val trade: NabuTransaction) : MorphTrade {

    override val hashOut: String?
        get() = trade.hashOut

    override val status: MorphTrade.Status
        get() = trade.state.map()

    override val quote: MorphTradeOrder
        get() = object : MorphTradeOrder {
            override val pair: CoinPair
                get() = trade.pair

            override val orderId: String
                get() = trade.id

            override val depositAmount: CryptoValue
                get() = trade.deposit

            override val withdrawalAmount: CryptoValue
                get() = trade.withdrawal

            override val quotedRate: BigDecimal
                get() = trade.rate

            override val minerFee: CryptoValue
                get() = when (pair.to) {
                    CryptoCurrency.BTC -> CryptoValue.ZeroBtc
                    CryptoCurrency.ETHER -> CryptoValue.ZeroEth
                    CryptoCurrency.BCH -> CryptoValue.ZeroBch
                }
        }

    override fun enoughInfoForDisplay() = true
}

internal fun TransactionState.map(): MorphTrade.Status =
    when (this) {
        TransactionState.PendingExecution,
        TransactionState.PendingDeposit,
        TransactionState.FinishedDeposit,
        TransactionState.PendingWithdrawal -> MorphTrade.Status.NO_DEPOSITS
        TransactionState.Finished -> MorphTrade.Status.COMPLETE
        TransactionState.PendingRefund, TransactionState.Refunded -> MorphTrade.Status.RESOLVED
        TransactionState.Failed, TransactionState.Expired -> MorphTrade.Status.FAILED
    }