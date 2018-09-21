package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Test

class AccountReferenceTest {

    @Test
    fun `can reference a bitcoin account`() {
        AccountReference.BitcoinLike(CryptoCurrency.BTC, "My Bitcoin account", "xpub123")
            .apply {
                cryptoCurrency `should be` CryptoCurrency.BTC
                label `should be` "My Bitcoin account"
                xpub `should be` "xpub123"
            }
    }

    @Test
    fun `can reference a bitcoin cash account`() {
        AccountReference.BitcoinLike(CryptoCurrency.BCH, "My BitcoinCash account", "xpub456")
            .apply {
                cryptoCurrency `should be` CryptoCurrency.BCH
                label `should be` "My BitcoinCash account"
                xpub `should be` "xpub456"
            }
    }

    @Test
    fun `can reference an ethereum account`() {
        AccountReference.Ethereum("My Ethereum account", "0xaddress")
            .apply {
                cryptoCurrency `should be` CryptoCurrency.ETHER
                label `should be` "My Ethereum account"
                address `should be` "0xaddress"
            }
    }

    @Test
    fun `inequality on currency`() {
        AccountReference.Ethereum("", "") `should not equal`
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "")
    }

    @Test
    fun `inequality on label`() {
        AccountReference.BitcoinLike(CryptoCurrency.BTC, "1", "") `should not equal`
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "2", "")
    }

    @Test
    fun `inequality on xpub`() {
        AccountReference.BitcoinLike(CryptoCurrency.BTC, "1", "xpub1") `should not equal`
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "1", "xpub2")
    }

    @Test
    fun `equality Bitcoin like`() {
        AccountReference.BitcoinLike(CryptoCurrency.BTC, "1", "xpub1") `should equal`
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "1", "xpub1")
    }

    @Test
    fun `inequality Ethereum label`() {
        AccountReference.Ethereum("1", "0xAddress1") `should not equal`
            AccountReference.Ethereum("2", "0xAddress1")
    }

    @Test
    fun `inequality Ethereum address`() {
        AccountReference.Ethereum("1", "0xAddress1") `should not equal`
            AccountReference.Ethereum("1", "0xAddress2")
    }

    @Test
    fun `equality Ethereum`() {
        AccountReference.Ethereum("1", "0xAddress1") `should equal`
            AccountReference.Ethereum("1", "0xAddress1")
    }
}
