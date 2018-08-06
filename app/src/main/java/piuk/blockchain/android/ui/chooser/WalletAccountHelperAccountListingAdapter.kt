package piuk.blockchain.android.ui.chooser

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Observable
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.androidcore.injection.PresenterScope
import javax.inject.Inject

@PresenterScope
class WalletAccountHelperAccountListingAdapter @Inject constructor(
    private val walletAccountHelper: WalletAccountHelper
) : AccountListing {

    override fun accountList(cryptoCurrency: CryptoCurrency): Observable<List<AccountChooserItem>> =
        Observable.just(
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> walletAccountHelper.getHdAccounts()
                CryptoCurrency.BCH -> walletAccountHelper.getHdBchAccounts()
                CryptoCurrency.ETHER -> walletAccountHelper.getEthAccount()
            }.map(this::mapAccountSummary)
        )

    override fun importedList(cryptoCurrency: CryptoCurrency): Observable<List<AccountChooserItem>> =
        Observable.just(
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> walletAccountHelper.getLegacyAddresses()
                CryptoCurrency.BCH -> walletAccountHelper.getLegacyBchAddresses()
                CryptoCurrency.ETHER -> emptyList()
            }.map(this::mapLegacyAddress)
        )

    private fun mapAccountSummary(it: ItemAccount): AccountChooserItem =
        AccountChooserItem.AccountSummary(
            it.label ?: "",
            it.displayBalance ?: "",
            it.accountObject
        )

    private fun mapLegacyAddress(itemAccount: ItemAccount): AccountChooserItem {
        val legacyAddress = itemAccount.accountObject as? LegacyAddress
        return AccountChooserItem.LegacyAddress(
            label = itemAccount.label ?: "",
            address = if (legacyAddress == null) null else itemAccount.address,
            displayBalance = itemAccount.displayBalance ?: "",
            isWatchOnly = legacyAddress?.isWatchOnly ?: true,
            accountObject = itemAccount.accountObject
        )
    }
}
