package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_choose_bank_account.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountDisplayable
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountListObject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class BankAccountSelectionDelegate(
    private val listener: BankAccountSelectionListener
) : AdapterDelegate<BankAccountDisplayable> {

    override fun isForViewType(items: List<BankAccountDisplayable>, position: Int): Boolean =
        items[position] is BankAccountListObject

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AddAccountViewHolder(parent.inflate(R.layout.item_choose_bank_account))

    override fun onBindViewHolder(
        items: List<BankAccountDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as AddAccountViewHolder
        holder.bind(items[position] as BankAccountListObject, listener)
    }

    private class AddAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val rootView = itemView.card_view_select_account
        private val iban = itemView.text_view_iban

        fun bind(bankAccount: BankAccountListObject, listener: BankAccountSelectionListener) {
            iban.text = bankAccount.iban
            rootView.setOnClickListener { listener.onBankAccountSelected(bankAccount.bankAccountId) }
            rootView.setOnLongClickListener {
                consume { listener.onBankAccountLongPressed(bankAccount.bankAccountId) }
            }
        }
    }
}