package piuk.blockchain.android.ui.auth

import android.support.v7.app.AlertDialog
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface LandingView : View {

    fun showDebugMenu()

    fun showToast(message: String, @ToastCustom.ToastType toastType: String)

    fun showWarningPrompt(alertDialog: AlertDialog)
}
