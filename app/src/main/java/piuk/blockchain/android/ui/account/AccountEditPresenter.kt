package piuk.blockchain.android.ui.account

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.support.annotation.VisibleForTesting
import android.view.View
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.BIP38PrivateKey
import org.bitcoinj.params.BitcoinMainNetParams
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.ui.account.AccountEditActivity.Companion.EXTRA_ACCOUNT_INDEX
import piuk.blockchain.android.ui.account.AccountEditActivity.Companion.EXTRA_ADDRESS_INDEX
import piuk.blockchain.android.ui.account.AccountEditActivity.Companion.EXTRA_CRYPTOCURRENCY
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.ui.send.SendModel
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.ui.zxing.Contents
import piuk.blockchain.android.ui.zxing.encode.QRCodeEncoder
import piuk.blockchain.android.util.LabelUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.CryptoCurrency
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigInteger
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject

// TODO: This page is pretty nasty and could do with a proper refactor
class AccountEditPresenter @Inject internal constructor(
    private val prefsUtil: PrefsUtil,
    private val stringUtils: StringUtils,
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val metadataManager: MetadataManager,
    private val sendDataManager: SendDataManager,
    private val privateKeyFactory: PrivateKeyFactory,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val dynamicFeeCache: DynamicFeeCache,
    private val environmentSettings: EnvironmentConfig,
    private val currencyFormatManager: CurrencyFormatManager
) : BasePresenter<AccountEditView>() {

    // Visible for data binding
    internal lateinit var accountModel: AccountEditModel

    @VisibleForTesting
    internal var legacyAddress: LegacyAddress? = null
    @VisibleForTesting
    internal var account: Account? = null
    @VisibleForTesting
    internal var bchAccount: GenericMetadataAccount? = null
    @VisibleForTesting
    internal var secondPassword: String? = null
    @VisibleForTesting
    internal var pendingTransaction: PendingTransaction? = null
    private var accountIndex: Int = 0

    override fun onViewReady() {
        val intent = view.activityIntent

        accountIndex = intent.getIntExtra(EXTRA_ACCOUNT_INDEX, -1)
        val addressIndex = intent.getIntExtra(EXTRA_ADDRESS_INDEX, -1)
        val cryptoCurrency: CryptoCurrency =
            intent.getSerializableExtra(EXTRA_CRYPTOCURRENCY) as CryptoCurrency

        check(accountIndex >= 0 || addressIndex >= 0) { "Both accountIndex and addressIndex are less than 0" }
        check(cryptoCurrency != CryptoCurrency.ETHER) { "Ether is not supported on this page" }

        if (cryptoCurrency == CryptoCurrency.BTC) {
            renderBtc(accountIndex, addressIndex)
        } else {
            renderBch(accountIndex)
        }
    }

    private fun renderBtc(accountIndex: Int, addressIndex: Int) {
        if (accountIndex >= 0) {
            if (accountIndex >= payloadDataManager.accounts.size) {
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.finishPage()
                return
            }
            // V3
            account = payloadDataManager.accounts[accountIndex]
            with(accountModel) {
                label = account!!.label
                labelHeader = stringUtils.getString(R.string.name)
                scanPrivateKeyVisibility = View.GONE
                xpubDescriptionVisibility = View.VISIBLE
                xpubText = stringUtils.getString(R.string.extended_public_key)
                transferFundsVisibility = View.GONE
                updateArchivedUi(account!!.isArchived, ::isArchivableBtc)
                setDefault(isDefaultBtc(account))
            }
        } else if (addressIndex >= 0) {
            if (addressIndex >= payloadDataManager.legacyAddresses.size) {
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.finishPage()
                return
            }
            // V2
            legacyAddress = payloadDataManager.legacyAddresses[addressIndex]
            var label: String? = legacyAddress!!.label
            if (label.isNullOrEmpty()) {
                label = legacyAddress!!.address
            }
            with(accountModel) {
                this.label = label
                labelHeader = stringUtils.getString(R.string.name)
                xpubDescriptionVisibility = View.GONE
                xpubText = stringUtils.getString(R.string.address)
                defaultAccountVisibility = View.GONE // No default for V2
                updateArchivedUi(
                    legacyAddress!!.tag == LegacyAddress.ARCHIVED_ADDRESS,
                    ::isArchivableBtc
                )

                if (legacyAddress!!.isWatchOnly) {
                    scanPrivateKeyVisibility = View.VISIBLE
                    archiveVisibility = View.GONE
                } else {
                    scanPrivateKeyVisibility = View.GONE
                    archiveVisibility = View.VISIBLE
                }
            }

            // Subtract fee
            val balanceAfterFee = payloadDataManager.getAddressBalance(
                legacyAddress!!.address
            ).toLong() - sendDataManager.estimatedFee(
                1, 1,
                BigInteger.valueOf(dynamicFeeCache.btcFeeOptions!!.regularFee * 1000)
            ).toLong()

            if (balanceAfterFee > Payment.DUST.toLong() && !legacyAddress!!.isWatchOnly) {
                accountModel.transferFundsVisibility = View.VISIBLE
            } else {
                // No need to show 'transfer' if funds are less than dust amount
                accountModel.transferFundsVisibility = View.GONE
            }
        }
    }

    private fun renderBch(accountIndex: Int) {
        val accountMetadataList = bchDataManager.getAccountMetadataList()
        if (accountIndex >= accountMetadataList.size) {
            view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
            view.finishPage()
            return
        }

        bchAccount = accountMetadataList[accountIndex]
        with(accountModel) {
            label = bchAccount!!.label
            labelHeader = stringUtils.getString(R.string.name)
            scanPrivateKeyVisibility = View.GONE
            xpubDescriptionVisibility = View.VISIBLE
            xpubText = stringUtils.getString(R.string.extended_public_key)
            transferFundsVisibility = View.GONE
            updateArchivedUi(bchAccount!!.isArchived, ::isArchivableBch)
            setDefault(isDefaultBch(bchAccount))
        }

        view.hideMerchantCopy()
    }

    internal fun areLauncherShortcutsEnabled(): Boolean =
        prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)

    private fun setDefault(isDefault: Boolean) {
        if (isDefault) {
            with(accountModel) {
                defaultAccountVisibility = View.GONE
                archiveAlpha = 0.5f
                archiveText = stringUtils.getString(R.string.default_account_description)
                archiveClickable = false
            }
        } else {
            with(accountModel) {
                defaultAccountVisibility = View.VISIBLE
                defaultText = stringUtils.getString(R.string.make_default)
                defaultTextColor = R.color.primary_blue_accent
            }
        }
    }

    private fun isDefaultBtc(account: Account?): Boolean =
        payloadDataManager.defaultAccount === account

    private fun isDefaultBch(account: GenericMetadataAccount?): Boolean =
        bchDataManager.getDefaultGenericMetadataAccount() === account

    private fun updateArchivedUi(isArchived: Boolean, archivable: () -> Boolean) {
        if (isArchived) {
            with(accountModel) {
                archiveHeader = stringUtils.getString(R.string.unarchive)
                archiveText = stringUtils.getString(R.string.archived_description)
                archiveAlpha = 1.0f
                archiveVisibility = View.VISIBLE
                archiveClickable = true

                labelAlpha = 0.5f
                xpubAlpha = 0.5f
                xprivAlpha = 0.5f
                defaultAlpha = 0.5f
                transferFundsAlpha = 0.5f
                labelClickable = false
                xpubClickable = false
                xprivClickable = false
                defaultClickable = false
                transferFundsClickable = false
            }
        } else {
            // Don't allow archiving of default account
            if (archivable()) {
                with(accountModel) {
                    archiveAlpha = 1.0f
                    archiveVisibility = View.VISIBLE
                    archiveText = stringUtils.getString(R.string.not_archived_description)
                    archiveClickable = true
                }
            } else {
                with(accountModel) {
                    archiveVisibility = View.VISIBLE
                    archiveAlpha = 0.5f
                    archiveText = stringUtils.getString(R.string.default_account_description)
                    archiveClickable = false
                }
            }

            with(accountModel) {
                archiveHeader = stringUtils.getString(R.string.archive)
                labelAlpha = 1.0f
                xpubAlpha = 1.0f
                xprivAlpha = 1.0f
                defaultAlpha = 1.0f
                transferFundsAlpha = 1.0f
                labelClickable = true
                xpubClickable = true
                xprivClickable = true
                defaultClickable = true
                transferFundsClickable = true
            }
        }
    }

    internal fun onClickTransferFunds() {
        view.showProgressDialog(R.string.please_wait)

        getPendingTransactionForLegacyAddress(legacyAddress)
            .addToCompositeDisposable(this)
            .doAfterTerminate { view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .doOnNext { pendingTransaction = it }
            .subscribe(
                { pendingTransaction ->
                    if (pendingTransaction != null &&
                        pendingTransaction.bigIntAmount.compareTo(BigInteger.ZERO) == 1
                    ) {
                        val details = getTransactionDetailsForDisplay(pendingTransaction)
                        view.showPaymentDetails(details)
                    } else {
                        view.showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR)
                    }
                },
                {
                    view.showToast(
                        R.string.insufficient_funds,
                        ToastCustom.TYPE_ERROR
                    )
                }
            )
    }

    internal fun transferFundsClickable(): Boolean = accountModel.transferFundsClickable

    private fun getTransactionDetailsForDisplay(pendingTransaction: PendingTransaction?): PaymentConfirmationDetails {
        val details = PaymentConfirmationDetails()
        details.fromLabel = pendingTransaction!!.sendingObject.label
        if (pendingTransaction.receivingObject != null &&
            pendingTransaction.receivingObject.label != null &&
            !pendingTransaction.receivingObject.label!!.isEmpty()
        ) {
            details.toLabel = pendingTransaction.receivingObject.label
        } else {
            details.toLabel = pendingTransaction.receivingAddress
        }

        val fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val btcUnit = CryptoCurrency.BTC.name

        with(details) {
            cryptoAmount =
                currencyFormatManager.getFormattedSelectedCoinValue(pendingTransaction.bigIntAmount.toBigDecimal())
            cryptoFee =
                currencyFormatManager.getFormattedSelectedCoinValue(pendingTransaction.bigIntFee.toBigDecimal())
            btcSuggestedFee =
                currencyFormatManager.getFormattedSelectedCoinValue(pendingTransaction.bigIntFee.toBigDecimal())
            cryptoUnit = btcUnit
            this.fiatUnit = fiatUnit

            cryptoTotal = currencyFormatManager.getFormattedSelectedCoinValue(
                pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).toBigDecimal()
            )

            fiatFee = currencyFormatManager.getFormattedFiatValueFromSelectedCoinValue(
                pendingTransaction.bigIntFee.toBigDecimal()
            )
            fiatAmount = currencyFormatManager.getFormattedFiatValueFromSelectedCoinValue(
                pendingTransaction.bigIntAmount.toBigDecimal()
            )

            val totalFiat = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
            fiatTotal =
                currencyFormatManager.getFormattedFiatValueFromSelectedCoinValue(totalFiat.toBigDecimal())

            fiatSymbol = currencyFormatManager.getFiatSymbol(fiatUnit, Locale.getDefault())
            isLargeTransaction = isLargeTransaction(pendingTransaction)
            hasConsumedAmounts = pendingTransaction.unspentOutputBundle.consumedAmount
                .compareTo(BigInteger.ZERO) == 1
        }
        return details
    }

    private fun isLargeTransaction(pendingTransaction: PendingTransaction): Boolean {
        val txSize = sendDataManager.estimateSize(
            pendingTransaction.unspentOutputBundle.spendableOutputs.size,
            2
        ) // assume change
        val relativeFee =
            pendingTransaction.bigIntFee.toDouble() / pendingTransaction.bigIntAmount.toDouble() * 100.0

        return (pendingTransaction.bigIntFee.toLong() > SendModel.LARGE_TX_FEE &&
            txSize > SendModel.LARGE_TX_SIZE &&
            relativeFee > SendModel.LARGE_TX_PERCENTAGE)
    }

    internal fun submitPayment() {
        view.showProgressDialog(R.string.please_wait)

        val legacyAddress = pendingTransaction!!.sendingObject.accountObject as LegacyAddress?
        val changeAddress = legacyAddress!!.address

        val keys = ArrayList<ECKey>()

        try {
            val walletKey = payloadDataManager.getAddressECKey(legacyAddress, secondPassword)
                ?: throw NullPointerException("ECKey was null")
            keys.add(walletKey)
        } catch (e: Exception) {
            view.dismissProgressDialog()
            view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)
            return
        }
        sendDataManager.submitBtcPayment(
            pendingTransaction!!.unspentOutputBundle,
            keys,
            pendingTransaction!!.receivingAddress,
            changeAddress,
            pendingTransaction!!.bigIntFee,
            pendingTransaction!!.bigIntAmount
        ).addToCompositeDisposable(this)
            .doAfterTerminate { view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    legacyAddress.tag = LegacyAddress.ARCHIVED_ADDRESS
                    updateArchivedUi(true, ::isArchivableBtc)

                    view.showTransactionSuccess()

                    // Update V2 balance immediately after spend - until refresh from server
                    val spentAmount =
                        pendingTransaction!!.bigIntAmount.toLong() + pendingTransaction!!.bigIntFee.toLong()

                    if (pendingTransaction!!.sendingObject.accountObject is Account) {
                        payloadDataManager.subtractAmountFromAddressBalance(
                            (pendingTransaction!!.sendingObject.accountObject as Account).xpub,
                            spentAmount
                        )
                    } else {
                        payloadDataManager.subtractAmountFromAddressBalance(
                            (pendingTransaction!!.sendingObject.accountObject as LegacyAddress).address,
                            spentAmount
                        )
                    }

                    payloadDataManager.syncPayloadWithServer()
                        .emptySubscribe()

                    accountModel.transferFundsVisibility = View.GONE
                    view.setActivityResult(Activity.RESULT_OK)
                },
                { view.showToast(R.string.send_failed, ToastCustom.TYPE_ERROR) }
            )
    }

    internal fun updateAccountLabel(newLabel: String) {
        val labelCopy = newLabel.trim { it <= ' ' }

        val walletSync: Completable

        if (!labelCopy.isEmpty()) {
            val revertLabel: String

            if (LabelUtil.isExistingLabel(payloadDataManager, bchDataManager, labelCopy)) {
                view.showToast(R.string.label_name_match, ToastCustom.TYPE_ERROR)
                return
            }

            when {
                account != null -> {
                    revertLabel = account!!.label ?: ""
                    account!!.label = labelCopy
                    walletSync = payloadDataManager.syncPayloadWithServer()
                }
                legacyAddress != null -> {
                    revertLabel = legacyAddress!!.label ?: ""
                    legacyAddress!!.label = labelCopy
                    walletSync = payloadDataManager.syncPayloadWithServer()
                }
                else -> {
                    revertLabel = bchAccount!!.label ?: ""
                    bchAccount!!.label = labelCopy
                    walletSync = metadataManager.saveToMetadata(
                        bchDataManager.serializeForSaving(),
                        BitcoinCashWallet.METADATA_TYPE_EXTERNAL
                    )
                }
            }

            walletSync.addToCompositeDisposable(this)
                .doOnError { Timber.e(it) }
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .doAfterTerminate { view.dismissProgressDialog() }
                .subscribe(
                    {
                        accountModel.label = labelCopy
                        view.setActivityResult(Activity.RESULT_OK)
                    },
                    { revertLabelAndShowError(revertLabel) }
                )
        } else {
            view.showToast(R.string.label_cant_be_empty, ToastCustom.TYPE_ERROR)
        }
    }

    // Can't archive default account
    private fun isArchivableBtc(): Boolean = payloadDataManager.defaultAccount !== account

    // Can't archive default account
    private fun isArchivableBch(): Boolean =
        bchDataManager.getDefaultGenericMetadataAccount() !== bchAccount

    private fun revertLabelAndShowError(revertLabel: String) {
        // Remote save not successful - revert
        if (account != null) {
            account!!.label = revertLabel
        } else {
            legacyAddress!!.label = revertLabel
        }
        accountModel.label = revertLabel
        view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickChangeLabel(view: View) {
        getView().promptAccountLabel(accountModel.label)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickDefault(view: View) {
        val revertDefault: Int
        val walletSync: Completable

        if (account != null) {
            revertDefault = payloadDataManager.defaultAccountIndex
            walletSync = payloadDataManager.syncPayloadWithServer()
            payloadDataManager.wallet!!.hdWallets[0].defaultAccountIdx = accountIndex
        } else {
            revertDefault = bchDataManager.getDefaultAccountPosition()
            bchDataManager.setDefaultAccountPosition(accountIndex)
            walletSync = metadataManager.saveToMetadata(
                bchDataManager.serializeForSaving(),
                BitcoinCashWallet.METADATA_TYPE_EXTERNAL
            )
        }

        walletSync.addToCompositeDisposable(this)
            .doOnSubscribe { getView().showProgressDialog(R.string.please_wait) }
            .doOnError { Timber.e(it) }
            .doAfterTerminate { getView().dismissProgressDialog() }
            .subscribe(
                {
                    if (account != null) {
                        setDefault(isDefaultBtc(account))
                    } else {
                        setDefault(isDefaultBch(bchAccount))
                    }

                    updateSwipeToReceiveAddresses()
                    getView().updateAppShortcuts()
                    getView().setActivityResult(Activity.RESULT_OK)
                },
                { revertDefaultAndShowError(revertDefault) }
            )
    }

    private fun updateSwipeToReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable {
            swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
            swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses()
            swipeToReceiveHelper.storeEthAddress()
            Void.TYPE
        }.subscribeOn(Schedulers.computation())
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    private fun revertDefaultAndShowError(revertDefault: Int) {
        // Remote save not successful - revert
        payloadDataManager.wallet!!.hdWallets[0].defaultAccountIdx = revertDefault
        view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickScanXpriv(view: View) {
        if (payloadDataManager.wallet!!.isDoubleEncryption) {
            getView().promptPrivateKey(
                String.format(
                    stringUtils.getString(R.string.watch_only_spend_instructionss),
                    legacyAddress!!.address
                )
            )
        } else {
            getView().startScanActivity()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickShowXpub(view: View) {
        if (account != null || bchAccount != null) {
            getView().showXpubSharingWarning()
        } else {
            showAddressDetails()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickArchive(view: View) {
        val title: String
        val subTitle: String

        if (account != null && account!!.isArchived ||
            bchAccount != null && bchAccount!!.isArchived ||
            legacyAddress != null && legacyAddress!!.tag == LegacyAddress.ARCHIVED_ADDRESS
        ) {
            title = stringUtils.getString(R.string.unarchive)
            subTitle = stringUtils.getString(R.string.unarchive_are_you_sure)
        } else {
            title = stringUtils.getString(R.string.archive)
            subTitle = stringUtils.getString(R.string.archive_are_you_sure)
        }

        getView().promptArchive(title, subTitle)
    }

    private fun toggleArchived(): Boolean {
        return if (account != null) {
            account!!.isArchived = !account!!.isArchived
            account!!.isArchived
        } else if (legacyAddress != null) {
            if (legacyAddress!!.tag == LegacyAddress.ARCHIVED_ADDRESS) {
                legacyAddress!!.tag = LegacyAddress.NORMAL_ADDRESS
                false
            } else {
                legacyAddress!!.tag = LegacyAddress.ARCHIVED_ADDRESS
                true
            }
        } else {
            bchAccount!!.isArchived = !bchAccount!!.isArchived
            bchAccount!!.isArchived
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @VisibleForTesting
    @Throws(Exception::class)
    internal fun importAddressPrivateKey(
        key: ECKey,
        address: LegacyAddress,
        matchesIntendedAddress: Boolean
    ) {
        setLegacyAddressKey(key, address)

        payloadDataManager.syncPayloadWithServer()
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    view.setActivityResult(Activity.RESULT_OK)
                    accountModel.scanPrivateKeyVisibility = View.GONE
                    accountModel.archiveVisibility = View.VISIBLE

                    if (matchesIntendedAddress) {
                        view.privateKeyImportSuccess()
                    } else {
                        view.privateKeyImportMismatch()
                    }
                },
                { view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR) }
            )
    }

    @Throws(Exception::class)
    private fun setLegacyAddressKey(key: ECKey, address: LegacyAddress) {
        // If double encrypted, save encrypted in payload
        if (!payloadDataManager.wallet!!.isDoubleEncryption) {
            address.setPrivateKeyFromBytes(key.privKeyBytes)
        } else {
            val encryptedKey = Base58.encode(key.privKeyBytes)
            val encrypted2 = DoubleEncryptionFactory.encrypt(
                encryptedKey,
                payloadDataManager.wallet!!.sharedKey,
                secondPassword,
                payloadDataManager.wallet!!.options.pbkdf2Iterations
            )
            address.privateKey = encrypted2
        }
    }

    @SuppressLint("VisibleForTests")
    @Suppress("MemberVisibilityCanBePrivate")
    @Throws(Exception::class)
    internal fun importUnmatchedPrivateKey(key: ECKey) {
        if (payloadDataManager.wallet!!.legacyAddressStringList.contains(
                key.toAddress(environmentSettings.bitcoinNetworkParameters).toString()
            )
        ) {
            // Wallet contains address associated with this private key, find & save it with scanned key
            val foundAddressString = key.toAddress(BitcoinMainNetParams.get()).toString()
            for (legacyAddress in payloadDataManager.wallet!!.legacyAddressList) {
                if (legacyAddress.address == foundAddressString) {
                    importAddressPrivateKey(key, legacyAddress, false)
                    break
                }
            }
        } else {
            // Create new address and store
            val legacyAddress = LegacyAddress.fromECKey(key)

            setLegacyAddressKey(key, legacyAddress)
            remoteSaveUnmatchedPrivateKey(legacyAddress)

            view.privateKeyImportMismatch()
        }
    }

    internal fun showAddressDetails() {
        var heading: String? = null
        var note: String? = null
        var copy: String? = null
        var qrString: String? = null
        var bitmap: Bitmap? = null

        when {
            account != null -> {
                heading = stringUtils.getString(R.string.extended_public_key)
                note = stringUtils.getString(R.string.scan_this_code)
                copy = stringUtils.getString(R.string.copy_xpub)
                qrString = account!!.xpub
            }
            legacyAddress != null -> {
                heading = stringUtils.getString(R.string.address)
                note = legacyAddress!!.address
                copy = stringUtils.getString(R.string.copy_address)
                qrString = legacyAddress!!.address
            }
            bchAccount != null -> {
                heading = stringUtils.getString(R.string.extended_public_key)
                note = stringUtils.getString(R.string.scan_this_code)
                copy = stringUtils.getString(R.string.copy_xpub)
                qrString = bchAccount!!.xpub
            }
        }

        val qrCodeDimension = 260
        val qrCodeEncoder = QRCodeEncoder(
            qrString,
            null,
            Contents.Type.TEXT,
            BarcodeFormat.QR_CODE.toString(),
            qrCodeDimension
        )
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap()
        } catch (e: WriterException) {
            Timber.e(e)
        }

        view.showAddressDetails(heading, note, copy, bitmap, qrString)
    }

    internal fun handleIncomingScanIntent(data: Intent) {
        val scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT)
        val format = privateKeyFactory.getFormat(scanData)
        if (format != null) {
            if (format != PrivateKeyFactory.BIP38) {
                importNonBIP38Address(format, scanData)
            } else {
                view.promptBIP38Password(scanData)
            }
        } else {
            view.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR)
        }
    }

    internal fun archiveAccount() {
        val isArchived = toggleArchived()
        val walletSync: Completable
        val updateTransactions: Completable
        val archivable: () -> Boolean

        if (account != null || legacyAddress != null) {
            walletSync = payloadDataManager.syncPayloadWithServer()
            archivable = ::isArchivableBtc
            updateTransactions = payloadDataManager.updateAllTransactions()
        } else {
            walletSync = metadataManager.saveToMetadata(
                bchDataManager.serializeForSaving(),
                BitcoinCashWallet.METADATA_TYPE_EXTERNAL
            )
            archivable = ::isArchivableBch
            updateTransactions =
                Completable.fromObservable(bchDataManager.getWalletTransactions(50, 50))
        }

        walletSync.addToCompositeDisposable(this)
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doOnError { Timber.e(it) }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribe(
                {
                    updateTransactions.emptySubscribe()

                    updateArchivedUi(isArchived, archivable)
                    view.setActivityResult(Activity.RESULT_OK)
                },
                { view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR) }
            )
    }

    @SuppressLint("VisibleForTests")
    private fun importNonBIP38Address(format: String, data: String) {
        view.showProgressDialog(R.string.please_wait)

        try {
            val key = privateKeyFactory.getKey(format, data)
            if (key != null && key.hasPrivKey()) {
                val keyAddress =
                    key.toAddress(environmentSettings.bitcoinNetworkParameters).toString()
                if (legacyAddress!!.address != keyAddress) {
                    // Private key does not match this address - warn user but import nevertheless
                    importUnmatchedPrivateKey(key)
                } else {
                    importAddressPrivateKey(key, legacyAddress!!, true)
                }
            } else {
                view.showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR)
            }
        } catch (e: Exception) {
            view.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR)
        }

        view.dismissProgressDialog()
    }

    @SuppressLint("VisibleForTests")
    internal fun importBIP38Address(data: String, pw: String) {
        view.showProgressDialog(R.string.please_wait)

        try {
            val bip38 =
                BIP38PrivateKey.fromBase58(environmentSettings.bitcoinNetworkParameters, data)
            val key = bip38.decrypt(pw)

            if (key != null && key.hasPrivKey()) {
                val keyAddress =
                    key.toAddress(environmentSettings.bitcoinNetworkParameters).toString()
                if (legacyAddress!!.address != keyAddress) {
                    // Private key does not match this address - warn user but import nevertheless
                    importUnmatchedPrivateKey(key)
                } else {
                    importAddressPrivateKey(key, legacyAddress!!, true)
                }
            } else {
                view.showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR)
            }
        } catch (e: Exception) {
            view.showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR)
            Timber.e(e)
        }

        view.dismissProgressDialog()
    }

    private fun remoteSaveUnmatchedPrivateKey(legacyAddress: LegacyAddress) {
        val addressCopy = ArrayList(payloadDataManager.legacyAddresses)
        addressCopy.add(legacyAddress)
        payloadDataManager.legacyAddresses = addressCopy

        payloadDataManager.syncPayloadWithServer()
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    // Subscribe to new address only if successfully created
                    view.sendBroadcast("address", legacyAddress.address)
                    view.setActivityResult(Activity.RESULT_OK)
                },
                { view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR) }
            )
    }

    /**
     * Generates a [PendingTransaction] object for a given legacy address, where the output is
     * the default account in the user's wallet
     *
     * @param legacyAddress The [LegacyAddress] you wish to transfer funds from
     * @return An [<]
     */
    private fun getPendingTransactionForLegacyAddress(legacyAddress: LegacyAddress?): Observable<PendingTransaction> {
        val pendingTransaction = PendingTransaction()

        return sendDataManager.getUnspentOutputs(legacyAddress!!.address)
            .flatMap { unspentOutputs ->
                val suggestedFeePerKb =
                    BigInteger.valueOf(dynamicFeeCache.btcFeeOptions!!.regularFee * 1000)

                val sweepableCoins =
                    sendDataManager.getMaximumAvailable(unspentOutputs, suggestedFeePerKb)
                val sweepAmount = sweepableCoins.left

                var label: String? = legacyAddress.label
                if (label.isNullOrEmpty()) {
                    label = legacyAddress.address
                }

                // To default account
                val defaultAccount = payloadDataManager.defaultAccount
                pendingTransaction.sendingObject = ItemAccount(
                    label,
                    sweepAmount.toString(),
                    "",
                    sweepAmount.toLong(),
                    legacyAddress,
                    legacyAddress.address
                )
                pendingTransaction.receivingObject = ItemAccount(
                    defaultAccount.label,
                    "",
                    "",
                    sweepAmount.toLong(),
                    defaultAccount,
                    null
                )
                pendingTransaction.unspentOutputBundle = sendDataManager.getSpendableCoins(
                    unspentOutputs,
                    sweepAmount,
                    suggestedFeePerKb
                )
                pendingTransaction.bigIntAmount = sweepAmount
                pendingTransaction.bigIntFee =
                    pendingTransaction.unspentOutputBundle.absoluteFee

                payloadDataManager.getNextReceiveAddress(defaultAccount)
            }
            .doOnNext { pendingTransaction.receivingAddress = it }
            .map { pendingTransaction }
    }
}
