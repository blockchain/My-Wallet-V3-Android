package piuk.blockchain.android.ui.login

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface LoginView : View {

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun startPinEntryActivity()
}