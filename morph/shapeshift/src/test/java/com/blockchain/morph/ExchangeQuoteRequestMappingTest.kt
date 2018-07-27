package com.blockchain.morph

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class ExchangeQuoteRequestMappingTest {

    @Test
    fun `can map selling`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.bitcoinFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            CryptoCurrency.ETHER
        ).map()
            .apply {
                pair `should be` CoinPair.BTC_TO_ETH.pairCode
                depositAmount `should equal` BigDecimal.valueOf(1.234).setScale(8)
                withdrawalAmount `should be` BigDecimal.ZERO
            }
    }

    @Test
    fun `can map buying`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.ETHER,
            CryptoValue.bitcoinCashFromMajor(
                BigDecimal.valueOf(5.687)
            )
        ).map()
            .apply {
                pair `should be` CoinPair.ETH_TO_BCH.pairCode
                withdrawalAmount `should equal` BigDecimal.valueOf(5.687).setScale(8)
                depositAmount `should be` BigDecimal.ZERO
            }
    }

    @Test
    fun `Ether should be scaled to 8 on buy`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.BTC,
            CryptoValue.etherFromMajor(
                BigDecimal.valueOf(5.123456785)
            )
        ).map()
            .withdrawalAmount `should equal` BigDecimal.valueOf(5.12345678)
    }

    @Test
    fun `Ether should be scaled to 8 on buy - round up`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.BTC,
            CryptoValue.etherFromMajor(
                BigDecimal.valueOf(5.123456786)
            )
        ).map()
            .withdrawalAmount `should equal` BigDecimal.valueOf(5.12345679)
    }

    @Test
    fun `Ether should be scaled to 8 on sell`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.etherFromMajor(
                BigDecimal.valueOf(5.123456785)
            ),
            CryptoCurrency.BTC
        ).map()
            .depositAmount `should equal` BigDecimal.valueOf(5.12345678)
    }

    @Test
    fun `Ether should be scaled to 8 on sell - round up`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.etherFromMajor(
                BigDecimal.valueOf(5.123456786)
            ),
            CryptoCurrency.BTC
        ).map()
            .depositAmount `should equal` BigDecimal.valueOf(5.12345679)
    }
}