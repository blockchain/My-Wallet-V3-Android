package piuk.blockchain.android.ui.backup.transfer

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import javax.inject.Inject

class ConfirmFundsTransferPresenter @Inject constructor(
    private val walletAccountHelper: WalletAccountHelper,
    private val fundsDataManager: TransferFundsDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val currencyFormatManager: CurrencyFormatManager
) : BasePresenter<ConfirmFundsTransferView>() {

    @VisibleForTesting
    internal var pendingTransactions: MutableList<PendingTransaction> =
        mutableListOf()

    override fun onViewReady() {
        updateToAddress(payloadDataManager.defaultAccountIndex)
    }

    internal fun accountSelected(position: Int) {
        updateToAddress(payloadDataManager.getPositionOfAccountFromActiveList(position))
    }

    /**
     * Transacts all [PendingTransaction] objects
     *
     * @param secondPassword The user's double encryption password if necessary
     */
    @SuppressLint("VisibleForTests")
    internal fun sendPayment(secondPassword: String?) {
        val archiveAll = view.getIfArchiveChecked()

        fundsDataManager.sendPayment(pendingTransactions, secondPassword)
            .doOnSubscribe {
                view.setPaymentButtonEnabled(false)
                view.showProgressDialog()
            }
            .addToCompositeDisposable(this)
            .doOnTerminate { view.hideProgressDialog() }
            .subscribe({
                view.showToast(R.string.transfer_confirmed, ToastCustom.TYPE_OK)
                if (archiveAll) {
                    archiveAll()
                } else {
                    view.dismissDialog()
                }
            }, {
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.dismissDialog()
            })
    }

    /**
     * Returns only HD Accounts as we want to move funds to a backed up place
     *
     * @return A [List] of [ItemAccount] objects
     */
    internal fun getReceiveToList() = walletAccountHelper.getHdAccounts()

    /**
     * Get corrected default account position
     *
     * @return int account position in list of non-archived accounts
     */
    internal fun getDefaultAccount() = Math.max(
        payloadDataManager.getPositionOfAccountInActiveList(payloadDataManager.defaultAccountIndex),
        0
    )

    @VisibleForTesting
    internal fun updateUi(totalToSend: Long, totalFee: Long) {
        view.updateFromLabel(
            stringUtils.getQuantityString(
                R.plurals.transfer_label_plural,
                pendingTransactions.size
            )
        )

        val fiatAmount = currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(
            totalToSend.toBigDecimal()
        )
        val fiatFee =
            currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(totalFee.toBigDecimal())

        view.updateTransferAmountBtc(
            currencyFormatManager.getFormattedSelectedCoinValueWithUnit(
                totalToSend.toBigDecimal()
            )
        )
        view.updateTransferAmountFiat(fiatAmount)
        view.updateFeeAmountBtc(currencyFormatManager.getFormattedSelectedCoinValueWithUnit(totalFee.toBigDecimal()))
        view.updateFeeAmountFiat(fiatFee)
        view.setPaymentButtonEnabled(true)

        view.onUiUpdated()
    }

    @VisibleForTesting
    internal fun archiveAll() {
        for (spend in pendingTransactions) {
            (spend.sendingObject.accountObject as LegacyAddress).tag =
                LegacyAddress.ARCHIVED_ADDRESS
        }

        payloadDataManager.syncPayloadWithServer()
            .doOnSubscribe { view.showProgressDialog() }
            .addToCompositeDisposable(this)
            .doOnTerminate {
                view.hideProgressDialog()
                view.dismissDialog()
            }
            .subscribe(
                { view.showToast(R.string.transfer_archive, ToastCustom.TYPE_OK) },
                { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    @SuppressLint("VisibleForTests")
    private fun updateToAddress(indexOfReceiveAccount: Int) {
        fundsDataManager.getTransferableFundTransactionList(indexOfReceiveAccount)
            .doOnSubscribe { view.setPaymentButtonEnabled(false) }
            .addToCompositeDisposable(this)
            .subscribe({ triple ->
                pendingTransactions = triple.left
                updateUi(triple.middle, triple.right)
            }, {
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.dismissDialog()
            })
    }
}
