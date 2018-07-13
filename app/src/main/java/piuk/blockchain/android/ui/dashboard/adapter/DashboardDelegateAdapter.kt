package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.PieChartsState
import info.blockchain.balance.CryptoCurrency

/**
 * @param context The Activity/Fragment [Context]
 * @param assetSelector A callback for getting the selected coin from the asset balance card
 * @param coinSelector A callback for getting the selected coin from the pie Chart
 */
class DashboardDelegateAdapter(
    context: Context,
    assetSelector: (CryptoCurrency) -> Unit,
    coinSelector: (CryptoCurrency) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val onboardingDelegate = OnboardingDelegate<Any>(context)
    private val pieChartDelegate = PieChartDelegate<Any>(context, coinSelector)
    private val assetPriceDelegate = AssetPriceCardDelegate<Any>(context, assetSelector)

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(AnnouncementDelegate())
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(onboardingDelegate)
        delegatesManager.addAdapterDelegate(pieChartDelegate)
        delegatesManager.addAdapterDelegate(assetPriceDelegate)
    }

    /**
     * Updates the state of the Balance card without causing a refresh of the entire View.
     */
    fun updatePieChartState(chartsState: PieChartsState) {
        pieChartDelegate.updateChartState(chartsState)
    }
}