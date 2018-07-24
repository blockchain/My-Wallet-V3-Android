package piuk.blockchain.android.ui.buysell.confirmation.sell

import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface CoinifySellConfirmationView : View {

    val locale: Locale

    val displayableQuote: SellConfirmationDisplayModel

    val bankAccountId: Int

    fun updateCounter(timeRemaining: String)

    fun showTimeExpiring()

    fun showQuoteExpiredDialog()

    fun displayProgressDialog()

    fun dismissProgressDialog()

    fun showErrorDialog(errorMessage: String)

    fun showTransactionComplete()

    fun displaySecondPasswordDialog()
}
