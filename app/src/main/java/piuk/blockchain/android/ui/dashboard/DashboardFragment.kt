package piuk.blockchain.android.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_dashboard.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import javax.inject.Inject


class DashboardFragment : BaseFragment<DashboardView, DashboardPresenter>(), DashboardView {

    override val shouldShowBuy: Boolean
        get() = AndroidUtils.is19orHigher()

    private val dashboardAdapter by unsafeLazy { DashboardDelegateAdapter(activity) }
    @Inject lateinit var dashboardPresenter: DashboardPresenter

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container!!.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs_dashboard.apply {
            addTab(tabs_dashboard.newTab().setText(R.string.bitcoin))
            addTab(tabs_dashboard.newTab().setText(R.string.ether))
            setOnTabSelectedListener {
                if (it == 0) {
                    presenter.updateSelectedCurrency(CryptoCurrencies.BTC)
                    dashboardAdapter.updateSelectedCurrency(CryptoCurrencies.BTC)
                } else {
                    presenter.updateSelectedCurrency(CryptoCurrencies.ETHER)
                    dashboardAdapter.updateSelectedCurrency(CryptoCurrencies.ETHER)
                }
            }
        }

        recycler_view?.apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = dashboardAdapter
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        presenter.updatePrices()
    }

    override fun notifyItemAdded(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemInserted(position)
        recycler_view.scrollToPosition(0)
    }

    override fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemRemoved(position)
        recycler_view.smoothScrollToPosition(0)
    }

    override fun updateChartState(chartsState: ChartsState) {
        dashboardAdapter.updateChartState(chartsState)
    }

    override fun updateEthBalance(balance: String) {
        textview_eth.text = balance
    }

    override fun updateBtcBalance(balance: String) {
        textview_btc.text = balance
    }

    override fun updateTotalBalance(balance: String) {
        textview_total.text = balance
    }

    override fun updateCryptoCurrencyPrice(price: String) {
        dashboardAdapter.updateCurrencyPrice(price)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun startBuyActivity() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(Intent(MainActivity.ACTION_BUY))
    }

    override fun startReceiveFragment() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(Intent(MainActivity.ACTION_RECEIVE))
    }

    override fun createPresenter() = dashboardPresenter

    override fun getMvpView() = this

    companion object {

        @JvmStatic
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }

    }

}