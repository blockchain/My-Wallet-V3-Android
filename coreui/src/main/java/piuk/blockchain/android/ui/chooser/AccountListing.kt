package piuk.blockchain.android.ui.chooser

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable

interface AccountListing {
    fun accountList(cryptoCurrency: CryptoCurrency):
        Observable<List<AccountChooserItem>>

    fun importedList(cryptoCurrency: CryptoCurrency):
        Observable<List<AccountChooserItem>>
}
