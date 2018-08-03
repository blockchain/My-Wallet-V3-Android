package piuk.blockchain.android.ui.chooser

import android.support.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.contacts.data.Contact
import io.reactivex.Single
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import piuk.blockchain.androidcore.data.contacts.ContactsPredicates
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class AccountChooserPresenter @Inject constructor(
    private val accountHelper: AccountListing,
    private val stringUtils: StringUtils,
    private val contactsDataManager: ContactsDataManager
) : BasePresenter<AccountChooserView>() {

    private val itemAccounts = ArrayList<AccountChooserItem>()

    override fun onViewReady() {
        val mode = view.accountMode

        when (mode) {
            AccountMode.Exchange -> loadShapeShiftAccounts()
            AccountMode.ContactsOnly -> loadContactsOnly()
            AccountMode.Bitcoin -> loadBitcoinOnly()
            AccountMode.BitcoinHdOnly -> loadBitcoinHdOnly()
            AccountMode.BitcoinCash -> loadBitcoinCashOnly()
            AccountMode.BitcoinCashSend -> loadBitcoinCashSend()
        }
    }

    private fun loadBitcoinOnly() {
        itemAccounts.add(header(R.string.wallets))
        parseBtcAccountList()
            .addToCompositeDisposable(this)
            .flatMap { parseBtcImportedList() }
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadBitcoinHdOnly() {
        parseBtcAccountList()
            .addToCompositeDisposable(this)
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadBitcoinCashOnly() {
        itemAccounts.add(header(R.string.wallets))
        parseBchAccountList()
            .addToCompositeDisposable(this)
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadBitcoinCashSend() {
        itemAccounts.add(header(R.string.wallets))
        parseBchAccountList()
            .addToCompositeDisposable(this)
            .flatMap { parseBchImportedList() }
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadShapeShiftAccounts() {
        itemAccounts.add(header(R.string.bitcoin))
        parseBtcAccountList()
            .addToCompositeDisposable(this)
            .flatMap { parseEthAccount() }
            .doOnSuccess { itemAccounts.add(header(R.string.bitcoin_cash)) }
            .flatMap { parseBchAccountList() }
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadContactsOnly() {
        parseContactsList()
            .addToCompositeDisposable(this)
            .subscribe(
                {
                    when {
                        !it.isEmpty() -> view.updateUi(itemAccounts)
                        else -> view.showNoContacts()
                    }
                },
                { Timber.e(it) }
            )
    }

    private fun parseContactsList(): Single<List<Contact>> {
        return if (!view.isContactsEnabled) {
            Single.just(ArrayList())
        } else {
            contactsDataManager.getContactList()
                .filter(ContactsPredicates.filterByConfirmed())
                .toList()
                .doOnSuccess { contacts ->
                    if (!contacts.isEmpty()) {
                        itemAccounts.add(
                            header(R.string.contacts_title)
                        )
                        contacts.filter { it.name != null }
                            .mapTo(itemAccounts) {
                                AccountChooserItem.Contact(it.name!!, it)
                            }
                    }
                }
        }
    }

    private fun parseBtcAccountList() =
        accountHelper
            .accountList(CryptoCurrency.BTC)
            .doOnNext {
                itemAccounts.addAll(it)
            }.singleOrError()

    private fun parseBchAccountList() =
        accountHelper
            .accountList(CryptoCurrency.BCH)
            .doOnNext {
                itemAccounts.addAll(it)
            }.singleOrError()

    private fun parseBtcImportedList() =
        accountHelper
            .importedList(CryptoCurrency.BTC)
            .doOnNext {
                if (!it.isEmpty()) {
                    itemAccounts.add(header(R.string.imported_addresses))
                    itemAccounts.addAll(it)
                }
            }.singleOrError()

    private fun parseBchImportedList() =
        accountHelper
            .importedList(CryptoCurrency.BCH)
            .doOnNext {
                if (!it.isEmpty()) {
                    itemAccounts.add(header(R.string.imported_addresses))
                    itemAccounts.addAll(it)
                }
            }.singleOrError()

    private fun parseEthAccount() =
        accountHelper
            .accountList(CryptoCurrency.ETHER)
            .doOnNext {
                itemAccounts.add(header(R.string.ether))
                itemAccounts.addAll(it)
            }.map { itemAccounts.toList() }
            .singleOrError()

    private fun header(@StringRes stringResourceId: Int) =
        AccountChooserItem.Header(stringUtils.getString(stringResourceId))
}
