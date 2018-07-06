package piuk.blockchain.android.ui.buysell.launcher

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View

interface BuySellLauncherView : View {

    fun onStartCoinifySignUp()

    fun onStartCoinifyOverview()

    fun finishPage()

    fun showErrorToast(@StringRes message: Int)

    fun displayProgressDialog()

    fun dismissProgressDialog()
}