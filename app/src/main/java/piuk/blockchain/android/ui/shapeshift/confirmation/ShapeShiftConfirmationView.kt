package piuk.blockchain.android.ui.shapeshift.confirmation

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface ShapeShiftConfirmationView : View {

    val shapeShiftData: ShapeShiftData

    val locale: Locale

    fun showToast(message: Int, toastType: String)

    fun finishPage()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun setButtonState(enabled: Boolean)

    fun updateCounter(timeRemaining: String)

    fun updateDeposit(label: String, amount: String)

    fun updateReceive(label: String, amount: String)

    fun updateTotalAmount(label: String, amount: String)

    fun updateExchangeRate(exchangeRate: String)

    fun updateTransactionFee(displayString: String)

    fun updateNetworkFee(displayString: String)

    fun showSecondPasswordDialog()

    fun showTimeExpiring()

    fun showQuoteExpiredDialog()

    fun launchProgressPage(depositAddress: String)
}