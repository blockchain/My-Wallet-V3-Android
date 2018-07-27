package com.blockchain.morph

import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

import info.blockchain.wallet.shapeshift.data.Quote as DataQuote

class ExchangeQuoteMappingTest {

    @Test
    fun `can map pair`() {
        DataQuote().apply {
            pair = "btc_eth"
        }.map()
            .pair `should be` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `can map withdrawal amount`() {
        DataQuote().apply {
            pair = "btc_bch"
            withdrawalAmount = BigDecimal.valueOf(1234.5678)
        }.map()
            .withdrawalAmount `should equal` CryptoValue.bitcoinFromMajor(BigDecimal.valueOf(1234.5678))
    }

    @Test
    fun `can map withdrawal amount - Ether`() {
        DataQuote().apply {
            pair = "eth_btc"
            withdrawalAmount = BigDecimal.valueOf(99.876)
        }.map()
            .withdrawalAmount `should equal` CryptoValue.etherFromMajor(BigDecimal.valueOf(99.876))
    }

    @Test
    fun `can map deposit amount`() {
        DataQuote().apply {
            pair = "btc_bch"
            depositAmount = BigDecimal.valueOf(6.745)
        }.map()
            .depositAmount `should equal` CryptoValue.bitcoinCashFromMajor(BigDecimal.valueOf(6.745))
    }

    @Test
    fun `can map deposit amount - Ether`() {
        DataQuote().apply {
            pair = "btc_eth"
            depositAmount = BigDecimal.valueOf(9.456)
        }.map()
            .depositAmount `should equal` CryptoValue.etherFromMajor(BigDecimal.valueOf(9.456))
    }
}