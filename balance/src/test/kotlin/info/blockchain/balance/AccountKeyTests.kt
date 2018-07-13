package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.junit.Test
import piuk.blockchain.androidcore.data.currency.CryptoCurrency

class AccountKeyTests {

    @Test
    fun `single address account`() {
        AccountKey.SingleAddress(
            CryptoCurrency.BTC,
            "mhjL1kMDfjmb92FWvd1VaSRE5TfxbrCzWA"
        )
            .apply {
                address `should be` "mhjL1kMDfjmb92FWvd1VaSRE5TfxbrCzWA"
                currency `should be` CryptoCurrency.BTC
            }
    }

    @Test
    fun `single address account - alternative`() {
        AccountKey.SingleAddress(
            CryptoCurrency.BCH,
            "mzNoEDQnrZnsn2NEbAaDvBo3ndfuUfwQ3h"
        )
            .apply {
                address `should be` "mzNoEDQnrZnsn2NEbAaDvBo3ndfuUfwQ3h"
                currency `should be` CryptoCurrency.BCH
            }
    }

    @Test
    fun `whole balance ethereum`() {
        AccountKey.EntireWallet(CryptoCurrency.ETHER)
            .apply {
                currency `should be` CryptoCurrency.ETHER
            }
    }

    @Test
    fun `only imported bitcoin`() {
        AccountKey.OnlyImported(CryptoCurrency.BTC)
            .apply {
                currency `should be` CryptoCurrency.BTC
            }
    }
}
