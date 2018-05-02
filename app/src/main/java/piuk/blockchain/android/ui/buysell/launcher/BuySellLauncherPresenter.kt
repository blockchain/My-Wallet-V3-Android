package piuk.blockchain.android.ui.buysell.launcher

import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import javax.inject.Inject


class BuySellLauncherPresenter @Inject constructor(
): BasePresenter<BuySellLauncherView>() {

    override fun onViewReady() {

        view.onStartCoinifySignUp()
    }
}