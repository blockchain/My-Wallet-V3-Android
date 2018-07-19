package piuk.blockchain.android.ui.buysell.overview.adapter

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.KycStatus
import piuk.blockchain.androidcoreui.utils.extensions.autoNotify
import kotlin.properties.Delegates

internal class CoinifyOverviewAdapter(
    listener: CoinifyTxFeedListener
) : DelegationAdapter<BuySellDisplayable>(AdapterDelegatesManager(), emptyList()) {

    init {
        delegatesManager.addAdapterDelegate(BuySellButtonDelegate(listener))
        delegatesManager.addAdapterDelegate(BuySellTransactionDelegate(listener))
        delegatesManager.addAdapterDelegate(BuySellKycStatusDelegate(listener))
        delegatesManager.addAdapterDelegate(BuySellSubscriptionDelegate(listener))
        delegatesManager.addAdapterDelegate(BuySellEmptyListDelegate())
        setHasStableIds(true)
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<BuySellDisplayable> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()
}

interface CoinifyTxFeedListener {

    fun onTransactionClicked(transactionId: Int)

    fun onBuyClicked()

    fun onSellClicked()

    fun onKycReviewClicked(kycStatus: KycStatus)

    fun onSubscriptionClicked(subscriptionId: Int)
}