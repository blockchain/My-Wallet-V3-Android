package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_add_bank_account.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.AddAccountButton
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountDisplayable
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class AddAccountDelegate(
    private val listener: BankAccountSelectionListener
) : AdapterDelegate<BankAccountDisplayable> {

    override fun isForViewType(items: List<BankAccountDisplayable>, position: Int): Boolean =
        items[position] is AddAccountButton

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AddAccountViewHolder(parent.inflate(R.layout.item_add_bank_account))

    override fun onBindViewHolder(
        items: List<BankAccountDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as AddAccountViewHolder
        holder.bind(listener)
    }

    private class AddAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val rootView = itemView.card_view_add_account

        fun bind(listener: BankAccountSelectionListener) {
            rootView.setOnClickListener { listener.onAddAccountSelected() }
        }
    }
}