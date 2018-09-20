package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Test

class AccountReferenceTest {

    @Test
    fun `can reference a bitcoin account`() {
        AccountReference(CryptoCurrency.BTC, "")
            .cryptoCurrency `should be` CryptoCurrency.BTC
    }

    @Test
    fun `can reference a bitcoin cash account`() {
        AccountReference(CryptoCurrency.BCH, "")
            .cryptoCurrency `should be` CryptoCurrency.BCH
    }

    @Test
    fun `can reference an ethereum account`() {
        AccountReference(CryptoCurrency.ETHER, "")
            .cryptoCurrency `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `can have label`() {
        AccountReference(CryptoCurrency.ETHER, "label")
            .label `should be` "label"
    }

    @Test
    fun `inequality on currency`() {
        AccountReference(CryptoCurrency.ETHER, "") `should not equal`
            AccountReference(CryptoCurrency.BTC, "")
    }

    @Test
    fun `inequality on label`() {
        AccountReference(CryptoCurrency.BTC, "1") `should not equal`
            AccountReference(CryptoCurrency.BTC, "2")
    }

    @Test
    fun equality() {
        AccountReference(CryptoCurrency.BTC, "1") `should equal`
            AccountReference(CryptoCurrency.BTC, "1")
    }
}
