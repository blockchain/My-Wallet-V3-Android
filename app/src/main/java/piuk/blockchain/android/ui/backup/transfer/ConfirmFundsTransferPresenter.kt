package piuk.blockchain.android.ui.backup.transfer

import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.android.R
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import javax.inject.Inject

class ConfirmFundsTransferPresenter @Inject constructor(
        private val walletAccountHelper: WalletAccountHelper,
        private val fundsDataManager: TransferFundsDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val prefsUtil: PrefsUtil,
        private val stringUtils: StringUtils,
        private val exchangeRateFactory: ExchangeRateFactory
) : BasePresenter<ConfirmFundsTransferView>() {

    @VisibleForTesting internal var pendingTransactions: MutableList<PendingTransaction> = mutableListOf()

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
    internal fun sendPayment(secondPassword: String?) {
        val archiveAll = view.getIfArchiveChecked()

        fundsDataManager.sendPayment(pendingTransactions, secondPassword)
                .doOnSubscribe {
                    view.setPaymentButtonEnabled(false)
                    view.showProgressDialog()
                }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doAfterTerminate { view.hideProgressDialog() }
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
    internal fun getReceiveToList() = walletAccountHelper.getHdAccounts(true)

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
        view.updateFromLabel(stringUtils.getQuantityString(
                R.plurals.transfer_label_plural,
                pendingTransactions.size)
        )

        val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
        val fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val btcUnit = monetaryUtil.getBtcUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
        val exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit)

        val fiatAmount = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * (totalToSend.toDouble() / 1e8))
        val fiatFee = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * (totalFee.toDouble() / 1e8))

        view.updateTransferAmountBtc(
                "${monetaryUtil.getDisplayAmountWithFormatting(totalToSend)} $btcUnit")
        view.updateTransferAmountFiat("${exchangeRateFactory.getSymbol(fiatUnit)}$fiatAmount")
        view.updateFeeAmountBtc(
                "${monetaryUtil.getDisplayAmountWithFormatting(totalFee)} $btcUnit")
        view.updateFeeAmountFiat("${exchangeRateFactory.getSymbol(fiatUnit)}$fiatFee")
        view.setPaymentButtonEnabled(true)

        view.onUiUpdated()
    }

    @VisibleForTesting
    internal fun archiveAll() {
        for (spend in pendingTransactions) {
            (spend.sendingObject.accountObject as LegacyAddress).tag = LegacyAddress.ARCHIVED_ADDRESS
        }

        payloadDataManager.syncPayloadWithServer()
                .doOnSubscribe { view.showProgressDialog() }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doAfterTerminate {
                    view.hideProgressDialog()
                    view.dismissDialog()
                }
                .subscribe(
                        { view.showToast(R.string.transfer_archive, ToastCustom.TYPE_OK) },
                        { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    private fun updateToAddress(indexOfReceiveAccount: Int) {
        fundsDataManager.getTransferableFundTransactionList(indexOfReceiveAccount)
                .doOnSubscribe { view.setPaymentButtonEnabled(false) }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({ triple ->
                    pendingTransactions = triple.left
                    updateUi(triple.middle, triple.right)
                }, {
                    view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                    view.dismissDialog()
                })
    }

}
