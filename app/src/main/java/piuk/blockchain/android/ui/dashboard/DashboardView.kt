package piuk.blockchain.android.ui.dashboard

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface DashboardView : View {

    val shouldShowBuy: Boolean

    fun updateChartState(chartsState: ChartsState)

    fun updateEthBalance(balance: String)

    fun updateBtcBalance(balance: String)

    fun updateTotalBalance(balance: String)

    fun updateCryptoCurrencyPrice(price: String)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun notifyItemAdded(displayItems: MutableList<Any>, position: Int)

    fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int)

    fun startBuyActivity()

    fun startReceiveFragment()

}