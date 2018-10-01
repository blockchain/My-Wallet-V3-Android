package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import org.amshove.kluent.`should be`
import org.amshove.kluent.mock
import org.junit.Test

class ExchangeViewStateValidityTest {

    @Test
    fun `initially not valid`() {
        givenExchangeState()
            .assertNotValid()
    }

    @Test
    fun `is valid - base crypto`() {
        givenExchangeState()
            .copy(
                fix = Fix.BASE_CRYPTO,
                fromCrypto = 10.ether(),
                latestQuote = Quote(
                    fix = Fix.BASE_CRYPTO,
                    from = Quote.Value(cryptoValue = 10.ether(), fiatValue = mock()),
                    to = mock()
                )
            )
            .assertValid()
    }

    @Test
    fun `is valid - base fiat`() {
        givenExchangeState()
            .copy(
                fix = Fix.BASE_FIAT,
                fromFiat = 11.usd(),
                latestQuote = Quote(
                    fix = Fix.BASE_FIAT,
                    from = Quote.Value(cryptoValue = mock(), fiatValue = 11.usd()),
                    to = mock()
                )
            )
            .assertValid()
    }

    @Test
    fun `is valid - counter crypto`() {
        givenExchangeState()
            .copy(
                fix = Fix.COUNTER_CRYPTO,
                toCrypto = 12.ether(),
                latestQuote = Quote(
                    fix = Fix.COUNTER_CRYPTO,
                    from = Quote.Value(cryptoValue = mock(), fiatValue = mock()),
                    to = Quote.Value(cryptoValue = 12.ether(), fiatValue = mock())
                )
            )
            .assertValid()
    }

    @Test
    fun `is valid - counter fiat`() {
        givenExchangeState()
            .copy(
                fix = Fix.COUNTER_FIAT,
                toFiat = 13.usd(),
                latestQuote = Quote(
                    fix = Fix.COUNTER_FIAT,
                    from = mock(),
                    to = Quote.Value(cryptoValue = mock(), fiatValue = 13.usd())
                )
            )
            .assertValid()
    }

    @Test
    fun `is not valid without a quote`() {
        givenExchangeState()
            .copy(
                fix = Fix.BASE_CRYPTO,
                fromCrypto = 10.ether()
            )
            .assertNotValid()
    }

    @Test
    fun `is not valid if the fix doesn't match`() {
        givenExchangeState()
            .copy(
                fix = Fix.BASE_CRYPTO,
                fromCrypto = 10.ether(),
                latestQuote = Quote(
                    fix = Fix.COUNTER_CRYPTO,
                    from = Quote.Value(cryptoValue = 10.ether(), fiatValue = mock()),
                    to = mock()
                )
            )
            .assertNotValid()
    }

    @Test
    fun `is not valid if the value doesn't match`() {
        givenExchangeState()
            .copy(
                fix = Fix.BASE_CRYPTO,
                fromCrypto = 10.ether(),
                latestQuote = Quote(
                    fix = Fix.BASE_CRYPTO,
                    from = Quote.Value(cryptoValue = 9.ether(), fiatValue = mock()),
                    to = mock()
                )
            )
            .assertNotValid()
    }

    @Test
    fun `is not valid if the quote from exceeds the available`() {
        givenExchangeState()
            .copy(
                maxSpendable = 9.ether(),
                fix = Fix.BASE_FIAT,
                fromCrypto = 10.ether(),
                fromFiat = 10.usd(),
                latestQuote = Quote(
                    fix = Fix.BASE_FIAT,
                    from = Quote.Value(cryptoValue = 10.ether(), fiatValue = 10.usd()),
                    to = mock()
                )
            )
            .assertNotValid()
    }

    @Test
    fun `is valid if the quote from equals the available`() {
        givenExchangeState()
            .copy(
                maxSpendable = 9.ether(),
                fix = Fix.BASE_FIAT,
                fromCrypto = 9.ether(),
                fromFiat = 10.usd(),
                latestQuote = Quote(
                    fix = Fix.BASE_FIAT,
                    from = Quote.Value(cryptoValue = 9.ether(), fiatValue = 10.usd()),
                    to = mock()
                )
            )
            .assertValid()
    }

    @Test
    fun `is valid if the available exceeds the quoted amount`() {
        givenExchangeState()
            .copy(
                maxSpendable = 99.ether(),
                fix = Fix.BASE_FIAT,
                fromCrypto = 9.ether(),
                fromFiat = 10.usd(),
                latestQuote = Quote(
                    fix = Fix.BASE_FIAT,
                    from = Quote.Value(cryptoValue = 9.ether(), fiatValue = 10.usd()),
                    to = mock()
                )
            )
            .assertValid()
    }

    @Test
    fun `is valid if the available balance is somehow in a different currency`() {
        givenExchangeState()
            .copy(
                maxSpendable = 1.bitcoin(),
                fix = Fix.BASE_FIAT,
                fromCrypto = 9.ether(),
                fromFiat = 10.usd(),
                latestQuote = Quote(
                    fix = Fix.BASE_FIAT,
                    from = Quote.Value(cryptoValue = 9.ether(), fiatValue = 10.usd()),
                    to = mock()
                )
            )
            .assertValid()
    }
}

private fun givenExchangeState() =
    initial("USD")
        .toInternalState()

private fun ExchangeViewState.assertValid() {
    isValid() `should be` true
}

private fun ExchangeViewState.assertNotValid() {
    isValid() `should be` false
}
