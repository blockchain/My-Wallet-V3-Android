package piuk.blockchain.android.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_dashboard.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.websocket.WebSocketService
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.charts.ChartsActivity
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.MainActivity.ACCOUNT_EDIT
import piuk.blockchain.android.ui.home.MainActivity.ACTION_RECEIVE_BCH
import piuk.blockchain.android.ui.home.MainActivity.CONTACTS_EDIT
import piuk.blockchain.android.ui.home.MainActivity.SETTINGS_EDIT
import piuk.blockchain.android.util.OSUtil
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.home.MainActivity.ACTION_EXCHANGE
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import java.util.Locale
import javax.inject.Inject

class DashboardFragment : BaseFragment<DashboardView, DashboardPresenter>(), DashboardView {

    override val shouldShowBuy: Boolean = AndroidUtils.is19orHigher()
    override val locale: Locale = Locale.getDefault()

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var dashboardPresenter: DashboardPresenter
    @Inject
    lateinit var osUtil: OSUtil
    private val dashboardAdapter by unsafeLazy {
        DashboardDelegateAdapter(
            context!!,
            { ChartsActivity.start(context!!, it) },
            { startBalance(it) }
        )
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT && activity != null) {
                // Update balances
                presenter?.updateBalances()
            }
        }
    }
    private val spacerDecoration: BottomSpacerDecoration by unsafeLazy {
        BottomSpacerDecoration(ViewUtils.convertDpToPixel(56f, context).toInt())
    }
    private val safeLayoutManager by unsafeLazy { SafeLayoutManager(context!!) }

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view?.apply {
            layoutManager = safeLayoutManager
            adapter = dashboardAdapter
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
        }
        LocalBroadcastManager.getInstance(context!!)
            .registerReceiver(receiver, IntentFilter(BalanceFragment.ACTION_INTENT))

        recycler_view?.scrollToPosition(0)
    }

    override fun onPause() {
        super.onPause()
        context?.run {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SETTINGS_EDIT || requestCode == CONTACTS_EDIT || requestCode == ACCOUNT_EDIT) {
            presenter.updateBalances()
        }
    }

    override fun scrollToTop() {
        safeLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    override fun notifyItemAdded(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemInserted(position)
        handleRecyclerViewUpdates()
    }

    override fun notifyItemUpdated(displayItems: MutableList<Any>, positions: List<Int>) {
        dashboardAdapter.items = displayItems
        positions.forEach { dashboardAdapter.notifyItemChanged(it) }
        handleRecyclerViewUpdates()
    }

    override fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemRemoved(position)
    }

    override fun updatePieChartState(chartsState: PieChartsState) {
        dashboardAdapter.updatePieChartState(chartsState)
        handleRecyclerViewUpdates()
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    override fun startBuyActivity() {
        broadcastIntent(MainActivity.ACTION_BUY)
    }

    override fun startBitcoinCashReceive() {
        broadcastIntent(ACTION_RECEIVE_BCH)
    }

    override fun startKycFlow() {
        broadcastIntent(ACTION_EXCHANGE)
    }

    override fun startWebsocketService() {
        context?.run {
            val intent = Intent(this, WebSocketService::class.java)

            if (!osUtil.isServiceRunning(WebSocketService::class.java)) {
                applicationContext.startService(intent)
            } else {
                // Restarting this here ensures re-subscription after app restart - the service may remain
                // running, but the subscription to the WebSocket won't be restarted unless onCreate called
                applicationContext.stopService(intent)
                applicationContext.startService(intent)
            }
        }
    }

    override fun createPresenter() = dashboardPresenter

    override fun getMvpView() = this

    private fun startBalance(cryptoCurrency: CryptoCurrency) {
        val action = when (cryptoCurrency) {
            CryptoCurrency.BTC -> MainActivity.ACTION_BTC_BALANCE
            CryptoCurrency.ETHER -> MainActivity.ACTION_ETH_BALANCE
            CryptoCurrency.BCH -> MainActivity.ACTION_BCH_BALANCE
        }

        broadcastIntent(action)
    }

    private fun broadcastIntent(action: String) {
        activity?.run {
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(action))
        }
    }

    /**
     * Inserts a spacer into the last position in the list
     */
    private fun handleRecyclerViewUpdates() {
        recycler_view?.apply {
            removeItemDecoration(spacerDecoration)
            addItemDecoration(spacerDecoration)
        }
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).supportActionBar?.let {
            (activity as ToolBarActivity).setupToolbar(
                it, R.string.dashboard_title
            )
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }

    /**
     * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
     */
    private inner class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun supportsPredictiveItemAnimations() = false
    }
}