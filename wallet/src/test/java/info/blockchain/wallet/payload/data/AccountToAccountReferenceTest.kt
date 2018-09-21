package info.blockchain.wallet.payload.data

import com.blockchain.wallet.toAccountReference
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.amshove.kluent.mock
import org.junit.Test

class AccountToAccountReferenceTest {

    @Test
    fun `Account to an AccountReference`() {
        Account().apply {
            label = "Bitcoin account"
            xpub = "xpubBtc0123"
        }.toAccountReference()
            .apply {
                cryptoCurrency `should be` CryptoCurrency.BTC
                label `should be` "Bitcoin account"
                this `should be instance of` AccountReference.BitcoinLike::class
                (this as AccountReference.BitcoinLike).xpub `should be` "xpubBtc0123"
            }
    }

    @Test
    fun `GenericMetadataAccount to an AccountReference`() {
        GenericMetadataAccount().apply {
            label = "BitcoinCash account"
            xpub = "xpubBch0456"
        }.toAccountReference()
            .apply {
                cryptoCurrency `should be` CryptoCurrency.BCH
                label `should be` "BitcoinCash account"
                this `should be instance of` AccountReference.BitcoinLike::class
                (this as AccountReference.BitcoinLike).xpub `should be` "xpubBch0456"
            }
    }

    @Test
    fun `EthereumAccount to an AccountReference`() {
        mock<EthereumAccount> {
            on { label } `it returns` "Ethereum account"
            on { address } `it returns` "0x1Address"
        }.toAccountReference()
            .apply {
                cryptoCurrency `should be` CryptoCurrency.ETHER
                label `should be` "Ethereum account"
                this `should be instance of` AccountReference.Ethereum::class
                (this as AccountReference.Ethereum).address `should be` "0x1Address"
            }
    }
}
