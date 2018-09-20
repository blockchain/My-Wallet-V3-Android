package info.blockchain.wallet.payload.data

import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import org.amshove.kluent.`should be`
import org.junit.Test

class AccountToAccountReferenceTest {

    @Test
    fun `Account to an AccountReference`() {
        Account().apply { label = "Bitcoin account" }.toAccountReference()
            .apply {
                cryptoCurrency `should be` CryptoCurrency.BTC
                label `should be` "Bitcoin account"
            }
    }

    @Test
    fun `GenericMetadataAccount to an AccountReference`() {
        GenericMetadataAccount().apply { label = "BitcoinCash account" }.toAccountReference()
            .apply {
                cryptoCurrency `should be` CryptoCurrency.BCH
                label `should be` "BitcoinCash account"
            }
    }

    @Test
    fun `EthereumAccount to an AccountReference`() {
        EthereumAccount().apply { label = "Ethereum account" }.toAccountReference()
            .apply {
                cryptoCurrency `should be` CryptoCurrency.ETHER
                label `should be` "Ethereum account"
            }
    }
}
