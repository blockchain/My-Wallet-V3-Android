package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test

class CryptoCurrencyWithMajorValueExtensionTest {

    @Test
    fun `with major value ETH`() {
        CryptoCurrency.ETHER.withMajorValue(12.3.toBigDecimal()) `should equal` CryptoValue.fromMajor(
            CryptoCurrency.ETHER,
            12.3.toBigDecimal()
        )
    }

    @Test
    fun `with major value BTC`() {
        CryptoCurrency.BTC.withMajorValue(9.12.toBigDecimal()) `should equal` CryptoValue.fromMajor(
            CryptoCurrency.BTC,
            9.12.toBigDecimal()
        )
    }
}
