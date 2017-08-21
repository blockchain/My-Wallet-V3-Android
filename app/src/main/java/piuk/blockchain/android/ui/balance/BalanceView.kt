package piuk.blockchain.android.ui.balance

import android.support.annotation.StringRes
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.util.MonetaryUtil

interface BalanceView : View {

    val isContactsEnabled: Boolean

    val shouldShowBuy: Boolean

    fun onTransactionsUpdated(displayObjects: List<Any>)

    fun onTotalBalanceUpdated(balance: String)

    fun onExchangeRateUpdated(exchangeRate: Double, isBtc: Boolean)

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun onAccountsUpdated(
            accounts: List<ItemAccount>,
            lastPrice: Double,
            fiat: String,
            monetaryUtil: MonetaryUtil,
            isBtc: Boolean
    )

    fun setUiState(@UiState.UiStateDef uiState: Int)

    fun onViewTypeChanged(isBtc: Boolean, btcFormat: Int)

    fun showFctxRequiringAttention(number: Int)

    fun onContactsHashMapUpdated(
            transactionDisplayMap: MutableMap<String, ContactTransactionDisplayModel>
    )

    fun showAccountChoiceDialog(
            accounts: List<String>,
            fctxId: String,
            amount: String,
            name: String,
            note: String?
    )

    fun initiatePayment(uri: String, recipientId: String, mdid: String, fctxId: String)

    fun showPayOrDeclineDialog(fctxId: String, amount: String, name: String, note: String?)

    fun showWaitingForPaymentDialog()

    fun showWaitingForAddressDialog()

    fun showSendAddressDialog(fctxId: String, amount: String, name: String, note: String?)

    fun showTransactionDeclineDialog(fctxId: String)

    fun showTransactionCancelDialog(fctxId: String)

    fun startBuyActivity()

    fun startReceiveFragment()

    fun onLoadOnboardingPages(pages: List<OnboardingPagerContent>)

}