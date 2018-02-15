package piuk.blockchain.android.ui.account

import android.annotation.SuppressLint
import android.content.Intent
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.PayloadException
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.BIP38PrivateKey
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.answers.AddressType
import piuk.blockchain.android.data.answers.CreateAccountEvent
import piuk.blockchain.android.data.answers.ImportEvent
import piuk.blockchain.android.data.answers.Logging
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.data.metadata.MetadataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.websocket.WebSocketService
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.LabelUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class AccountPresenter @Inject internal constructor(
        private val payloadDataManager: PayloadDataManager,
        private val bchDataManager: BchDataManager,
        private val metadataManager: MetadataManager,
        private val fundsDataManager: TransferFundsDataManager,
        private val prefsUtil: PrefsUtil,
        private val appUtil: AppUtil,
        private val privateKeyFactory: PrivateKeyFactory,
        private val environmentSettings: EnvironmentSettings
) : BasePresenter<AccountView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy {
        MonetaryUtil(getBtcFormat())
    }

    internal var doubleEncryptionPassword: String? = null
    internal var cryptoCurrency: CryptoCurrencies by Delegates.observable(CryptoCurrencies.BTC) { _, _, new ->
        check(new != CryptoCurrencies.ETHER) { "Ether not a supported cryptocurrency on this page" }
        onViewReady()
    }
    internal val accountSize = when (cryptoCurrency) {
        CryptoCurrencies.BTC -> getBtcAccounts().size
        CryptoCurrencies.BCH -> getBchAccounts().size
        CryptoCurrencies.ETHER -> throw IllegalStateException("Ether not a supported cryptocurrency on this page")
    }

    override fun onViewReady() {
        view.updateAccountList(getDisplayList())
    }

    /**
     * Silently check if there are any spendable legacy funds that need to be sent to default
     * account. Prompt user when done calculating.
     */
    internal fun checkTransferableLegacyFunds(isAutoPopup: Boolean, showWarningDialog: Boolean) {
        fundsDataManager.transferableFundTransactionListForDefaultAccount
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .subscribe(
                        { triple ->
                            if (payloadDataManager.wallet!!.isUpgraded && !triple.left.isEmpty()) {
                                view.onSetTransferLegacyFundsMenuItemVisible(true)

                                if ((prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true)
                                            || !isAutoPopup)
                                    && showWarningDialog) {
                                    view.onShowTransferableLegacyFundsWarning(isAutoPopup)
                                }
                            } else {
                                view.onSetTransferLegacyFundsMenuItemVisible(false)
                            }
                            view.dismissProgressDialog()
                        },
                        { view.onSetTransferLegacyFundsMenuItemVisible(false) }
                )
    }

    /**
     * Derive new Account from seed
     *
     * @param accountLabel A label for the account to be created
     */
    internal fun createNewAccount(accountLabel: String) {
        if (LabelUtil.isExistingLabel(payloadDataManager, bchDataManager, accountLabel)) {
            view.showToast(R.string.label_name_match, ToastCustom.TYPE_ERROR)
            return
        }

        payloadDataManager.createNewAccount(accountLabel, doubleEncryptionPassword)
                .doOnNext {
                    val intent = Intent(WebSocketService.ACTION_INTENT).apply {
                        putExtra(WebSocketService.EXTRA_X_PUB_BTC, it.xpub)
                    }
                    view.broadcastIntent(intent)
                }
                .flatMapCompletable {
                    bchDataManager.createAccount(it.xpub)
                    metadataManager.saveToMetadata(
                            bchDataManager.serializeForSaving(),
                            BitcoinCashWallet.METADATA_TYPE_EXTERNAL
                    )
                }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .doAfterTerminate { view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribe(
                        {
                            view.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
                            onViewReady()

                            Logging.logCustom(CreateAccountEvent(payloadDataManager.accounts.size))

                        },
                        { throwable ->
                            when (throwable) {
                                is DecryptionException -> view.showToast(
                                        R.string.double_encryption_password_error,
                                        ToastCustom.TYPE_ERROR
                                )
                                is PayloadException -> view.showToast(
                                        R.string.remote_save_ko,
                                        ToastCustom.TYPE_ERROR
                                )
                                else -> view.showToast(
                                        R.string.unexpected_error,
                                        ToastCustom.TYPE_ERROR
                                )
                            }
                        }
                )
    }

    /**
     * Sync [LegacyAddress] with server after either creating a new address or updating the
     * address in some way, for instance updating its name.
     *
     * @param address The [LegacyAddress] to be sync'd with the server
     */
    internal fun updateLegacyAddress(address: LegacyAddress) {
        payloadDataManager.updateLegacyAddress(address)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnSubscribe { view.showProgressDialog(R.string.saving_address) }
                .doOnError { Timber.e(it) }
                .doAfterTerminate { view.dismissProgressDialog() }
                .subscribe(
                        {
                            view.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
                            val intent = Intent(WebSocketService.ACTION_INTENT).apply {
                                putExtra(WebSocketService.EXTRA_BITCOIN_ADDRESS, address.address)
                            }
                            view.broadcastIntent(intent)
                            onViewReady()
                        },
                        { view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR) }
                )
    }

    /**
     * Checks status of camera and updates UI appropriately
     */
    internal fun onScanButtonClicked() {
        if (!appUtil.isCameraOpen) {
            view.startScanForResult()
        } else {
            view.showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR)
        }
    }

    /**
     * Imports BIP38 address and prompts user to rename address if successful
     *
     * @param data     The address to be imported
     * @param password The BIP38 encryption passphrase
     */
    @SuppressLint("VisibleForTests")
    internal fun importBip38Address(data: String, password: String) {
        view.showProgressDialog(R.string.please_wait)
        try {
            val bip38 =
                    BIP38PrivateKey.fromBase58(environmentSettings.bitcoinNetworkParameters, data)
            val key = bip38.decrypt(password)
            handlePrivateKey(key, doubleEncryptionPassword)
        } catch (e: Exception) {
            Timber.e(e)
            view.showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR)
        } finally {
            view.dismissProgressDialog()
        }
    }

    /**
     * Handles result of address scanning operation appropriately for each possible type of address
     *
     * @param data The address to be imported
     */
    internal fun onAddressScanned(data: String?) {
        if (data == null) {
            view.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR)
            return
        }
        try {
            val format = privateKeyFactory.getFormat(data)
            if (format != null) {
                // Private key scanned
                if (format != PrivateKeyFactory.BIP38) {
                    importNonBip38Address(format, data, doubleEncryptionPassword)
                } else {
                    view.showBip38PasswordDialog(data)
                }
            } else {
                // Watch-only address scanned
                importWatchOnlyAddress(data)
            }
        } catch (e: Exception) {
            Timber.e(e)
            view.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR)
        }

    }

    /**
     * Create [LegacyAddress] from correctly formatted address string, show rename dialog
     * after finishing
     *
     * @param address The address to be saved
     */
    internal fun confirmImportWatchOnly(address: String) {
        val legacyAddress = LegacyAddress()
        legacyAddress.address = address
        legacyAddress.createdDeviceName = "android"
        legacyAddress.createdTime = System.currentTimeMillis()
        legacyAddress.createdDeviceVersion = BuildConfig.VERSION_NAME

        payloadDataManager.addLegacyAddress(legacyAddress)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .subscribe(
                        {
                            view.showRenameImportedAddressDialog(legacyAddress)
                            Logging.logCustom(ImportEvent(AddressType.WATCH_ONLY))
                        },
                        {
                            view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)
                        }
                )
    }

    private fun importWatchOnlyAddress(address: String) {
        val addressCopy = correctAddressFormatting(address)

        if (!FormatsUtil.isValidBitcoinAddress(addressCopy)) {
            view.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR)
        } else if (payloadDataManager.wallet!!.legacyAddressStringList.contains(addressCopy)) {
            view.showToast(R.string.address_already_in_wallet, ToastCustom.TYPE_ERROR)
        } else {
            view.showWatchOnlyWarningDialog(addressCopy)
        }
    }

    private fun correctAddressFormatting(address: String): String {
        var addressCopy = address
        // Check for poorly formed BIP21 URIs
        if (addressCopy.startsWith("bitcoin://") && addressCopy.length > 10) {
            addressCopy = "bitcoin:" + addressCopy.substring(10)
        }

        if (FormatsUtil.isBitcoinUri(addressCopy)) {
            addressCopy = FormatsUtil.getBitcoinAddress(addressCopy)
        }

        return addressCopy
    }

    @SuppressLint("VisibleForTests")
    private fun importNonBip38Address(format: String, data: String, secondPassword: String?) {
        payloadDataManager.getKeyFromImportedData(format, data)
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doAfterTerminate { view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribe(
                        { handlePrivateKey(it, secondPassword) },
                        { view.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR) }
                )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @VisibleForTesting
    internal fun handlePrivateKey(key: ECKey?, secondPassword: String?) {
        if (key != null && key.hasPrivKey()) {
            // A private key to an existing address has been scanned
            payloadDataManager.setKeyForLegacyAddress(key, secondPassword)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnError { Timber.e(it) }
                    .subscribe(
                            {
                                view.showToast(
                                        R.string.private_key_successfully_imported,
                                        ToastCustom.TYPE_OK
                                )
                                onViewReady()
                                view.showRenameImportedAddressDialog(it)

                                Logging.logCustom(ImportEvent(AddressType.PRIVATE_KEY))

                            },
                            {
                                view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)
                            }
                    )
        } else {
            view.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR)
        }
    }

    private fun getDisplayList(): List<AccountItem> {
        return when (cryptoCurrency) {
            CryptoCurrencies.BTC -> getBtcDisplayList()
            CryptoCurrencies.BCH -> getBchDisplayList()
            CryptoCurrencies.ETHER -> throw IllegalStateException("Ether not a supported cryptocurrency on this page")
        }
    }

    private fun getBtcDisplayList(): List<AccountItem> {
        val accountsAndImportedList = mutableListOf<AccountItem>()
        var correctedPosition = 0

        // Create New Wallet button at top position
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_CREATE_NEW_WALLET_BUTTON))

        val defaultAccount = getBtcAccounts()[getDefaultBtcIndex()]

        for (account in getBtcAccounts()) {
            val balance = getBtcAccountBalance(account.xpub)
            var label: String? = account.label

            if (label != null && label.length > ADDRESS_LABEL_MAX_LENGTH) {
                label = """${label.substring(0, ADDRESS_LABEL_MAX_LENGTH)}..."""
            }
            if (label.isNullOrEmpty()) label = ""

            accountsAndImportedList.add(
                    AccountItem(
                            correctedPosition,
                            label, null,
                            balance,
                            account.isArchived,
                            false,
                            defaultAccount.xpub == account.xpub,
                            AccountItem.TYPE_ACCOUNT_BTC
                    )
            )
            correctedPosition++
        }

        // Import Address button at first position after wallets
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_IMPORT_ADDRESS_BUTTON))

        for (legacyAddress in getLegacyAddresses()) {
            var label: String? = legacyAddress.label
            val address: String = legacyAddress.address ?: ""
            val balance = getBtcAddressBalance(address)

            if (label != null && label.length > ADDRESS_LABEL_MAX_LENGTH) {
                label = """${label.substring(0, ADDRESS_LABEL_MAX_LENGTH)}..."""
            }
            if (label.isNullOrEmpty()) label = ""

            accountsAndImportedList.add(
                    AccountItem(
                            correctedPosition,
                            label,
                            address,
                            balance,
                            legacyAddress.tag == LegacyAddress.ARCHIVED_ADDRESS,
                            legacyAddress.isWatchOnly,
                            false,
                            AccountItem.TYPE_ACCOUNT_BTC
                    )
            )
            correctedPosition++
        }

        return accountsAndImportedList
    }

    private fun getBchDisplayList(): List<AccountItem> {
        val accountsAndImportedList = mutableListOf<AccountItem>()

        // Create New Wallet button at top position, non-clickable
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_WALLET_HEADER))

        val defaultAccount = getBchAccounts()[getDefaultBchIndex()]

        for ((position, account) in getBchAccounts().withIndex()) {
            val balance = getBchAccountBalance(account.xpub)
            var label: String? = account.label

            if (label != null && label.length > ADDRESS_LABEL_MAX_LENGTH) {
                label = """${label.substring(0, ADDRESS_LABEL_MAX_LENGTH)}..."""
            }
            if (label.isNullOrEmpty()) label = ""

            accountsAndImportedList.add(
                    AccountItem(
                            position,
                            label, null,
                            balance,
                            account.isArchived,
                            false,
                            defaultAccount.xpub == account.xpub,
                            AccountItem.TYPE_ACCOUNT_BCH
                    )
            )
        }

        // Import Address header, non clickable
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_LEGACY_HEADER))

        val total = getLegacyAddresses()
                .map { getBalanceFromBchAddress(it.address) }
                .sum()

        // Non-clickable
        accountsAndImportedList.add(
                AccountItem(AccountItem.TYPE_LEGACY_SUMMARY, getBchDisplayBalance(total))
        )

        return accountsAndImportedList
    }

    //region Convenience functions
    private fun getBtcAccounts(): List<Account> = payloadDataManager.accounts

    private fun getBchAccounts(): List<GenericMetadataAccount> = bchDataManager.getAccounts()

    private fun getLegacyAddresses(): List<LegacyAddress> = payloadDataManager.legacyAddresses

    private fun getDefaultBtcIndex(): Int = payloadDataManager.defaultAccountIndex

    private fun getDefaultBchIndex(): Int = bchDataManager.getDefaultAccountPosition()
    //endregion

    //region Balance and formatting functions
    private fun getBtcAccountBalance(xpub: String): String {
        val amount = getBalanceFromBtcAddress(xpub)
        val unit = monetaryUtil.getBtcUnits()[getBtcFormat()]

        return getFormattedDisplayString(amount, unit)
    }

    private fun getBchAccountBalance(xpub: String): String {
        val amount = getBalanceFromBchAddress(xpub)
        val unit = monetaryUtil.getBchUnits()[getBtcFormat()]

        return getFormattedDisplayString(amount, unit)
    }

    private fun getBtcAddressBalance(address: String): String {
        val amount = getBalanceFromBtcAddress(address)
        val unit = monetaryUtil.getBtcUnits()[getBtcFormat()]

        return getFormattedDisplayString(amount, unit)
    }

    private fun getBchDisplayBalance(amount: Long): String {
        val unit = monetaryUtil.getBchUnits()[getBtcFormat()]
        return getFormattedDisplayString(amount, unit)
    }

    private fun getFormattedDisplayString(amount: Long, unit: String) =
            """${monetaryUtil.getDisplayAmount(amount)} $unit"""

    private fun getBtcFormat(): Int =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getBalanceFromBtcAddress(address: String): Long =
            payloadDataManager.getAddressBalance(address).toLong()

    private fun getBalanceFromBchAddress(address: String): Long =
            bchDataManager.getAddressBalance(address).toLong()
    //endregion

    companion object {

        internal const val KEY_WARN_TRANSFER_ALL = "WARN_TRANSFER_ALL"
        internal const val ADDRESS_LABEL_MAX_LENGTH = 17

    }

}
