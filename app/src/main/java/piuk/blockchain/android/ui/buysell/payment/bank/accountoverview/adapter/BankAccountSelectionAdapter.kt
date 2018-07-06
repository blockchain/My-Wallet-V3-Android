package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.adapter

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountDisplayable
import piuk.blockchain.androidcoreui.utils.extensions.autoNotify
import kotlin.properties.Delegates

internal class BankAccountSelectionAdapter(
    listener: BankAccountSelectionListener
) : DelegationAdapter<BankAccountDisplayable>(AdapterDelegatesManager(), emptyList()) {

    init {
        delegatesManager.addAdapterDelegate(AddAccountDelegate(listener))
        delegatesManager.addAdapterDelegate(BankAccountSelectionDelegate(listener))
        setHasStableIds(true)
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<BankAccountDisplayable> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()
}

interface BankAccountSelectionListener {

    fun onBankAccountSelected(bankAccountId: Int)

    fun onBankAccountLongPressed(bankAccountId: Int)

    fun onAddAccountSelected()
}