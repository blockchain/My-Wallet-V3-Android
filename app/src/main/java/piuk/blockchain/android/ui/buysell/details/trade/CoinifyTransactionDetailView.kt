package piuk.blockchain.android.ui.buysell.details.trade

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.buysell.details.models.BuySellDetailsModel
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface CoinifyTransactionDetailView : View {

    val orderDetails: BuySellDetailsModel

    fun launchCardPayment(
        redirectUrl: String,
        paymentId: String,
        fromCurrency: String,
        cost: Double
    )

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun finishPage()
}