package piuk.blockchain.android.ui.buysell.payment.bank.addaddress

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface AddAddressView : View {

    val iban: String

    val bic: String

    val displayModel: SellConfirmationDisplayModel

    val accountHolderName: String

    val streetAndNumber: String

    val city: String

    val postCode: String

    val locale: Locale

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun showErrorDialog(errorDescription: String)

    fun showCountrySelected(country: String)

    fun goToConfirmation(bankAccountId: Int)
}