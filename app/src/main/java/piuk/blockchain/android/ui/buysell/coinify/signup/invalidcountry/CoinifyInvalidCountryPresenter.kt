package piuk.blockchain.android.ui.buysell.coinify.signup.invalidcountry

import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import javax.inject.Inject

class CoinifyInvalidCountryPresenter @Inject constructor() : BasePresenter<CoinifyInvalidCountryView>() {

    override fun onViewReady() = Unit

    fun requestEmailOnBuySellAvailability() {
        // TOD:O The current google doc is not accepting further responses
        view.close()
    }
}