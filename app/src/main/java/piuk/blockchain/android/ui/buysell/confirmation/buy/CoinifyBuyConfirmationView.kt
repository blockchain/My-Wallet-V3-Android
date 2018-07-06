package piuk.blockchain.android.ui.buysell.confirmation.buy

import piuk.blockchain.android.ui.buysell.createorder.models.BuyConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.createorder.models.OrderType
import piuk.blockchain.android.ui.buysell.details.models.AwaitingFundsModel
import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface CoinifyBuyConfirmationView : View {

    val orderType: OrderType

    val locale: Locale

    val displayableQuote: BuyConfirmationDisplayModel

    fun updateCounter(timeRemaining: String)

    fun showTimeExpiring()

    fun showQuoteExpiredDialog()

    fun displayProgressDialog()

    fun dismissProgressDialog()

    fun launchCardPaymentWebView(
        redirectUrl: String,
        paymentId: String,
        fromCurrency: String,
        cost: Double
    )

    fun showErrorDialog(errorMessage: String)

    fun showOverCardLimitDialog(localisedCardLimit: String, cardLimit: Double)

    fun launchCardConfirmation()

    fun launchBankConfirmation()

    fun launchTransferDetailsPage(tradeId: Int, awaitingFundsModel: AwaitingFundsModel)
}