package com.blockchain.nabu.dataadapters

import com.blockchain.morph.CoinPair
import com.blockchain.nabu.api.NabuTransaction
import com.blockchain.nabu.api.TransactionState
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class NabuTradeAdapterTest {

    @Test
    fun `quote hash out`() {
        NabuTradeAdapter(
            getTransaction(hashOut = "Hash out")
        ).hashOut `should equal` "Hash out"
    }

    @Test
    fun `quote order id`() {
        NabuTradeAdapter(
            getTransaction(id = "ID")
        ).quote.orderId `should equal` "ID"
    }

    @Test
    fun `quote pair`() {
        NabuTradeAdapter(
            getTransaction(pair = CoinPair.BTC_TO_ETH)
        ).quote.pair `should equal` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `quote withdrawal amount`() {
        NabuTradeAdapter(
            getTransaction(withdrawal = 1.337.ether())
        ).quote.withdrawalAmount `should equal` 1.337.ether()
    }

    @Test
    fun `quote deposit amount`() {
        NabuTradeAdapter(
            getTransaction(deposit = 1.337.bitcoin())
        ).quote.depositAmount `should equal` 1.337.bitcoin()
    }

    @Test
    fun `enoughInfoForDisplay always true`() {
        NabuTradeAdapter(
            getTransaction()
        ).enoughInfoForDisplay() `should be` true
    }

    @Test
    fun `quote mining fee returns ether`() {
        NabuTradeAdapter(
            getTransaction(fee = 0.00001.ether())
        ).quote.minerFee `should equal` 0.00001.ether()
    }

    @Test
    fun `quote mining fee returns bitcoin`() {
        NabuTradeAdapter(
            getTransaction(fee = 0.00001.bitcoin())
        ).quote.minerFee `should equal` 0.00001.bitcoin()
    }

    @Test
    fun `quote mining fee returns bitcoin cash`() {
        NabuTradeAdapter(
            getTransaction(fee = 0.00001.bitcoinCash())
        ).quote.minerFee `should equal` 0.00001.bitcoinCash()
    }

    @Test
    fun `quote rate`() {
        NabuTradeAdapter(
            getTransaction(rate = 10.0.toBigDecimal())
        ).quote.quotedRate `should equal` 10.0.toBigDecimal()
    }

    @Test
    fun `quote fee`() {
        NabuTradeAdapter(
            getTransaction(fee = 0.001.bitcoin())
        ).quote.minerFee `should equal` 0.001.bitcoin()
    }

    @Test
    fun `quote fiat value`() {
        NabuTradeAdapter(
            getTransaction(fiatValue = 20.gbp())
        ).quote.fiatValue `should equal` 20.gbp()
    }

    private fun getTransaction(
        id: String = "",
        createdAt: String = "",
        pair: CoinPair = CoinPair.BTC_TO_ETH,
        rate: BigDecimal = 0.0.toBigDecimal(),
        refundAddress: String = "",
        depositAddress: String = "",
        deposit: CryptoValue = CryptoValue.ZeroBtc,
        withdrawalAddress: String = "",
        withdrawal: CryptoValue = CryptoValue.ZeroEth,
        state: TransactionState = TransactionState.PendingDeposit,
        hashOut: String = "Hash out",
        fee: CryptoValue = CryptoValue.ZeroEth,
        fiatValue: FiatValue = 0.0.gbp()
    ): NabuTransaction = NabuTransaction(
        id = id,
        createdAt = createdAt,
        pair = pair,
        rate = rate,
        refundAddress = refundAddress,
        depositAddress = depositAddress,
        deposit = deposit,
        withdrawalAddress = withdrawalAddress,
        withdrawal = withdrawal,
        state = state,
        hashOut = hashOut,
        fee = fee,
        fiatValue = fiatValue
    )
}