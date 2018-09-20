package com.blockchain.nabu.datamanagers

import com.blockchain.morph.CoinPair
import com.blockchain.nabu.api.NabuTransaction
import com.blockchain.nabu.api.TransactionState
import com.blockchain.nabu.dataadapters.NabuTradeStatusResponseAdapter
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import org.amshove.kluent.`should equal`
import org.junit.Test

class NabuTradeStatusResponseAdapterTest {

    @Test
    fun `incoming value`() {
        NabuTradeStatusResponseAdapter(
            nabuTransaction
        ).incomingValue `should equal` 0.1337.ether()
    }

    @Test
    fun `outgoing value`() {
        NabuTradeStatusResponseAdapter(
            nabuTransaction
        ).outgoingValue `should equal` 0.008022.bitcoin()
    }

    @Test
    fun `address passed through`() {
        NabuTradeStatusResponseAdapter(
            nabuTransaction
        ).address `should equal` "1Deposit6bAHb8ybZjqQMjJrcCrHGW9sb6uF"
    }

    @Test
    fun `transaction passed through`() {
        NabuTradeStatusResponseAdapter(
            nabuTransaction
        ).transaction `should equal`
            "0xcc34f317a2fc8fb318777ea2529dfaf2ad9338907637137c3ec7d614abe7557f"
    }

    @Test
    fun `default transaction is empty string`() {
        NabuTradeStatusResponseAdapter(
            nabuTransaction.copy(hashOut = null)
        ).transaction `should equal` ""
    }

    private val nabuTransaction: NabuTransaction = NabuTransaction(
        id = "ede39566-1f0d-4e48-96fa-b558b70e46b7",
        createdAt = "2018-07-30T13:45:67.890Z",
        pair = CoinPair.BTC_TO_ETH,
        rate = 0.06.toBigDecimal(),
        refundAddress = "1Refund6bAHb8ybZjqQMjJrcCrHGW9sb6uF",
        depositAddress = "1Deposit6bAHb8ybZjqQMjJrcCrHGW9sb6uF",
        deposit = 0.008022.bitcoin(),
        withdrawalAddress = "0xwithdrawa7d398351b8be11c439e05c5b3259aec9b",
        withdrawal = 0.1337.ether(),
        state = TransactionState.Finished,
        hashOut = "0xcc34f317a2fc8fb318777ea2529dfaf2ad9338907637137c3ec7d614abe7557f",
        fee = 0.000834.ether(),
        fiatValue = 100.gbp()
    )
}