package com.blockchain.ui.chooser

import android.support.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import piuk.blockchain.androidcore.data.contacts.ContactsPredicates
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

class AccountChooserPresenter @Inject constructor(
    private val accountHelper: AccountListing,
    private val stringUtils: StringUtils,
    private val contactsDataManager: ContactsDataManager
) : BasePresenter<AccountChooserView>() {

    override fun onViewReady() {
        when (view.accountMode) {
            AccountMode.Exchange -> loadExchangeAccounts()
            AccountMode.ContactsOnly -> loadContactsOnly()
            AccountMode.Bitcoin -> loadBitcoinOnly()
            AccountMode.BitcoinHdOnly -> loadBitcoinHdOnly()
            AccountMode.BitcoinCash -> loadBitcoinCashOnly()
            AccountMode.BitcoinCashSend -> loadBitcoinCashSend()
        }
    }

    private fun loadBitcoinOnly() {
        btcAccountListWithHeader(R.string.wallets)
            .add(btcImportedListWithHeader())
            .subscribeToUpdateList()
    }

    private fun loadBitcoinHdOnly() {
        btcAccountListWithoutHeader()
            .subscribeToUpdateList()
    }

    private fun loadBitcoinCashOnly() {
        bchAccountListWithHeader(R.string.wallets)
            .subscribeToUpdateList()
    }

    private fun loadBitcoinCashSend() {
        bchAccountListWithHeader(R.string.wallets)
            .add(bchImportedListWithHeader())
            .subscribeToUpdateList()
    }

    private fun loadExchangeAccounts() {
        btcAccountListWithHeader(R.string.bitcoin)
            .add(ethAccountWithHeader())
            .add(bchAccountListWithHeader(R.string.bitcoin_cash))
            .subscribeToUpdateList()
    }

    private fun loadContactsOnly() {
        contactsList()
            .addToCompositeDisposable(this)
            .subscribe(
                {
                    when {
                        !it.isEmpty() -> view.updateUi(it)
                        else -> view.showNoContacts()
                    }
                },
                { Timber.e(it) }
            )
    }

    private fun contactsList() =
        if (!view.isContactsEnabled) {
            Single.just(emptyList())
        } else {
            contactsDataManager.getContactList()
                .filter(ContactsPredicates.filterByConfirmed())
                .filter { it.name != null }
                .map {
                    AccountChooserItem.Contact(it.name!!, it)
                }
                .toList()
                .map {
                    if (!it.isEmpty()) {
                        prefixHeader(R.string.contacts_title, it)
                    } else it
                }
        }

    private fun btcAccountListWithoutHeader() =
        accountListWithoutHeader(CryptoCurrency.BTC)

    private fun btcAccountListWithHeader(@StringRes stringResourceId: Int) =
        accountListWithHeader(CryptoCurrency.BTC, stringResourceId)

    private fun bchAccountListWithHeader(@StringRes stringResourceId: Int) =
        accountListWithHeader(CryptoCurrency.BCH, stringResourceId)

    private fun btcImportedListWithHeader() =
        importedListWithHeader(CryptoCurrency.BTC)

    private fun bchImportedListWithHeader() =
        importedListWithHeader(CryptoCurrency.BCH)

    private fun ethAccountWithHeader() =
        accountListWithHeader(CryptoCurrency.ETHER, R.string.ether)

    private fun importedListWithHeader(cryptoCurrency: CryptoCurrency) =
        accountHelper
            .importedList(cryptoCurrency)
            .singleOrError()
            .map {
                if (!it.isEmpty()) {
                    prefixHeader(R.string.imported_addresses, it)
                } else {
                    it
                }
            }

    private fun accountListWithHeader(
        cryptoCurrency: CryptoCurrency,
        @StringRes headerResourceId: Int
    ) =
        accountListWithoutHeader(cryptoCurrency)
            .map { prefixHeader(headerResourceId, it) }

    private fun accountListWithoutHeader(cryptoCurrency: CryptoCurrency) =
        accountHelper
            .accountList(cryptoCurrency)
            .singleOrError()

    private fun prefixHeader(
        @StringRes stringResourceId: Int,
        items: List<AccountChooserItem>
    ) = listOf(header(stringResourceId)) + items

    private fun header(@StringRes stringResourceId: Int) =
        AccountChooserItem.Header(stringUtils.getString(stringResourceId))

    private fun Single<List<AccountChooserItem>>.subscribeToUpdateList() =
        addToCompositeDisposable(this@AccountChooserPresenter)
            .subscribe(
                { view.updateUi(it) },
                { Timber.e(it) }
            )
}

private fun <T> Single<List<T>>.add(other: Single<List<T>>) =
    Single.zip(this, other,
        BiFunction<List<T>, List<T>, List<T>> { l1, l2 -> l1 + l2 })
