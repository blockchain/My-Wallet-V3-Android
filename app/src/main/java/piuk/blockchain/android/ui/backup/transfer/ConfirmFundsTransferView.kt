package piuk.blockchain.android.ui.backup.transfer

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface ConfirmFundsTransferView : View {

    val locale: Locale

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateFromLabel(label: String)

    fun updateTransferAmountBtc(amount: String)

    fun updateTransferAmountFiat(amount: String)

    fun updateFeeAmountBtc(amount: String)

    fun updateFeeAmountFiat(amount: String)

    fun dismissDialog()

    fun setPaymentButtonEnabled(enabled: Boolean)

    fun getIfArchiveChecked(): Boolean

    fun showProgressDialog()

    fun hideProgressDialog()

    fun onUiUpdated()
}