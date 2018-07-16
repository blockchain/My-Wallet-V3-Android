package piuk.blockchain.android.ui.chooser

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import piuk.blockchain.androidcore.data.contacts.ContactsPredicates
import piuk.blockchain.androidcore.data.currency.BTCDenomination
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.math.BigInteger
import java.util.ArrayList
import javax.inject.Inject

class AccountChooserPresenter @Inject internal constructor(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val currencyState: CurrencyState,
    private val stringUtils: StringUtils,
    private val contactsDataManager: ContactsDataManager,
    private val currencyFormatManager: CurrencyFormatManager
) : BasePresenter<AccountChooserView>() {

    private val itemAccounts = ArrayList<ItemAccount>()

    override fun onViewReady() {
        val mode = view.accountMode

        when (mode) {
            AccountMode.ShapeShift -> loadShapeShiftAccounts()
            AccountMode.ContactsOnly -> loadContactsOnly()
            AccountMode.Bitcoin -> loadBitcoinOnly()
            AccountMode.BitcoinSummary -> loadBitcoinSummary()
            AccountMode.BitcoinHdOnly -> loadBitcoinHdOnly()
            AccountMode.BitcoinCash -> loadBitcoinCashOnly()
            AccountMode.BitcoinCashSend -> loadBitcoinCashSend()
            AccountMode.BitcoinCashSummary -> loadBitcoinCashSummary()
        }
    }

    private fun loadBitcoinOnly() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))
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
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))
        parseBchAccountList()
            .addToCompositeDisposable(this)
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadBitcoinCashSend() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))
        parseBchAccountList()
            .addToCompositeDisposable(this)
            .flatMap { parseBchImportedList() }
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadBitcoinSummary() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))

        val legacyAddresses = payloadDataManager.legacyAddresses
            .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }
        val accounts = payloadDataManager.accounts
            .filter { !it.isArchived }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = payloadDataManager.walletBalance

            itemAccounts.add(ItemAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                displayBalance = getBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance
                )
                absoluteBalance = bigIntBalance.toLong()
                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
            })
        }

        parseBtcAccountList()
            .addToCompositeDisposable(this)
            .doOnSuccess {
                // Show "Imported Addresses" if wallet contains legacy addresses
                if (!legacyAddresses.isEmpty()) {
                    val bigIntBalance = payloadDataManager.importedAddressesBalance

                    itemAccounts.add(ItemAccount().apply {
                        displayBalance = getBalanceString(
                            currencyState.isDisplayingCryptoCurrency,
                            bigIntBalance
                        )
                        label = stringUtils.getString(R.string.imported_addresses)
                        absoluteBalance = bigIntBalance.toLong()
                        type = ItemAccount.TYPE.ALL_LEGACY
                    })
                }
            }
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadBitcoinCashSummary() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))

        val accounts = bchDataManager.getActiveAccounts()

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || bchDataManager.getImportedAddressBalance() > BigInteger.ZERO) {
            val bigIntBalance = bchDataManager.getWalletBalance()

            itemAccounts.add(ItemAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                displayBalance = getBalanceString(
                    currencyState.isDisplayingCryptoCurrency,
                    bigIntBalance
                )
                absoluteBalance = bigIntBalance.toLong()
                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
            })
        }

        parseBchAccountList()
            .addToCompositeDisposable(this)
            .doOnSuccess {
                // Show "Imported Addresses" if wallet contains legacy addresses
                if (bchDataManager.getImportedAddressBalance() > BigInteger.ZERO) {
                    val bigIntBalance = bchDataManager.getImportedAddressBalance()

                    itemAccounts.add(ItemAccount().apply {
                        displayBalance = getBalanceString(
                            currencyState.isDisplayingCryptoCurrency,
                            bigIntBalance
                        )
                        label = stringUtils.getString(R.string.imported_addresses)
                        absoluteBalance = bigIntBalance.toLong()
                        type = ItemAccount.TYPE.ALL_LEGACY
                    })
                }
            }
            .subscribe(
                { view.updateUi(itemAccounts) },
                { Timber.e(it) }
            )
    }

    private fun loadShapeShiftAccounts() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.bitcoin)))
        parseBtcAccountList()
            .addToCompositeDisposable(this)
            .flatMap { parseEthAccount() }
            .doOnSuccess { itemAccounts.add(ItemAccount(stringUtils.getString(R.string.bitcoin_cash))) }
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
                            ItemAccount(stringUtils.getString(R.string.contacts_title))
                        )
                        contacts.mapTo(itemAccounts) {
                            ItemAccount(
                                null,
                                null,
                                null,
                                null,
                                it,
                                ""
                            )
                        }
                    }
                }
        }
    }

    private fun parseBtcAccountList(): Single<List<ItemAccount>> =
        getBtcAccountList().doOnNext { itemAccounts.addAll(it) }.singleOrError()

    private fun parseBchAccountList(): Single<List<ItemAccount>> =
        getBchAccountList().doOnNext { itemAccounts.addAll(it) }.singleOrError()

    private fun parseBtcImportedList(): Single<List<ItemAccount>> {
        return getBtcImportedList().doOnNext {
            if (!it.isEmpty()) {
                itemAccounts.add(ItemAccount(stringUtils.getString(R.string.imported_addresses)))
                itemAccounts.addAll(it)
            }
        }.singleOrError()
    }

    private fun parseBchImportedList(): Single<List<ItemAccount>> {
        return getBchImportedList().doOnNext {
            if (!it.isEmpty()) {
                itemAccounts.add(ItemAccount(stringUtils.getString(R.string.imported_addresses)))
                itemAccounts.addAll(it)
            }
        }.singleOrError()
    }

    private fun parseEthAccount(): Single<List<ItemAccount>> {
        return getEthAccount().doOnNext {
            itemAccounts.add(ItemAccount(stringUtils.getString(R.string.ether)))
            itemAccounts.add(it)
        }.map { itemAccounts.toList() }
            .singleOrError()
    }

    private fun getBtcAccountList(): Observable<List<ItemAccount>> =
        Observable.just(walletAccountHelper.getHdAccounts())

    private fun getBtcImportedList(): Observable<List<ItemAccount>> =
        Observable.just(ArrayList(walletAccountHelper.getLegacyAddresses()))

    private fun getBchAccountList(): Observable<List<ItemAccount>> =
        Observable.just(ArrayList(walletAccountHelper.getHdBchAccounts()))

    private fun getBchImportedList(): Observable<List<ItemAccount>> =
        Observable.just(ArrayList(walletAccountHelper.getLegacyBchAddresses()))

    private fun getEthAccount(): Observable<ItemAccount> =
        Observable.just(walletAccountHelper.getEthAccount()[0])

    private fun getBalanceString(displayAsBtc: Boolean, btcBalance: BigInteger): String {
        return if (displayAsBtc) {
            currencyFormatManager.getFormattedValueWithUnit(CryptoValue(CryptoCurrency.BTC, btcBalance))
        } else {
            currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(
                coinValue = btcBalance.toBigDecimal(),
                convertBtcDenomination = BTCDenomination.SATOSHI
            )
        }
    }
}
