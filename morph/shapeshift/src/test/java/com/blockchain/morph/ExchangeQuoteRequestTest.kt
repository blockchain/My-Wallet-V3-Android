package com.blockchain.morph

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`should be`
import org.junit.Test
import java.math.BigDecimal

class ExchangeQuoteRequestTest {

    @Test
    fun `selling pair btc eth`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.bitcoinFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            CryptoCurrency.ETHER
        ).pair `should be` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `selling pair btc eth with fiat symbol`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.bitcoinFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            CryptoCurrency.ETHER,
            indicativeFiatSymbol = "USD"
        ).fiatSymbol `should be` "USD"
    }

    @Test
    fun `selling pair bch btc`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.bitcoinCashFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            CryptoCurrency.BTC
        ).pair `should be` CoinPair.BCH_TO_BTC
    }

    @Test
    fun `selling pair bch btc with fiat symbol`() {
        ExchangeQuoteRequest.Selling(
            CryptoValue.bitcoinCashFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            CryptoCurrency.BTC,
            indicativeFiatSymbol = "CAD"
        ).fiatSymbol `should be` "CAD"
    }

    @Test
    fun `buying pair btc eth`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.ETHER,
            CryptoValue.bitcoinFromMajor(
                BigDecimal.valueOf(1.234)
            )
        ).pair `should be` CoinPair.ETH_TO_BTC
    }

    @Test
    fun `buying pair btc eth with fiat symbol`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.ETHER,
            CryptoValue.bitcoinFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            indicativeFiatSymbol = "JPY"
        ).fiatSymbol `should be` "JPY"
    }

    @Test
    fun `buying pair bch btc`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.BTC,
            CryptoValue.bitcoinCashFromMajor(
                BigDecimal.valueOf(1.234)
            )
        ).pair `should be` CoinPair.BTC_TO_BCH
    }

    @Test
    fun `buying pair bch btc with fiat symbol`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.BTC,
            CryptoValue.bitcoinCashFromMajor(
                BigDecimal.valueOf(1.234)
            ),
            indicativeFiatSymbol = "GBP"
        ).fiatSymbol `should be` "GBP"
    }
}