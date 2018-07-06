package piuk.blockchain.android.ui.buysell.details.trade

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.buysell.details.models.BuySellDetailsModel
import piuk.blockchain.androidcoreui.ui.base.View

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

    fun showErrorToast(@StringRes message: Int)
}