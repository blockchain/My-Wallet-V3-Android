package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.junit.Test

class AccountReferenceTest {

    @Test
    fun `can reference a bitcoin account`() {
        AccountReference(CryptoCurrency.BTC)
            .cryptoCurrency `should be` CryptoCurrency.BTC
    }

    @Test
    fun `can reference a bitcoin cash account`() {
        AccountReference(CryptoCurrency.BCH)
            .cryptoCurrency `should be` CryptoCurrency.BCH
    }

    @Test
    fun `can reference an ethereum account`() {
        AccountReference(CryptoCurrency.ETHER)
            .cryptoCurrency `should be` CryptoCurrency.ETHER
    }
}
