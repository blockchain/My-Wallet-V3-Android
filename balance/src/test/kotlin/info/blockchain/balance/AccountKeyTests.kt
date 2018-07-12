package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.junit.Test
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies

class AccountKeyTests {

    @Test
    fun `single address account`() {
        AccountKey.SingleAddress(
            CryptoCurrencies.BTC,
            "mhjL1kMDfjmb92FWvd1VaSRE5TfxbrCzWA"
        )
            .apply {
                address `should be` "mhjL1kMDfjmb92FWvd1VaSRE5TfxbrCzWA"
                currency `should be` CryptoCurrencies.BTC
            }
    }

    @Test
    fun `single address account - alternative`() {
        AccountKey.SingleAddress(
            CryptoCurrencies.BCH,
            "mzNoEDQnrZnsn2NEbAaDvBo3ndfuUfwQ3h"
        )
            .apply {
                address `should be` "mzNoEDQnrZnsn2NEbAaDvBo3ndfuUfwQ3h"
                currency `should be` CryptoCurrencies.BCH
            }
    }

    @Test
    fun `whole balance ethereum`() {
        AccountKey.EntireWallet(CryptoCurrencies.ETHER)
            .apply {
                currency `should be` CryptoCurrencies.ETHER
            }
    }

    @Test
    fun `only imported bitcoin`() {
        AccountKey.OnlyImported(CryptoCurrencies.BTC)
            .apply {
                currency `should be` CryptoCurrencies.BTC
            }
    }
}
