package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import org.amshove.kluent.`should be`
import org.amshove.kluent.mock
import org.junit.Test

class ExchangeViewModelValidityTest {

    @Test
    fun `is valid - base crypto`() {
        ExchangeViewModel(
            fromAccount = mock(),
            toAccount = mock(),
            from = value(
                userEntered(10.ether()),
                upToDate(25.gbp())
            ),
            to = mock(),
            latestQuote = Quote(
                fix = Fix.BASE_CRYPTO,
                from = Quote.Value(cryptoValue = 10.ether(), fiatValue = mock()),
                to = mock()
            )
        ).isValid() `should be` true
    }

    @Test
    fun `is not valid without a quote`() {
        ExchangeViewModel(
            fromAccount = mock(),
            toAccount = mock(),
            from = value(
                userEntered(10.ether()),
                upToDate(25.gbp())
            ),
            to = mock(),
            latestQuote = null
        ).isValid() `should be` false
    }

    @Test
    fun `is not valid if the fix doesn't match`() {
        ExchangeViewModel(
            fromAccount = mock(),
            toAccount = mock(),
            from = value(
                userEntered(10.ether()),
                upToDate(25.gbp())
            ),
            to = mock(),
            latestQuote = Quote(
                fix = Fix.COUNTER_CRYPTO,
                from = mock(),
                to = Quote.Value(cryptoValue = 10.ether(), fiatValue = mock())
            )
        ).isValid() `should be` false
    }


    @Test
    fun `is not valid if the value doesn't match`() {
        ExchangeViewModel(
            fromAccount = mock(),
            toAccount = mock(),
            from = value(
                userEntered(10.ether()),
                upToDate(25.gbp())
            ),
            to = mock(),
            latestQuote = Quote(
                fix = Fix.BASE_CRYPTO,
                from = Quote.Value(cryptoValue = 9.ether(), fiatValue = mock()),
                to = mock()
            )
        ).isValid() `should be` false
    }
}