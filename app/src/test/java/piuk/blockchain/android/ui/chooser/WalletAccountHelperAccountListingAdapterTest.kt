package piuk.blockchain.android.ui.chooser

import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper

class WalletAccountHelperAccountListingAdapterTest {

    @Test
    fun `BTC accounts`() {
        val account = mock<Any>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getHdAccounts() } `it returns` listOf(
                ItemAccount().apply {
                    label = "Acc1"
                    displayBalance = "123 BTC"
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.BTC)
            .assertSingleAccountSummary {
                label `should equal` "Acc1"
                displayBalance `should equal` "123 BTC"
                accountObject `should be` account
            }
    }

    @Test
    fun `BCH accounts`() {
        val account = mock<Any>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getHdBchAccounts() } `it returns` listOf(
                ItemAccount().apply {
                    label = "Acc2"
                    displayBalance = "456 BCH"
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.BCH)
            .assertSingleAccountSummary {
                label `should equal` "Acc2"
                displayBalance `should equal` "456 BCH"
                accountObject `should be` account
            }
    }

    @Test
    fun `ETH accounts`() {
        val account = mock<Any>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getEthAccount() } `it returns` listOf(
                ItemAccount().apply {
                    label = "Acc3"
                    displayBalance = "99 ETH"
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.ETHER)
            .assertSingleAccountSummary {
                label `should equal` "Acc3"
                displayBalance `should equal` "99 ETH"
                accountObject `should be` account
            }
    }

    @Test
    fun `BTC imported (legacy) addresses`() {
        val account = mock<LegacyAddress>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyAddresses() } `it returns` listOf(
                ItemAccount().apply {
                    label = "My address"
                    address = "mhPgaJ366MXe7SNGeaCBBsWAhkM2JfB5Cm"
                    displayBalance = "7 BTC"
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BTC)
            .assertSingleLegacyAddress {
                label `should equal` "My address"
                address `should equal` "mhPgaJ366MXe7SNGeaCBBsWAhkM2JfB5Cm"
                displayBalance `should equal` "7 BTC"
                accountObject `should be` account
            }
    }

    @Test
    fun `BCH imported (legacy) addresses`() {
        val account = mock<LegacyAddress>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyBchAddresses() } `it returns` listOf(
                ItemAccount().apply {
                    label = "My address 2"
                    address = "mpE7PuLdFQaKfHsFSFqM9FbTvLczB3j1QV"
                    displayBalance = "8 BCH"
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BCH)
            .assertSingleLegacyAddress {
                label `should equal` "My address 2"
                address `should equal` "mpE7PuLdFQaKfHsFSFqM9FbTvLczB3j1QV"
                displayBalance `should equal` "8 BCH"
                accountObject `should be` account
            }
    }

    @Test
    fun `BTC imported (legacy) addresses - watch only`() {
        val account = mock<LegacyAddress> {
            on { isWatchOnly } `it returns` true
        }
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyAddresses() } `it returns` listOf(
                ItemAccount().apply {
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BTC)
            .assertSingleLegacyAddress {
                isWatchOnly `should be` true
            }
    }

    @Test
    fun `BTC imported (legacy) addresses - non watch only`() {
        val account = mock<LegacyAddress> {
            on { isWatchOnly } `it returns` false
        }
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyAddresses() } `it returns` listOf(
                ItemAccount().apply {
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BTC)
            .assertSingleLegacyAddress {
                isWatchOnly `should be` false
            }
    }

    @Test
    fun `BTC imported (legacy) addresses - null address when not a legacy`() {
        val account = mock<Any>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyAddresses() } `it returns` listOf(
                ItemAccount().apply {
                    address = "mwfJF7GdsShHBtLCAhWUjymTwAwtf1E5LE"
                    accountObject = account
                })
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BTC)
            .assertSingleLegacyAddress {
                address `should be` null
            }
    }

    private fun givenAccountListing(walletAccountHelper: WalletAccountHelper): AccountListing =
        WalletAccountHelperAccountListingAdapter(walletAccountHelper)
}

private fun Observable<List<AccountChooserItem>>.assertSingleAccountSummary(
    assertBlock: AccountChooserItem.AccountSummary.() -> Unit
) = assertSingle().single().apply {
    assertBlock(this as? AccountChooserItem.AccountSummary ?: throw Exception("Wrong type"))
}

private fun Observable<List<AccountChooserItem>>.assertSingleLegacyAddress(
    assertBlock: AccountChooserItem.LegacyAddress.() -> Unit
) = assertSingle().single().apply {
    assertBlock(this as? AccountChooserItem.LegacyAddress ?: throw Exception("Wrong type"))
}

fun <T> Observable<T>.assertSingle(): T =
    test().values().single()
