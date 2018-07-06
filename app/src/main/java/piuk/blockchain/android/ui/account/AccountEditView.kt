package piuk.blockchain.android.ui.account

import android.content.Intent
import android.graphics.Bitmap
import android.support.annotation.StringRes

import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface AccountEditView : View {

    val activityIntent: Intent

    fun promptAccountLabel(currentLabel: String?)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType type: String)

    fun setActivityResult(resultCode: Int)

    fun startScanActivity()

    fun promptPrivateKey(message: String)

    fun promptArchive(title: String, message: String)

    fun promptBIP38Password(data: String)

    fun privateKeyImportMismatch()

    fun privateKeyImportSuccess()

    fun showXpubSharingWarning()

    fun showAddressDetails(
        heading: String?,
        note: String?,
        copy: String?,
        bitmap: Bitmap?,
        qrString: String?
    )

    fun showPaymentDetails(details: PaymentConfirmationDetails)

    fun showTransactionSuccess()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun sendBroadcast(key: String, data: String)

    fun updateAppShortcuts()

    fun hideMerchantCopy()

    fun finishPage()
}
